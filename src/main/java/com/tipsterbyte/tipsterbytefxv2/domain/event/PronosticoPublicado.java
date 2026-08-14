// ─────────────────────────────────────────────
// [QUÉ]: Evento emitido cuando un tipster publica un pronóstico (CU-07).
// [POR QUÉ]: Interesa a las notificaciones a suscriptores (FASE 13 RabbitMQ).
// [RELACIONES]: Emitido por el aggregate Pronostico; publicado por CU-07.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.event;

import java.time.Instant;
import java.util.UUID;

public record PronosticoPublicado(UUID aggregateId, Instant ocurridoEn) implements DomainEvent {

    public PronosticoPublicado(UUID aggregateId) {
        this(aggregateId, Instant.now());
    }
}