// ─────────────────────────────────────────────
// [QUÉ]: Repositorio Spring Data JPA para la entidad FuenteExtraccionEntity (tabla fuentes_extraccion).
// [POR QUÉ]: Provee las consultas derivadas que FuenteExtraccionRepositoryJpaAdapter
//            necesita: por id, por tipo (clave natural) y listado completo.
// [ALTERNATIVAS]: Consultas JPQL manuales; se descartan porque Spring Data deriva
//                 las queries con suficiente expresividad para este puerto.
// [RELACIONES]: Implementa el acceso a datos del puerto FuenteExtraccionRepository (CU-11).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.FuenteExtraccionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FuenteExtraccionJpaRepository extends JpaRepository<FuenteExtraccionEntity, UUID> {

    Optional<FuenteExtraccionEntity> findByTipo(TipoFuenteExtraccion tipo);
}
