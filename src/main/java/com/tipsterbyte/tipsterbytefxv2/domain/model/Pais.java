// ─────────────────────────────────────────────
// [QUÉ]: Entity que representa un país con ligas registradas en el catálogo
//        (fuente #1 ext-soccerway-countries).
// [POR QUÉ]: El catálogo de países es un concepto propio del negocio (CU-10):
//            la fuente #1 entrega 176 países con metadatos (código, ISO, continente)
//            que hoy no viven en ningún lado del modelo. Tiene identidad propia (id)
//            que permanece aunque sus atributos cambien, por eso es Entity y no VO.
// [ALTERNATIVAS]: VO dentro de Liga; se descarta porque el país es un agregado del
//                 catálogo (existiría aunque no haya ligas) y necesita repositorio.
// [RELACIONES]: Alimenta el catálogo de CU-10. Liga.pais (String) conserva el nombre
//               denormalizado; Pais es la tabla maestra de países.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.util.Objects;
import java.util.UUID;

public final class Pais {

    private final UUID id;
    private final String nombre;
    private final String isoAlpha2;
    private final String continente;
    private final String code;
    private final String href;
    private final boolean mapeado;

    // [QUÉ]: Construye un país generando su identidad (alta desde la fuente #1).
    public Pais(String nombre, String isoAlpha2, String continente, String code, String href, boolean mapeado) {
        this(UUID.randomUUID(), nombre, isoAlpha2, continente, code, href, mapeado);
    }

    // [QUÉ]: Construye un país con identidad provista (reconstrucción desde persistencia).
    public Pais(UUID id, String nombre, String isoAlpha2, String continente, String code, String href, boolean mapeado) {
        if (id == null) {
            throw new DomainException("Pais requiere id");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("Pais requiere nombre");
        }
        if (isoAlpha2 == null || isoAlpha2.isBlank()) {
            throw new DomainException("Pais requiere isoAlpha2");
        }
        this.id = id;
        this.nombre = nombre;
        this.isoAlpha2 = isoAlpha2;
        this.continente = continente;
        this.code = code;
        this.href = href;
        this.mapeado = mapeado;
    }

    public UUID id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public String isoAlpha2() {
        return isoAlpha2;
    }

    public String continente() {
        return continente;
    }

    public String code() {
        return code;
    }

    public String href() {
        return href;
    }

    public boolean mapeado() {
        return mapeado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pais pais)) return false;
        return id.equals(pais.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}