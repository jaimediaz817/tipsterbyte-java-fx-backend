// ─────────────────────────────────────────────
// [QUÉ]: Repositorio Spring Data JPA para la entidad PronosticoEntity (tabla pronosticos).
// [POR QUÉ]: Provee las consultas derivadas que PronosticoRepositoryJpaAdapter necesita:
//            por id, PUBLICADO por colección de partidos, y persistencia.
// [ALTERNATIVAS]: JPQL manual; se descarta porque Spring Data deriva las queries.
// [RELACIONES]: Implementa el acceso a datos del puerto PronosticoRepository
//               (CU-06, CU-07, CU-08).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPronostico;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.PronosticoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PronosticoJpaRepository extends JpaRepository<PronosticoEntity, UUID> {

    // Pronósticos visibles de una colección de partidos (CU-08 consulta solo PUBLICADO).
    List<PronosticoEntity> findByEstadoAndPartidoIdIn(EstadoPronostico estado, Collection<UUID> partidoIds);
}