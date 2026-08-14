// ─────────────────────────────────────────────
// [QUÉ]: Evento emitido cuando un partido se programa desde el calendario.
// [POR QUÉ]: Interesa al cache de calendario (FASE 12 Redis) y a la carga de cuotas.
// [RELACIONES]: Emitido por el aggregate Partido; publicado por CU-02.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.event;

import java.time.Instant;
import java.util.UUID;

public record PartidoProgramado(UUID aggregateId, Instant ocurridoEn) implements DomainEvent {

    public PartidoProgramado(UUID aggregateId) {
        this(aggregateId, Instant.now());
    }
}