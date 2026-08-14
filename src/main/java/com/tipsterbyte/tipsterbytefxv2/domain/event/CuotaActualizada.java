// ─────────────────────────────────────────────
// [QUÉ]: Evento emitido cuando la fuente de odds entrega cuotas nuevas (CU-03).
// [POR QUÉ]: Interesa al cache de cuotas (FASE 12) y a la validación de pronósticos
//            con cuota vigente (BR-004).
// [RELACIONES]: Emitido por el aggregate Partido; publicado por CU-03.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.event;

import java.time.Instant;
import java.util.UUID;

public record CuotaActualizada(UUID aggregateId, Instant ocurridoEn) implements DomainEvent {

    public CuotaActualizada(UUID aggregateId) {
        this(aggregateId, Instant.now());
    }
}