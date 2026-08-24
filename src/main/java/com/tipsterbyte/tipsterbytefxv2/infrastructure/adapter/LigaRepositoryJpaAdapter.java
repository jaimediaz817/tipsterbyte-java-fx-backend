// ─────────────────────────────────────────────
// [QUÉ]: Adapter JPA del puerto LigaRepository. Convierte entre el aggregate Liga del
//        dominio y LigaEntity de persistencia (mapper).
// [POR QUÉ]: Implementa el puerto definido en application sin que el dominio conozca
//            JPA. Las operaciones de lectura se marcan @Transactional(readOnly = true)
//            para que el mapeo de colecciones LAZY ocurra dentro de la transacción.
//            Los equipos y las posiciones se mapean A TRAVÉS de cada temporada
//            (fase dedicada de temporadas): son datos de la temporada, no de la liga.
//            paisId se resuelve como referencia perezosa a PaisEntity (FK real).
// [ALTERNATIVAS]: @EntityGraph para cargar colecciones; se descarta por simplicidad:
//                 la transacción de lectura es suficiente en este volumen de datos.
// [RELACIONES]: Implementa application.port.LigaRepository (CU-01, CU-02, CU-04).
//               Refere PaisJpaRepository para la FK pais_id.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PosicionTabla;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.EquipoEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.LigaEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.PaisEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.PosicionTablaEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.TemporadaEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.LigaJpaRepository;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.PaisJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class LigaRepositoryJpaAdapter implements LigaRepository {

    private final LigaJpaRepository jpaRepository;
    private final PaisJpaRepository paisJpaRepository;

    public LigaRepositoryJpaAdapter(LigaJpaRepository jpaRepository,
                                    PaisJpaRepository paisJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.paisJpaRepository = paisJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Liga> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(this::toDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Liga> buscarActivas() {
        return jpaRepository.findByEstado(EstadoLiga.ACTIVA).stream()
                .map(this::toDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Liga> buscarPorEstado(EstadoLiga estado) {
        return jpaRepository.findByEstado(estado).stream()
                .map(this::toDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Liga> buscarPorEstadoYPais(EstadoLiga estado, String pais) {
        return jpaRepository.findByEstadoAndPaisNombreIgnoreCase(estado, pais).stream()
                .map(this::toDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Liga> buscarPorUrlSoccerway(String urlSoccerway) {
        return jpaRepository.findByUrlSoccerway(urlSoccerway).map(this::toDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public long contar() {
        return jpaRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Liga> buscarPorPais(String pais) {
        return jpaRepository.findByPaisNombreIgnoreCase(pais).stream()
                .map(this::toDominio)
                .toList();
    }

    @Override
    @Transactional
    public void guardar(Liga liga) {
        LigaEntity entidad = toEntity(liga);
        jpaRepository.save(entidad);
    }

    private Liga toDominio(LigaEntity entidad) {
        UUID paisId = entidad.getPaisRef() != null ? entidad.getPaisRef().getId() : null;
        Set<Temporada> temporadas = entidad.getTemporadas().stream()
                .map(this::temporadaToDominio)
                .collect(java.util.stream.Collectors.toSet());
        return Liga.reconstruir(
                entidad.getId(), entidad.getNombre(), entidad.getPais(), paisId,
                entidad.getEstado(), entidad.getUrlSoccerway(), entidad.getApiId(),
                temporadas);
    }

    // [QUÉ]: Mapea una temporada persistida con SU plantilla de equipos y SU tabla.
    private Temporada temporadaToDominio(TemporadaEntity entidad) {
        List<Equipo> equipos = entidad.getEquipos().stream()
                .map(e -> new Equipo(e.getId(), e.getNombre(), e.getLogoUrl()))
                .toList();
        List<PosicionTabla> posiciones = entidad.getPosiciones().stream()
                .map(p -> new PosicionTabla(
                        new Equipo(p.getEquipo().getId(), p.getEquipo().getNombre()),
                        p.getPosicion(), p.getJugados(), p.getGanados(), p.getEmpatados(),
                        p.getPerdidos(), p.getGolesFavor(), p.getGolesContra(), p.getPuntos(),
                        p.getUltimosResultados()))
                .toList();
        return new Temporada(
                entidad.getId(), entidad.getLiga().getId(), entidad.getNombre(),
                entidad.getSemestre(), entidad.getAnioInicio(), entidad.getAnioFin(),
                entidad.getEstado(), equipos, posiciones);
    }

    private LigaEntity toEntity(Liga liga) {
        LigaEntity entidad = new LigaEntity(
                liga.id(), liga.nombre(), liga.pais(),
                liga.urlSoccerway(), liga.apiId(), liga.estado());
        if (liga.paisId() != null) {
            // Referencia perezosa al país: no requiere SELECT previo (la FK basta).
            entidad.setPaisRef(paisJpaRepository.getReferenceById(liga.paisId()));
        }
        for (Temporada temporada : liga.getTemporadas()) {
            entidad.agregarTemporada(temporadaToEntity(temporada, entidad));
        }
        return entidad;
    }

    // [QUÉ]: Mapea una temporada del dominio con sus equipos/posiciones. Reutiliza el
    //        EquipoEntity ya agregado cuando una posición refiere al mismo equipo.
    private TemporadaEntity temporadaToEntity(Temporada temporada, LigaEntity liga) {
        TemporadaEntity entidad = new TemporadaEntity(
                temporada.id(), liga, temporada.nombre(), temporada.semestre(),
                temporada.anioInicio(), temporada.anioFin(), temporada.estado());
        Map<UUID, EquipoEntity> equiposById = new HashMap<>();
        for (Equipo equipo : temporada.equipos()) {
            EquipoEntity equipoEntity = new EquipoEntity(equipo.id(), equipo.nombre(), equipo.logoUrl());
            entidad.agregarEquipo(equipoEntity);
            equiposById.put(equipo.id(), equipoEntity);
        }
        for (PosicionTabla posicion : temporada.posiciones()) {
            EquipoEntity equipoEntity = equiposById.get(posicion.equipo().id());
            entidad.agregarPosicion(new PosicionTablaEntity(
                    equipoEntity, posicion.posicion(), posicion.jugados(), posicion.ganados(),
                    posicion.empatados(), posicion.perdidos(), posicion.golesFavor(),
                    posicion.golesContra(), posicion.puntos(), posicion.ultimosResultados()));
        }
        return entidad;
    }
}
