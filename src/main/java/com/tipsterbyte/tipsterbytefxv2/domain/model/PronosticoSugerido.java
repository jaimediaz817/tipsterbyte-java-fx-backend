// ─────────────────────────────────────────────
// [QUÉ]: Entity que representa un pronóstico sugerido por una estrategia tras evaluación.
// [POR QUÉ]: HU-16 AC10 — cuando la evaluación de una estrategia determina que un partido
//            supera el umbral de confianza, se guarda como sugerencia para que el tipster
//            la revise antes de publicar como pronóstico oficial.
// [ALTERNATIVAS]: Devolver solo en memoria; se descarta porque el tipster quiere ver
//                 sugerencias acumuladas sin re-evaluar cada vez.
// [RELACIONES]: CU-25 `ConsultarSugerenciasUseCase` lee; CU-24 `EvaluarEstrategiaUseCase` escribe.
//               FK a `estrategias` y `partidos`.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PronosticoSugerido {

    private final UUID id;
    private final UUID estrategiaId;
    private final UUID partidoId;
    private final BigDecimal score;
    private final int criteriosCumplidos;
    private final int criteriosFallidos;
    private final Instant createdAt;

    public PronosticoSugerido(UUID estrategiaId, UUID partidoId, BigDecimal score,
                              int criteriosCumplidos, int criteriosFallidos) {
        this(UUID.randomUUID(), estrategiaId, partidoId, score,
                criteriosCumplidos, criteriosFallidos, Instant.now());
    }

    public PronosticoSugerido(UUID id, UUID estrategiaId, UUID partidoId, BigDecimal score,
                              int criteriosCumplidos, int criteriosFallidos, Instant createdAt) {
        this.id = id;
        this.estrategiaId = estrategiaId;
        this.partidoId = partidoId;
        this.score = score;
        this.criteriosCumplidos = criteriosCumplidos;
        this.criteriosFallidos = criteriosFallidos;
        this.createdAt = createdAt;
    }

    public UUID id() { return id; }
    public UUID estrategiaId() { return estrategiaId; }
    public UUID partidoId() { return partidoId; }
    public BigDecimal score() { return score; }
    public int criteriosCumplidos() { return criteriosCumplidos; }
    public int criteriosFallidos() { return criteriosFallidos; }
    public Instant createdAt() { return createdAt; }
}
