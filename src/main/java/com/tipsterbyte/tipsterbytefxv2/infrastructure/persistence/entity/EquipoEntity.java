// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA del Equipo (tabla equipos), miembro de una TEMPORADA.
// [POR QUÉ]: Equipo tiene identidad propia en el dominio; se persiste como fila con
//            FK a su temporada (equipos.temporada_id → temporadas.id): la plantilla es
//            de una temporada concreta — un equipo que desciende no está en la siguiente,
//            y cada temporada conserva su historial (fase dedicada de temporadas).
//            No se expone JPA al dominio (los mappers convierten).
// [ALTERNATIVAS]: FK a ligas (modelo anterior); se descarta porque con múltiples
//                 temporadas por liga la pertenencia queda ambigua.
// [RELACIONES]: Mapea domain.model.Equipo (colección de Temporada). Referenciado por
//               PosicionTablaEntity. Propietario de la FK: TemporadaEntity.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "equipos")
public class EquipoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    // Escudo del equipo (URL de imagen) desde la fuente #6; nullable: los equipos
    // registrados por fuentes operativas (#3/#4) pueden no tenerlo.
    @Column(name = "logo_url", length = 300)
    private String logoUrl;

    @Column(name = "fundacion")
    private Integer fundacion;

    // Temporada a la que pertenece la plantilla del equipo.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "temporada_id", nullable = false)
    private TemporadaEntity temporada;

    protected EquipoEntity() {
    }

    public EquipoEntity(UUID id, String nombre) {
        this(id, nombre, null);
    }

    public EquipoEntity(UUID id, String nombre, String logoUrl) {
        this(id, nombre, logoUrl, null);
    }

    public EquipoEntity(UUID id, String nombre, String logoUrl, Integer fundacion) {
        this.id = id;
        this.nombre = nombre;
        this.logoUrl = logoUrl;
        this.fundacion = fundacion;
    }

    public Integer getFundacion() {
        return fundacion;
    }

    public void setTemporada(TemporadaEntity temporada) {
        this.temporada = temporada;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public TemporadaEntity getTemporada() {
        return temporada;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

}
