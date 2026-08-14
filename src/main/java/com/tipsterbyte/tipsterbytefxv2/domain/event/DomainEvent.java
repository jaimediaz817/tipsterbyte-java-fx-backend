// ─────────────────────────────────────────────
// [QUÉ]: Interfaz base que todo evento de dominio debe implementar.
// [POR QUÉ]: Permite a los aggregates recolectar eventos de forma genérica
//            (List<DomainEvent>) y entregarlos al caso de uso con pullEventos().
// [ALTERNATIVAS]: Sin interfaz, cada aggregate con lista tipada; se descarta porque
//                 complica la recolección y la futura publicación (FASE 13).
// [RELACIONES]: Implementada por LigaActivada, PartidoProgramado, CuotaActualizada,
//               PronosticoPublicado y SuscripcionCreada.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.event;

import java.time.Instant;

public interface DomainEvent {

    // [QUÉ]: Identidad del aggregate que originó el evento.
    java.util.UUID aggregateId();

    // [QUÉ]: Momento en que ocurrió el evento.
    Instant ocurridoEn();
}