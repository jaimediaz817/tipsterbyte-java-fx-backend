// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia del aggregate Partido.
// [POR QUÉ]: Abstrae la persistencia del dominio (adapter JPA en FASE 8). Los casos
//            de uso CU-02, CU-03, CU-05, CU-06 y CU-07 guardan/recuperan partidos.
// [ALTERNATIVAS]: Trabajar con la entidad JPA directamente en application; se descarta
//                 porque acoplaría la capa de aplicación a infraestructura.
// [RELACIONES]: CU-02, CU-03, CU-05, CU-06, CU-07. Implementado por PartidoRepositoryJpaAdapter (FASE 8).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartidoRepository {

    // [QUÉ]: Recupera un partido por su id, o vacío si no existe.
    Optional<Partido> buscarPorId(UUID id);

    // [QUÉ]: Recupera los partidos de una liga.
    List<Partido> buscarPorLiga(UUID ligaId);

    // [QUÉ]: Recupera los partidos próximos (PROGRAMADO o EN_VIVO) de una liga.
    // [POR QUÉ]: CU-03 sincroniza cuotas solo de partidos no finalizados.
    List<Partido> buscarProximosPorLiga(UUID ligaId);

    // [QUÉ]: Recupera los partidos de una liga programados en una fecha concreta.
    // [POR QUÉ]: CU-08 filtra pronósticos por liga y fecha.
    List<Partido> buscarPorLigaYFecha(UUID ligaId, java.time.LocalDate fecha);

    // [QUÉ]: Busca un partido por temporada y IDs de equipos local y visitante.
    // [POR QUÉ]: HU-14 AC4.3 — el use case necesita verificar si ya existe un partido
    //            entre dos equipos antes de crearlo.
    Optional<Partido> buscarPorTemporadaYEquipos(UUID temporadaId, UUID equipoLocalId, UUID equipoVisitanteId);

    // [QUÉ]: Persiste un partido (crea o actualiza según exista el id).
    void guardar(Partido partido);

}