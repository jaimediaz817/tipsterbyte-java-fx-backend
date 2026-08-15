// ─────────────────────────────────────────────
// [QUÉ]: Repositorio Spring Data JPA para la entidad LigaEntity (tabla ligas).
// [POR QUÉ]: Provee las consultas derivadas que LigaRepositoryJpaAdapter necesita:
//            por id, por estado (ACTIVA) y persistencia. Abstae del SQL a Hibernate.
// [ALTERNATIVAS]: Consultas JPQL manuales; se descartan porque Spring Data deriva
//                 las queries con suficiente expresividad para este puerto.
// [RELACIONES]: Implementa el acceso a datos del puerto LigaRepository (CU-01, CU-02, CU-04).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.LigaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LigaJpaRepository extends JpaRepository<LigaEntity, UUID> {

    List<LigaEntity> findByEstado(EstadoLiga estado);

    Optional<LigaEntity> findByUrlSoccerway(String urlSoccerway);
}