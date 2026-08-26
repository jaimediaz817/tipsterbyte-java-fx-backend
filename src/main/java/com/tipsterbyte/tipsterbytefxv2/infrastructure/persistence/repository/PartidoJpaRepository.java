// ─────────────────────────────────────────────
// [QUÉ]: Repositorio Spring Data JPA para la entidad PartidoEntity (tabla partidos).
// [POR QUÉ]: Provee las consultas que PartidoRepositoryJpaAdapter necesita: por id,
//            por liga (vía JOIN a través de la temporada), próximos por liga y por
//            liga y fecha. El partido referencia su temporada (Bridge Fix
//            Torneos/Temporadas); la liga se resuelve con temporadas.liga_id.
// [ALTERNATIVAS]: Consultas derivadas por ligaId propio; se descartan porque la
//                 columna liga_id ya no existe en partidos: la relación es
//                 partidos.temporada_id → temporadas.id → temporadas.liga_id.
// [RELACIONES]: Implementa el acceso a datos del puerto PartidoRepository
//               (CU-02, CU-03, CU-05, CU-06, CU-07).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.PartidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PartidoJpaRepository extends JpaRepository<PartidoEntity, UUID> {

    // [QUÉ]: Partidos de una liga (de cualquiera de sus temporadas) vía JOIN temporada→liga.
    @Query("select p from PartidoEntity p where p.temporada.liga.id = :ligaId")
    List<PartidoEntity> findByLigaId(@Param("ligaId") UUID ligaId);

    // [QUÉ]: Próximos = PROGRAMADO o EN_VIVO (los que aún pueden recibir cuotas, CU-03).
    @Query("select p from PartidoEntity p where p.temporada.liga.id = :ligaId and p.estado in :estados")
    List<PartidoEntity> findProximosByLigaId(@Param("ligaId") UUID ligaId,
                                             @Param("estados") Collection<EstadoPartido> estados);

    // [QUÉ]: Partidos de una liga programados dentro del rango [inicio, fin] de una fecha.
    @Query("select p from PartidoEntity p where p.temporada.liga.id = :ligaId "
            + "and p.fechaHora between :inicio and :fin")
    List<PartidoEntity> findByLigaIdAndFechaHoraBetween(@Param("ligaId") UUID ligaId,
                                                        @Param("inicio") LocalDateTime inicio,
                                                        @Param("fin") LocalDateTime fin);

    // [QUÉ]: Busca un partido por temporada y IDs de equipos local y visitante.
    // [POR QUÉ]: HU-14 AC4.3 — el use case necesita verificar si ya existe un partido
    //            entre dos equipos antes de crearlo.
    @Query("select p from PartidoEntity p where p.temporada.id = :temporadaId "
            + "and p.equipoLocalId = :equipoLocalId and p.equipoVisitanteId = :equipoVisitanteId")
    java.util.Optional<PartidoEntity> findByTemporadaIdAndEquipos(
            @Param("temporadaId") UUID temporadaId,
            @Param("equipoLocalId") UUID equipoLocalId,
            @Param("equipoVisitanteId") UUID equipoVisitanteId);
}
