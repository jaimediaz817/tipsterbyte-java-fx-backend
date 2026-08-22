// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA del detalle de fuente de extracción (tabla detalle_fuentes_extraccion):
//        asocia una TEMPORADA con una fuente y su URL (path_to_scrape).
// [POR QUÉ]: Es la representación persistente del entity DetalleFuenteExtraccion del
//            dominio. La unicidad por (temporada_id, tipo) garantiza que cada temporada
//            tenga a lo sumo una URL por fuente. Usa tabla propia para no colisionar con
//            el esquema Python (detalle_fuente_extraccion) en la misma BD compartida.
//            El vínculo con la liga se resuelve vía JOIN a través de la temporada
//            (temporadas.liga_id): las URLs son de una temporada concreta, no de la liga
//            genérica (Bridge Fix Torneos/Temporadas).
// [ALTERNATIVAS]: Guardar la URL en TemporadaEntity; se descarta porque una temporada
//                 tiene 3 URLs (una por fuente) y el catálogo de fuentes es gestionable
//                 (CU-11). Mantener columna liga_id propia; se descarta porque duplicaba
//                 el dato de temporadas.liga_id y permitía inconsistencias.
// [RELACIONES]: Mapea el entity DetalleFuenteExtraccion (CU-04/CU-11). Convertida por
//               DetalleFuenteExtraccionRepositoryJpaAdapter. Refiere TemporadaEntity y
//               FuenteExtraccionEntity. Consultada por los adapters de fuentes vía
//               buscarPorLigaYTipo (JOIN interno) o buscarPorTemporadaYTipo.
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
        uniqueConstraints = @UniqueConstraint(name = "uk_detalle_fuente_temporada_tipo", columnNames = {"temporada_id", "tipo"}))
public class DetalleFuenteExtraccionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Temporada a la que aplica la URL (FK detalle_fuentes_extraccion.temporada_id →
    // temporadas.id). La liga se deriva vía temporada.liga en las consultas por liga.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "temporada_id", nullable = false)
    private TemporadaEntity temporada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fuente_id", nullable = false)
    private FuenteExtraccionEntity fuente;

    // Columna duplicada del tipo de la fuente: evita una consulta extra y permite el
    // índice único (temporada_id, tipo) sin dependencia de la FK a la fuente.
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion tipo;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "activa", nullable = false)
    private boolean activa;

    protected DetalleFuenteExtraccionEntity() {
    }

    public DetalleFuenteExtraccionEntity(UUID id, TemporadaEntity temporada, FuenteExtraccionEntity fuente,
                                         com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion tipo,
                                         String url, boolean activa) {
        this.id = id;
        this.temporada = temporada;
        this.fuente = fuente;
        this.tipo = tipo;
        this.url = url;
        this.activa = activa;
    }

    public UUID getId() {
        return id;
    }

    public TemporadaEntity getTemporada() {
        return temporada;
    }

    // [QUÉ]: Conveniencia de lectura del id de la temporada (evita inicializar el proxy).
    public UUID getTemporadaId() {
        return temporada != null ? temporada.getId() : null;
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
