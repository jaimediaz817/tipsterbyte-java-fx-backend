package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.EquiposAliasEntity;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EquiposAliasJpaRepository extends JpaRepository<EquiposAliasEntity, UUID> {
    List<EquiposAliasEntity> findByNombreExternoIgnoreCaseAndTemporadaId(String nombreExterno, UUID temporadaId);
    List<EquiposAliasEntity> findByTemporadaIdAndFuenteTipo(UUID temporadaId, TipoFuenteExtraccion fuenteTipo);
}
