// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA que persiste el aggregate Estrategia con criterios embebidos en JSONB.
// [POR QUÉ]: HU-16 — la estrategia se almacena con sus criterios como JSONB para
//            flexibilidad de esquema (nuevos tipos de criterio sin migración).
// [ALTERNATIVAS]: Tabla separada de criterios con FK; se descarta por complejidad innecesaria.
// [RELACIONES]: Mapea `estrategias` (V12), compone criterios vía `@Convert`.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "estrategias")
public class EstrategiaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "tipster_id", nullable = false)
    private UUID tipsterId;

    @Column(name = "mercado", nullable = false, length = 30)
    private String mercado;

    @Column(name = "max_partidos")
    private Integer maxPartidos;

    @Column(name = "confianza_minima", precision = 3, scale = 2)
    private BigDecimal confianzaMinima;

    @Column(name = "activa", nullable = false)
    private boolean activa;

    @Column(name = "criterios", columnDefinition = "jsonb", nullable = false)
    private String criteriosJson;

    @Column(name = "liga_ids", columnDefinition = "uuid[]")
    private UUID[] ligaIds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // JPA
    protected EstrategiaEntity() {}

    public EstrategiaEntity(UUID id, String nombre, UUID tipsterId, String mercado,
                            Integer maxPartidos, BigDecimal confianzaMinima, boolean activa,
                            String criteriosJson, UUID[] ligaIds, Instant createdAt) {
        this.id = id;
        this.nombre = nombre;
        this.tipsterId = tipsterId;
        this.mercado = mercado;
        this.maxPartidos = maxPartidos;
        this.confianzaMinima = confianzaMinima;
        this.activa = activa;
        this.criteriosJson = criteriosJson;
        this.ligaIds = ligaIds;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public UUID getTipsterId() { return tipsterId; }
    public String getMercado() { return mercado; }
    public Integer getMaxPartidos() { return maxPartidos; }
    public BigDecimal getConfianzaMinima() { return confianzaMinima; }
    public boolean isActiva() { return activa; }
    public String getCriteriosJson() { return criteriosJson; }
    public UUID[] getLigaIds() { return ligaIds; }
    public Instant getCreatedAt() { return createdAt; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setMercado(String mercado) { this.mercado = mercado; }
    public void setMaxPartidos(Integer maxPartidos) { this.maxPartidos = maxPartidos; }
    public void setConfianzaMinima(BigDecimal confianzaMinima) { this.confianzaMinima = confianzaMinima; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public void setCriteriosJson(String criteriosJson) { this.criteriosJson = criteriosJson; }
    public void setLigaIds(UUID[] ligaIds) { this.ligaIds = ligaIds; }
}
