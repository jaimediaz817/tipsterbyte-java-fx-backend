// ─────────────────────────────────────────────
// [QUÉ]: Repositorio Spring Data JPA para la entidad PaisEntity (tabla paises).
// [POR QUÉ]: Provee las consultas derivadas que PaisRepositoryJpaAdapter necesita:
//            por id, por iso_alpha2 (clave natural) y listado completo.
// [ALTERNATIVAS]: Consultas JPQL manuales; se descartan porque Spring Data deriva
//                 las queries con suficiente expresividad para este puerto.
// [RELACIONES]: Implementa el acceso a datos del puerto PaisRepository (CU-10).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.PaisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaisJpaRepository extends JpaRepository<PaisEntity, UUID> {

    Optional<PaisEntity> findByIsoAlpha2(String isoAlpha2);
}