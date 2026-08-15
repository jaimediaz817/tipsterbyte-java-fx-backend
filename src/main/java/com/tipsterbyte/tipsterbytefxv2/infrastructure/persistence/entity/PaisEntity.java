// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA del catálogo de países (tabla paises).
// [POR QUÉ]: Es la representación persistente del entity Pais del dominio. Vive en
//            infraestructura para que el dominio no conozca JPA ni PostgreSQL. El
//            adapter/mapper convierte entre PaisEntity y el entity Pais.
// [ALTERNATIVAS]: Anotar el entity de dominio con @Entity; se descarta porque
//                 acoplaría el dominio a JPA, violando la Dependency Rule.
// [RELACIONES]: Mapea el entity Pais (CU-10). Convertida por PaisRepositoryJpaAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "paises")
public class PaisEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    // Clave natural de la fuente #1 (iso_alpha2) con unicidad para evitar duplicados.
    @Column(name = "iso_alpha2", nullable = false, unique = true, length = 2)
    private String isoAlpha2;

    @Column(name = "continente", nullable = false, length = 40)
    private String continente;

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "href", length = 200)
    private String href;

    @Column(name = "mapeado", nullable = false)
    private boolean mapeado;

    protected PaisEntity() {
    }

    public PaisEntity(UUID id, String nombre, String isoAlpha2, String continente,
                      String code, String href, boolean mapeado) {
        this.id = id;
        this.nombre = nombre;
        this.isoAlpha2 = isoAlpha2;
        this.continente = continente;
        this.code = code;
        this.href = href;
        this.mapeado = mapeado;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIsoAlpha2() {
        return isoAlpha2;
    }

    public String getContinente() {
        return continente;
    }

    public String getCode() {
        return code;
    }

    public String getHref() {
        return href;
    }

    public boolean isMapeado() {
        return mapeado;
    }
}