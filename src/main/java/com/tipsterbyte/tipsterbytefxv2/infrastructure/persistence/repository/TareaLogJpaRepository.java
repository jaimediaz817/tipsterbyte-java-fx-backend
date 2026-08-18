package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.TareaLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * ─────────────────────────────────────────────
 * [QUÉ]: Repositorio Spring Data JPA para TareaLogEntity.
 * [POR QUÉ]: Provee métodos CRUD y de búsqueda estándar.
 * [ALTERNATIVAS]: Definir queries manuales; se descarta porque
 *                 Spring Data JPA es suficiente para nuestras necesidades.
 * [RELACIONES]: Implementado por TareaLogRepositoryJpaAdapter.
 *                Extiende JpaRepository<TareaLogEntity, UUID>.
 * ─────────────────────────────────────────────
 */
public interface TareaLogJpaRepository extends JpaRepository<TareaLogEntity, UUID> {
    List<TareaLogEntity> findByTareaProgramadaIdOrderByTimestampDesc(UUID tareaProgramadaId);

    // [QUÉ]: Últimos (n) logs globales, más reciente primero; el límite lo inyecta
    //        Pageable en runtime (findTopN exige una N fija en el nombre del método).
    List<TareaLogEntity> findByOrderByTimestampDesc(Pageable pageable);
}
