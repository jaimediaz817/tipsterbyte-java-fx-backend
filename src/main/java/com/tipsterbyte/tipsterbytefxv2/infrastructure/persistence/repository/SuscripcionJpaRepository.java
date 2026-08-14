// ─────────────────────────────────────────────
// [QUÉ]: Repositorio Spring Data JPA para la entidad SuscripcionEntity (tabla suscripciones).
// [POR QUÉ]: Provee la consulta derivada que SuscripcionRepositoryJpaAdapter necesita:
//            suscripciones ACTIVA de un cliente (CU-08 valida BR-006) y persistencia.
// [ALTERNATIVAS]: JPQL manual; se descarta porque Spring Data deriva la query.
// [RELACIONES]: Implementa el acceso a datos del puerto SuscripcionRepository (CU-08, CU-09).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoSuscripcion;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.SuscripcionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SuscripcionJpaRepository extends JpaRepository<SuscripcionEntity, UUID> {

    List<SuscripcionEntity> findByClienteIdAndEstado(UUID clienteId, EstadoSuscripcion estado);
}