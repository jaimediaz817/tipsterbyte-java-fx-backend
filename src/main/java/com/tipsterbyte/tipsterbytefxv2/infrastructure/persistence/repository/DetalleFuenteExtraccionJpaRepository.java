// ─────────────────────────────────────────────
// [QUÉ]: Repositorio Spring Data JPA para la entidad DetalleFuenteExtraccionEntity
//        (tabla detalle_fuentes_extraccion).
// [POR QUÉ]: Provee las consultas que DetalleFuenteExtraccionRepositoryJpaAdapter
//            necesita: por (temporadaId, tipo) con unicidad, por liga vía JOIN a
//            través de la temporada, y listado por liga. El detalle referencia su
//            temporada (Bridge Fix Torneos/Temporadas); la liga se resuelve con
//            temporadas.liga_id.
// [ALTERNATIVAS]: Consultas derivadas por ligaId propio; se descartan porque la
//                 columna liga_id ya no existe en detalle_fuentes_extraccion.
// [RELACIONES]: Implementa el acceso a datos del puerto DetalleFuenteExtraccionRepository
//               (CU-04, CU-11) y es consultado por los adapters de fuentes (CU-01/02/03).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.DetalleFuenteExtraccionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DetalleFuenteExtraccionJpaRepository extends JpaRepository<DetalleFuenteExtraccionEntity, UUID> {

    // [QUÉ]: Detalle de una temporada concreta para un tipo de fuente (único por UK).
    //        El guion bajo navega la asociación temporada.id (la propiedad es 'temporada').
    Optional<DetalleFuenteExtraccionEntity> findByTemporada_IdAndTipo(UUID temporadaId, TipoFuenteExtraccion tipo);

    // [QUÉ]: Detalle de una liga para un tipo de fuente, resuelto vía JOIN
    //        temporada → liga (compatibilidad con adapters de fuentes).
    @Query("select d from DetalleFuenteExtraccionEntity d "
            + "join d.temporada t "
            + "where t.liga.id = :ligaId and d.tipo = :tipo")
    Optional<DetalleFuenteExtraccionEntity> findByLigaIdAndTipo(@Param("ligaId") UUID ligaId,
                                                                @Param("tipo") TipoFuenteExtraccion tipo);

    // [QUÉ]: Todos los detalles de fuentes de una liga (todas sus temporadas).
    @Query("select d from DetalleFuenteExtraccionEntity d "
            + "join d.temporada t "
            + "where t.liga.id = :ligaId")
    List<DetalleFuenteExtraccionEntity> findByLigaId(@Param("ligaId") UUID ligaId);
}
