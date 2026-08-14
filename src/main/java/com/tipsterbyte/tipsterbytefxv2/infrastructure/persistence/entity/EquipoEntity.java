// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA del Equipo (tabla equipos), miembro del aggregate Liga.
// [POR QUÉ]: Equipo tiene identidad propia en el dominio; se persiste como fila con
//            FK a la liga. No se expone JPA al dominio (los mappers convierten).
// [ALTERNATIVAS]: Embeddable dentro de LigaEntity; se descarta porque Equipo es una
//                 Entity con identidad y es referenciada por Partido y PosicionTabla.
// [RELACIONES]: Mapea domain.model.Equipo. Compuesta por LigaEntity y referenciada
//               por PartidoEntity y PosicionTablaEntity.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liga_id", nullable = false)
    private LigaEntity liga;

    protected EquipoEntity() {
    }

    public EquipoEntity(UUID id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public void setLiga(LigaEntity liga) {
        this.liga = liga;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public LigaEntity getLiga() {
        return liga;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}