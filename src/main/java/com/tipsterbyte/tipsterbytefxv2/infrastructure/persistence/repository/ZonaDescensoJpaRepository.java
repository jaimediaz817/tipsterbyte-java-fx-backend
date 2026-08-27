package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.ZonaDescensoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ZonaDescensoJpaRepository extends JpaRepository<ZonaDescensoEntity, UUID> {
    Optional<ZonaDescensoEntity> findByTemporadaId(UUID temporadaId);
}
