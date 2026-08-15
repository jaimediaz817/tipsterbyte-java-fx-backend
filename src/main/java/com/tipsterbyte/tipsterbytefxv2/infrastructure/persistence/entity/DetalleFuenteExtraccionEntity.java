// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA del detalle de fuente de extracción (tabla detalle_fuentes_extraccion):
//        asocia una liga con una fuente y su URL (path_to_scrape).
// [POR QUÉ]: Es la representación persistente del entity DetalleFuenteExtraccion del
//            dominio. La unicidad por (liga_id, tipo) garantiza que cada liga tenga a
//            lo sumo una URL por fuente. Usa tabla propia para no colisionar con el
//            esquema Python (detalle_fuente_extraccion) en la misma BD compartida.
// [ALTERNATIVAS]: Guardar la URL en LigaEntity; se descarta porque una liga tiene 3 URLs
//                 (una por fuente) y el catálogo de fuentes es gestionable (CU-11).
// [RELACIONES]: Mapea el entity DetalleFuenteExtraccion (CU-04/CU-11). Convertida por
//               DetalleFuenteExtraccionRepositoryJpaAdapter. Refiere FuenteExtraccionEntity.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "detalle_fuentes_extraccion",
        uniqueConstraints = @UniqueConstraint(name = "uk_detalle_fuente_liga_tipo", columnNames = {"liga_id", "tipo"}))
public class DetalleFuenteExtraccionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "liga_id", nullable = false)
    private UUID ligaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fuente_id", nullable = false)
    private FuenteExtraccionEntity fuente;

    // Columna duplicada del tipo de la fuente: evita una consulta extra y permite el
    // índice único (liga_id, tipo) sin dependencia de la FK a la fuente.
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion tipo;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "activa", nullable = false)
    private boolean activa;

    protected DetalleFuenteExtraccionEntity() {
    }

    public DetalleFuenteExtraccionEntity(UUID id, UUID ligaId, FuenteExtraccionEntity fuente,
                                         com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion tipo,
                                         String url, boolean activa) {
        this.id = id;
        this.ligaId = ligaId;
        this.fuente = fuente;
        this.tipo = tipo;
        this.url = url;
        this.activa = activa;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLigaId() {
        return ligaId;
    }

    public FuenteExtraccionEntity getFuente() {
        return fuente;
    }

    public com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion getTipo() {
        return tipo;
    }

    public String getUrl() {
        return url;
    }

    public boolean isActiva() {
        return activa;
    }
}
