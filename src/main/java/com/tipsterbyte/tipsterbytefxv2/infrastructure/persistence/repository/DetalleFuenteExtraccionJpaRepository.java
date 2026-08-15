// ─────────────────────────────────────────────
// [QUÉ]: Repositorio Spring Data JPA para la entidad DetalleFuenteExtraccionEntity
//        (tabla detalle_fuentes_extraccion).
// [POR QUÉ]: Provee las consultas derivadas que DetalleFuenteExtraccionRepositoryJpaAdapter
//            necesita: por (ligaId, tipo) con unicidad y listado por liga.
// [ALTERNATIVAS]: Consultas JPQL manuales; se descartan porque Spring Data deriva
//                 las queries con suficiente expresividad para este puerto.
// [RELACIONES]: Implementa el acceso a datos del puerto DetalleFuenteExtraccionRepository
//               (CU-04, CU-11).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.DetalleFuenteExtraccionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DetalleFuenteExtraccionJpaRepository extends JpaRepository<DetalleFuenteExtraccionEntity, UUID> {

    Optional<DetalleFuenteExtraccionEntity> findByLigaIdAndTipo(UUID ligaId, TipoFuenteExtraccion tipo);

    List<DetalleFuenteExtraccionEntity> findByLigaId(UUID ligaId);
}
