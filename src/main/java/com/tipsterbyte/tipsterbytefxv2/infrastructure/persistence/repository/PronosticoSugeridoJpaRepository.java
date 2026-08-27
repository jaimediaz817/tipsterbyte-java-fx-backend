package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.PronosticoSugeridoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PronosticoSugeridoJpaRepository extends JpaRepository<PronosticoSugeridoEntity, UUID> {
    List<PronosticoSugeridoEntity> findByEstrategiaIdOrderByCreatedAtDesc(UUID estrategiaId);
    List<PronosticoSugeridoEntity> findByEstrategiaIdAndPartidoId(UUID estrategiaId, UUID partidoId);
    void deleteByEstrategiaId(UUID estrategiaId);
}
