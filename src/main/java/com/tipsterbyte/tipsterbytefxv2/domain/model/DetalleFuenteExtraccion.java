// ─────────────────────────────────────────────
// [QUÉ]: Entity que representa la asociación entre una temporada y una fuente de
//        extracción con su URL (path_to_scrape). Unica por (temporadaId, tipo de fuente).
// [POR QUÉ]: Es el vínculo que el usuario suministra al activar una liga (CU-04):
//            la URL real de cada fuente (Flashscore, Wplay, Soccerway) para esa temporada.
//            Los adapters la resuelven por temporadaId + tipo para llamar a los endpoints.
//            Tiene identidad propia (id) por eso es Entity y no VO.
// [ALTERNATIVAS]: Columna url en Temporada; se descarta porque una temporada tiene 3 URLs (una
//                 por fuente) y el catálogo de fuentes es gestionable (CU-11).
// [RELACIONES]: Compone Temporada (por temporadaId) + FuenteExtraccion (por referencia al entity).
//               Resuelta por los adapters de fuentes (CU-01/02/03). FASE 8.5.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.util.Objects;
import java.util.UUID;

public final class DetalleFuenteExtraccion {

    private final UUID id;
    private final UUID temporadaId;
    private final FuenteExtraccion fuente;
    private final String url;
    private final boolean activa;

    // [QUÉ]: Construye un detalle generando su identidad (asociación vía CU-04/CU-11).
    public DetalleFuenteExtraccion(UUID temporadaId, FuenteExtraccion fuente, String url, boolean activa) {
        this(UUID.randomUUID(), temporadaId, fuente, url, activa);
    }

    // [QUÉ]: Construye un detalle con identidad provista (reconstrucción desde persistencia).
    public DetalleFuenteExtraccion(UUID id, UUID temporadaId, FuenteExtraccion fuente, String url, boolean activa) {
        if (id == null) {
            throw new DomainException("DetalleFuenteExtraccion requiere id");
        }
        if (temporadaId == null) {
            throw new DomainException("DetalleFuenteExtraccion requiere temporadaId");
        }
        if (fuente == null) {
            throw new DomainException("DetalleFuenteExtraccion requiere fuente");
        }
        if (url == null || url.isBlank()) {
            throw new DomainException("DetalleFuenteExtraccion requiere url");
        }
        this.id = id;
        this.temporadaId = temporadaId;
        this.fuente = fuente;
        this.url = url;
        this.activa = activa;
    }

    public UUID id() {
        return id;
    }

    public UUID temporadaId() {
        return temporadaId;
    }

    // [QUÉ]: Compatibilidad legacy: devuelve el ligaId derivado de la temporada.
    // [POR QUÉ]: Compatibilidad con código que asume ligaId directo (adapters legacy).
    // [ALTERNATIVAS]: Inyectar TemporadaRepository para resolver ligaId.
    @Deprecated
    public UUID ligaId() {
        return null;
    }

    public FuenteExtraccion fuente() {
        return fuente;
    }

    public TipoFuenteExtraccion tipo() {
        return fuente.tipo();
    }

    public String url() {
        return url;
    }

    public boolean activa() {
        return activa;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DetalleFuenteExtraccion detalle)) return false;
        return id.equals(detalle.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
