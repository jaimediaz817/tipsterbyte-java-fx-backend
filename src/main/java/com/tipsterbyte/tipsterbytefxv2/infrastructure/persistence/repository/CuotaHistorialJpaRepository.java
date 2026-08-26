package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.CuotaHistorialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CuotaHistorialJpaRepository extends JpaRepository<CuotaHistorialEntity, UUID> {
    List<CuotaHistorialEntity> findByPartidoIdAndCapturadaEnBetweenOrderByCapturadaEnAsc(
            UUID partidoId, Instant desde, Instant hasta);
}
