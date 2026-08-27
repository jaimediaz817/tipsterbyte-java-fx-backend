// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA que persiste un pronóstico sugerido por una estrategia.
// [POR QUÉ]: HU-16 AC10 — cuando la evaluación supera el umbral de confianza,
//            se guarda como sugerencia para que el tipster la revise.
// [RELACIONES]: Mapea `pronostico_sugerido` (V12), FK a `estrategias` y `partidos`.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pronostico_sugerido")
public class PronosticoSugeridoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "estrategia_id", nullable = false)
    private UUID estrategiaId;

    @Column(name = "partido_id", nullable = false)
    private UUID partidoId;

    @Column(name = "score", nullable = false, precision = 4, scale = 3)
    private BigDecimal score;

    @Column(name = "criterios_cumplidos", nullable = false)
    private int criteriosCumplidos;

    @Column(name = "criterios_fallidos", nullable = false)
    private int criteriosFallidos;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PronosticoSugeridoEntity() {}

    public PronosticoSugeridoEntity(UUID id, UUID estrategiaId, UUID partidoId, BigDecimal score,
                                    int criteriosCumplidos, int criteriosFallidos, Instant createdAt) {
        this.id = id;
        this.estrategiaId = estrategiaId;
        this.partidoId = partidoId;
        this.score = score;
        this.criteriosCumplidos = criteriosCumplidos;
        this.criteriosFallidos = criteriosFallidos;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getEstrategiaId() { return estrategiaId; }
    public UUID getPartidoId() { return partidoId; }
    public BigDecimal getScore() { return score; }
    public int getCriteriosCumplidos() { return criteriosCumplidos; }
    public int getCriteriosFallidos() { return criteriosFallidos; }
    public Instant getCreatedAt() { return createdAt; }
}
