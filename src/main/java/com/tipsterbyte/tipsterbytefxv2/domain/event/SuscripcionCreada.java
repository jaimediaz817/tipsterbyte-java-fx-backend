// ─────────────────────────────────────────────
// [QUÉ]: Evento emitido cuando un cliente se suscribe a un tipster (CU-09).
// [POR QUÉ]: Interesa a facturación y notificaciones (FASE 13) y habilita BR-006.
// [RELACIONES]: Emitido por el aggregate Suscripcion; publicado por CU-09.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.event;

import java.time.Instant;
import java.util.UUID;

public record SuscripcionCreada(UUID aggregateId, Instant ocurridoEn) implements DomainEvent {

    public SuscripcionCreada(UUID aggregateId) {
        this(aggregateId, Instant.now());
    }
}