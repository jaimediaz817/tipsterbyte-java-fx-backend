// ─────────────────────────────────────────────
// [QUÉ]: Evento emitido cuando una liga se activa (fuentes operativas, BR-001).
// [POR QUÉ]: Interesa al scheduler de ingesta (FASE 15) y a notificaciones (FASE 13).
// [ALTERNATIVAS]: Sin evento, consultar estado al sincronizar; se descarta porque
//                 el inicio de la ingesta es una reacción, no una consulta.
// [RELACIONES]: Emitido por el aggregate Liga; publicado por CU-04.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.event;

import java.time.Instant;
import java.util.UUID;

public record LigaActivada(UUID aggregateId, Instant ocurridoEn) implements DomainEvent {

    public LigaActivada(UUID aggregateId) {
        this(aggregateId, Instant.now());
    }
}