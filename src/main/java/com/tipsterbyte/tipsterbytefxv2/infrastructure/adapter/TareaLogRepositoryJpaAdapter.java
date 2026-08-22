package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.TareaLogRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.TareaLogEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.TareaLogJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ─────────────────────────────────────────────
 * [QUÉ]: Adapter JPA que implementa el puerto TareaLogRepository.
 * [POR QUÉ]: Permite a la capa application usar el repositorio sin conocer JPA.
 * [ALTERNATIVAS]: Anotar el repositorio JPA con @Repository y usarlo directamente;
 *                 se descarta porque acoplaría la capa application a Spring Data.
 * [RELACIONES]: Implementa TareaLogRepository y usa TareaLogJpaRepository.
 *                Usado por CatalogoScheduler y TareaProgramadaController.
 * ─────────────────────────────────────────────
 */
@Repository
public class TareaLogRepositoryJpaAdapter implements TareaLogRepository {

    private final TareaLogJpaRepository jpaRepository;

    public TareaLogRepositoryJpaAdapter(TareaLogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void guardar(TareaLog log) {
        TareaLogEntity entity = TareaLogEntity.fromDomainModel(log);
        jpaRepository.save(entity);
    }

    @Override
    public List<TareaLog> buscarPorTareaProgramadaId(UUID tareaProgramadaId) {
        return jpaRepository.findByTareaProgramadaIdOrderByTimestampDesc(tareaProgramadaId)
                .stream()
                .map(TareaLogEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<TareaLog> listarUltimas(int limite) {
        return jpaRepository.findByOrderByTimestampDesc(PageRequest.of(0, limite))
                .stream()
                .map(TareaLogEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<TareaLog> buscarPorExecutionId(String executionId) {
        return jpaRepository.findByExecutionIdOrderByTimestampDesc(executionId)
                .stream()
                .map(TareaLogEntity::toDomainModel)
                .collect(Collectors.toList());
    }
}
