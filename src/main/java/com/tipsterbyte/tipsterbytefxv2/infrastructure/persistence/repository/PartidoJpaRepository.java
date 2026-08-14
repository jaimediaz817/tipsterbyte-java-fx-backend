// ─────────────────────────────────────────────
// [QUÉ]: Repositorio Spring Data JPA para la entidad PartidoEntity (tabla partidos).
// [POR QUÉ]: Provee las consultas derivadas que PartidoRepositoryJpaAdapter necesita:
//            por id, por liga, próximos por liga, por liga y fecha, y persistencia.
// [ALTERNATIVAS]: JPQL manual; se descarta porque Spring Data deriva las queries.
// [RELACIONES]: Implementa el acceso a datos del puerto PartidoRepository
//               (CU-02, CU-03, CU-05, CU-06, CU-07).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.PartidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PartidoJpaRepository extends JpaRepository<PartidoEntity, UUID> {

    List<PartidoEntity> findByLigaId(UUID ligaId);

    // Próximos = PROGRAMADO o EN_VIVO (los que aún pueden recibir cuotas, CU-03).
    List<PartidoEntity> findByLigaIdAndEstadoIn(UUID ligaId, Collection<EstadoPartido> estados);

    // Partidos de una liga programados dentro del rango [inicio, fin] de una fecha.
    List<PartidoEntity> findByLigaIdAndFechaHoraBetween(UUID ligaId, LocalDateTime inicio, LocalDateTime fin);
}