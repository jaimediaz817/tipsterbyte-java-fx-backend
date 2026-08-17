// ─────────────────────────────────────────────
// [QUÉ]: Repositorio Spring Data JPA para la entidad PaisInteresEntity (tabla paises_interes).
// [POR QUÉ]: Provee las consultas derivadas que PaisInteresRepositoryJpaAdapter
//            necesita: por iso_alpha2 (clave natural) y listado por prioridad.
// [ALTERNATIVAS]: Consultas JPQL manuales; se descartan porque Spring Data deriva
//                 las queries con suficiente expresividad para este puerto.
// [RELACIONES]: Implementa el acceso a datos del puerto PaisInteresRepository (CU-14).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.PaisInteresEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaisInteresJpaRepository extends JpaRepository<PaisInteresEntity, UUID> {

    Optional<PaisInteresEntity> findByIsoAlpha2(String isoAlpha2);

    List<PaisInteresEntity> findAllByOrderByPrioridadAsc();

    void deleteByIsoAlpha2(String isoAlpha2);
}