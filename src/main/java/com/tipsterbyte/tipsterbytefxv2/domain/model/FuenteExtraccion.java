// ─────────────────────────────────────────────
// [QUÉ]: Entity que representa una fuente externa de extracción de datos deportivos
//        (posiciones, cuotas Wplay, calendario), es decir, el catálogo de fuentes.
// [POR QUÉ]: Es el catálogo de los 3 tipos de fuente que una liga puede asociar.
//            Tiene identidad propia (id) que permanece aunque cambien sus atributos,
//            por eso es Entity y no VO. Se inspira en la tabla fuente_extraccion del
//            proyecto Python, pero con nombres propios de Java (tabla fuentes_extraccion).
// [ALTERNATIVAS]: Enum con las 3 fuentes; se descartó porque el usuario pidió un
//                 catálogo gestionable (CU-11) que permita ampliar fuentes sin código.
// [RELACIONES]: Catálogo gestionado por CU-11. Referenciada por DetalleFuenteExtraccion
//               (una liga asocia una fuente con una URL). FASE 8.5.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.util.Objects;
import java.util.UUID;

public final class FuenteExtraccion {

    private final UUID id;
    private final String nombre;
    private final TipoFuenteExtraccion tipo;
    private final boolean activa;

    // [QUÉ]: Construye una fuente generando su identidad (alta vía CU-11).
    public FuenteExtraccion(String nombre, TipoFuenteExtraccion tipo, boolean activa) {
        this(UUID.randomUUID(), nombre, tipo, activa);
    }

    // [QUÉ]: Construye una fuente con identidad provista (reconstrucción desde persistencia).
    public FuenteExtraccion(UUID id, String nombre, TipoFuenteExtraccion tipo, boolean activa) {
        if (id == null) {
            throw new DomainException("FuenteExtraccion requiere id");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("FuenteExtraccion requiere nombre");
        }
        if (tipo == null) {
            throw new DomainException("FuenteExtraccion requiere tipo");
        }
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.activa = activa;
    }

    public UUID id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public TipoFuenteExtraccion tipo() {
        return tipo;
    }

    public boolean activa() {
        return activa;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FuenteExtraccion fuente)) return false;
        return id.equals(fuente.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
