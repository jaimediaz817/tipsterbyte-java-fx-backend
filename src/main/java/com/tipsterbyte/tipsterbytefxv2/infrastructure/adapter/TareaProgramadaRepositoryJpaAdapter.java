package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.TareaProgramadaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.TareaProgramadaEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.TareaProgramadaJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class TareaProgramadaRepositoryJpaAdapter implements TareaProgramadaRepository {

    private final TareaProgramadaJpaRepository jpaRepository;

    public TareaProgramadaRepositoryJpaAdapter(TareaProgramadaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<TareaProgramada> buscarPorLigaIdYTipoFuente(UUID ligaId, TipoFuenteExtraccion tipoFuente) {
        return jpaRepository.findByLigaIdAndTipoFuente(ligaId, tipoFuente)
                .map(this::toDomainModel);
    }

    @Override
    public Optional<TareaProgramada> buscarGlobal() {
        return jpaRepository.findByLigaIdIsNullAndTipoFuenteIsNull()
                .map(this::toDomainModel);
    }

    @Override
    public List<TareaProgramada> listarPorPrioridadAsc() {
        return jpaRepository.findAllByOrderByPrioridadAsc().stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public TareaProgramada guardar(TareaProgramada tarea) {
        TareaProgramadaEntity entity = toEntity(tarea);
        TareaProgramadaEntity saved = jpaRepository.save(entity);
        return toDomainModel(saved);
    }

    @Override
    public void eliminarPorId(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Optional<TareaProgramada> encontrarPorId(UUID id) {
        return jpaRepository.findById(id)
                .map(this::toDomainModel);
    }

    @Override
    public List<TareaProgramada> buscarPorLigaId(UUID ligaId) {
        return jpaRepository.findByLigaId(ligaId).stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList());
    }

    private TareaProgramada toDomainModel(TareaProgramadaEntity entity) {
        return new TareaProgramada(
                entity.getId(),
                entity.getLigaId(),
                entity.getTipoFuente(),
                entity.getPrioridad(),
                entity.getCronExpression(),
                entity.isActiva(),
                entity.getCreatedAt(),
                entity.getPrimerDisparo()
        );
    }

    private TareaProgramadaEntity toEntity(TareaProgramada domain) {
        return new TareaProgramadaEntity(
                domain.id(),
                domain.ligaId(),
                domain.tipoFuente(),
                domain.prioridad(),
                domain.cronExpression(),
                domain.activa(),
                domain.createdAt(),
                domain.primerDisparo()
        );
    }
}