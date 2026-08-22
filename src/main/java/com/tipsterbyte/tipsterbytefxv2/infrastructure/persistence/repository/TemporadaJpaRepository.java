// ─────────────────────────────────────────────
// [QUÉ]: Repositorio Spring Data JPA para TemporadaEntity.
// [POR QUÉ]: Proporciona operaciones CRUD y consultas derivadas para la tabla temporadas.
// [ALTERNATIVAS]: Consultas JPQL manuales; se descartan porque Spring Data reduce boilerplate.
// [RELACIONES]: Usado por TemporadaRepositoryJpaAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoTemporada;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.TemporadaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemporadaJpaRepository extends JpaRepository<TemporadaEntity, UUID> {

    List<TemporadaEntity> findByLigaId(UUID ligaId);

    Optional<TemporadaEntity> findByLigaIdAndNombre(UUID ligaId, String nombre);

    // [QUÉ]: Temporada activa de la liga (una sola activa por liga en este modelo).
    Optional<TemporadaEntity> findByLigaIdAndEstado(UUID ligaId, EstadoTemporada estado);
}
