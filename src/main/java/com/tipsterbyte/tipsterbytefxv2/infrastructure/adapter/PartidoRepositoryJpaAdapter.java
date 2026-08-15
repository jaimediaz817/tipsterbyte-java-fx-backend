// ─────────────────────────────────────────────
// [QUÉ]: Adapter JPA del puerto PartidoRepository. Convierte entre el aggregate Partido
//        del dominio y PartidoEntity de persistencia (mapper).
// [POR QUÉ]: Implementa el puerto definido en application sin que el dominio conozca
//            JPA. Lecturas @Transactional(readOnly = true) para mapear cuotas (LAZY).
//            Los equipos se denormalizan (id + nombre) según la decisión de FASE 8.
// [ALTERNATIVAS]: @EntityGraph para cuotas; se descarta por simplicidad.
// [RELACIONES]: Implementa application.port.PartidoRepository (CU-02, CU-03, CU-05,
//               CU-06, CU-07).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Cuota;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FechaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Resultado;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.CuotaEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.PartidoEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.PartidoJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PartidoRepositoryJpaAdapter implements PartidoRepository {

    private static final List<EstadoPartido> ESTADOS_PROXIMOS =
            List.of(EstadoPartido.PROGRAMADO, EstadoPartido.EN_VIVO);

    private final PartidoJpaRepository jpaRepository;

    public PartidoRepositoryJpaAdapter(PartidoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Partido> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(this::toDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Partido> buscarPorLiga(UUID ligaId) {
        return jpaRepository.findByLigaId(ligaId).stream().map(this::toDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Partido> buscarProximosPorLiga(UUID ligaId) {
        return jpaRepository.findByLigaIdAndEstadoIn(ligaId, ESTADOS_PROXIMOS).stream()
                .map(this::toDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Partido> buscarPorLigaYFecha(UUID ligaId, LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        return jpaRepository.findByLigaIdAndFechaHoraBetween(ligaId, inicio, fin).stream()
                .map(this::toDominio)
                .toList();
    }

    @Override
    @Transactional
    public void guardar(Partido partido) {
        jpaRepository.save(toEntity(partido));
    }

    private Partido toDominio(PartidoEntity entidad) {
        List<Cuota> cuotas = entidad.getCuotas().stream()
                .map(c -> new Cuota(c.getMercado(), c.getValor()))
                .toList();
        Resultado resultado = (entidad.getResultadoGolesLocal() != null
                && entidad.getResultadoGolesVisitante() != null)
                ? new Resultado(entidad.getResultadoGolesLocal(), entidad.getResultadoGolesVisitante())
                : null;
        return Partido.reconstruir(
                entidad.getId(), entidad.getLigaId(),
                new Equipo(entidad.getEquipoLocalId(), entidad.getEquipoLocalNombre()),
                new Equipo(entidad.getEquipoVisitanteId(), entidad.getEquipoVisitanteNombre()),
                new FechaProgramada(entidad.getFechaHora()), entidad.getEstado(),
                cuotas, resultado);
    }

    private PartidoEntity toEntity(Partido partido) {
        PartidoEntity entidad = new PartidoEntity(
                partido.id(), partido.ligaId(),
                partido.equipoLocal().id(), partido.equipoLocal().nombre(),
                partido.equipoVisitante().id(), partido.equipoVisitante().nombre(),
                partido.fechaProgramada().fechaHora(), partido.estado(),
                partido.resultado() != null ? partido.resultado().golesLocal() : null,
                partido.resultado() != null ? partido.resultado().golesVisitante() : null);
        for (Cuota cuota : partido.cuotas()) {
            entidad.agregarCuota(new CuotaEntity(cuota.mercado(), cuota.valor()));
        }
        return entidad;
    }
}