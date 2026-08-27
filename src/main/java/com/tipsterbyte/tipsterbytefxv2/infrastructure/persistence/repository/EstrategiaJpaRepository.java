package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.EstrategiaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EstrategiaJpaRepository extends JpaRepository<EstrategiaEntity, UUID> {
    List<EstrategiaEntity> findByTipsterIdOrderByCreatedAtDesc(UUID tipsterId);
    List<EstrategiaEntity> findByTipsterIdAndActivaTrue(UUID tipsterId);
    List<EstrategiaEntity> findByActivaTrue();
    long countByTipsterIdAndActivaTrue(UUID tipsterId);
}
