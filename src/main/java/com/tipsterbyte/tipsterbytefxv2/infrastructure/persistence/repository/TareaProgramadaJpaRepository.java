package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.TareaProgramadaEntity;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TareaProgramadaJpaRepository extends JpaRepository<TareaProgramadaEntity, UUID> {
    Optional<TareaProgramadaEntity> findByLigaIdAndTipoFuente(UUID ligaId, TipoFuenteExtraccion tipoFuente);
    Optional<TareaProgramadaEntity> findByLigaIdIsNullAndTipoFuenteIsNull();
    List<TareaProgramadaEntity> findAllByOrderByPrioridadAsc();
}