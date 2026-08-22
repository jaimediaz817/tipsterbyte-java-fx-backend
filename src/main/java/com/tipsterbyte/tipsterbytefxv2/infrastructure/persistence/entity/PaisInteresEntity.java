// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA de la lista de países de interés (tabla paises_interes).
// [POR QUÉ]: Es la representación persistente del entity PaisInteres del dominio
//            (CU-14). Vive en infraestructura para que el dominio no conozca JPA ni
//            PostgreSQL. La prioridad de poblamiento que consume CU-10 se lee de aquí.
// [ALTERNATIVAS]: Anotar el entity de dominio con @Entity; se descarta porque
//                 acoplaría el dominio a JPA, violando la Dependency Rule.
// [RELACIONES]: Mapea el entity PaisInteres (CU-14). Convertida por
//               PaisInteresRepositoryJpaAdapter; consumida por CU-10.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "paises_interes")
public class PaisInteresEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Clave natural de la fuente #1 con unicidad (una preferencia por país).
    @Column(name = "iso_alpha2", nullable = false, unique = true, length = 2)
    private String isoAlpha2;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "prioridad", nullable = false)
    private int prioridad;

    // Límite opcional de ligas a sincronizar por país (null = sin límite). CU-10 lo consume.
    @Column(name = "max_ligas_por_pais")
    private Integer maxLigasPorPais;

    protected PaisInteresEntity() {
    }

    public PaisInteresEntity(UUID id, String isoAlpha2, String nombre, int prioridad, Integer maxLigasPorPais) {
        this.id = id;
        this.isoAlpha2 = isoAlpha2;
        this.nombre = nombre;
        this.prioridad = prioridad;
        this.maxLigasPorPais = maxLigasPorPais;
    }

    public UUID getId() {
        return id;
    }

    public String getIsoAlpha2() {
        return isoAlpha2;
    }

    public String getNombre() {
        return nombre;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public Integer getMaxLigasPorPais() {
        return maxLigasPorPais;
    }
}