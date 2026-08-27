// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA que persiste la zona de descenso de una temporada.
// [POR QUÉ]: HU-16 AC4 — la posición de descenso se configura por temporada (no genérica).
// [RELACIONES]: Mapea `zona_descenso` (V12), FK a `temporadas`.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "zona_descenso")
public class ZonaDescensoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "temporada_id", nullable = false, unique = true)
    private UUID temporadaId;

    @Column(name = "posicion_descenso", nullable = false)
    private Integer posicionDescenso;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    protected ZonaDescensoEntity() {}

    public ZonaDescensoEntity(UUID id, UUID temporadaId, Integer posicionDescenso, String descripcion) {
        this.id = id;
        this.temporadaId = temporadaId;
        this.posicionDescenso = posicionDescenso;
        this.descripcion = descripcion;
    }

    public UUID getId() { return id; }
    public UUID getTemporadaId() { return temporadaId; }
    public Integer getPosicionDescenso() { return posicionDescenso; }
    public String getDescripcion() { return descripcion; }

    public void setPosicionDescenso(Integer posicionDescenso) { this.posicionDescenso = posicionDescenso; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
