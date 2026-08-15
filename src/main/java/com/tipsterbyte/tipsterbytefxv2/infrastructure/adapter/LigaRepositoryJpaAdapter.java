// ─────────────────────────────────────────────
// [QUÉ]: Adapter JPA del puerto LigaRepository. Convierte entre el aggregate Liga del
//        dominio y LigaEntity de persistencia (mapper).
// [POR QUÉ]: Implementa el puerto definido en application sin que el dominio conozca
//            JPA. Las operaciones de lectura se marcan @Transactional(readOnly = true)
//            para que el mapeo de colecciones LAZY (equipos, posiciones) ocurra dentro
//            de la transacción de Hibernate.
// [ALTERNATIVAS]: @EntityGraph para cargar colecciones; se descarta por simplicidad:
//                 la transacción de lectura es suficiente en este volumen de datos.
// [RELACIONES]: Implementa application.port.LigaRepository (CU-01, CU-02, CU-04).
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
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.PosicionTablaEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.LigaJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LigaRepositoryJpaAdapter implements LigaRepository {

    private final LigaJpaRepository jpaRepository;

    public LigaRepositoryJpaAdapter(LigaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
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
    public Optional<Liga> buscarPorUrlSoccerway(String urlSoccerway) {
        return jpaRepository.findByUrlSoccerway(urlSoccerway).map(this::toDominio);
    }

    @Override
    @Transactional
    public void guardar(Liga liga) {
        LigaEntity entidad = toEntity(liga);
        jpaRepository.save(entidad);
    }

    private Liga toDominio(LigaEntity entidad) {
        List<Equipo> equipos = entidad.getEquipos().stream()
                .map(e -> new Equipo(e.getId(), e.getNombre()))
                .toList();
        List<PosicionTabla> posiciones = entidad.getPosiciones().stream()
                .map(p -> new PosicionTabla(
                        new Equipo(p.getEquipo().getId(), p.getEquipo().getNombre()),
                        p.getPosicion(), p.getJugados(), p.getGanados(), p.getEmpatados(),
                        p.getPerdidos(), p.getGolesFavor(), p.getGolesContra(), p.getPuntos(),
                        p.getUltimosResultados()))
                .toList();
        return Liga.reconstruir(
                entidad.getId(), entidad.getNombre(), entidad.getPais(),
                new Temporada(entidad.getTemporadaAnioInicio(), entidad.getTemporadaAnioFin()),
                entidad.getEstado(), entidad.getUrlSoccerway(), entidad.getApiId(), equipos, posiciones);
    }

    private LigaEntity toEntity(Liga liga) {
        LigaEntity entidad = new LigaEntity(
                liga.id(), liga.nombre(), liga.pais(),
                liga.temporada().anioInicio(), liga.temporada().anioFin(),
                liga.urlSoccerway(), liga.apiId(), liga.estado());
        java.util.Map<UUID, EquipoEntity> equiposById = new java.util.HashMap<>();
        for (Equipo equipo : liga.equipos()) {
            EquipoEntity equipoEntity = new EquipoEntity(equipo.id(), equipo.nombre());
            entidad.agregarEquipo(equipoEntity);
            equiposById.put(equipo.id(), equipoEntity);
        }
        for (PosicionTabla posicion : liga.posiciones()) {
            // Reutiliza el EquipoEntity ya agregado a la liga (misma identidad).
            EquipoEntity equipoEntity = equiposById.get(posicion.equipo().id());
            entidad.agregarPosicion(new PosicionTablaEntity(
                    equipoEntity, posicion.posicion(), posicion.jugados(), posicion.ganados(),
                    posicion.empatados(), posicion.perdidos(), posicion.golesFavor(),
                    posicion.golesContra(), posicion.puntos(), posicion.ultimosResultados()));
        }
        return entidad;
    }
}