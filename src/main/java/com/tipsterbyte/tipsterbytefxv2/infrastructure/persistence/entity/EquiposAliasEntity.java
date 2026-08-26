package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EquiposAlias;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "equipos_alias")
public class EquiposAliasEntity {

    @Id
    private UUID id;

    @Column(name = "fuente_tipo", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoFuenteExtraccion fuenteTipo;

    @Column(name = "nombre_externo", nullable = false)
    private String nombreExterno;

    @Column(name = "equipo_id", nullable = false)
    private UUID equipoId;

    @Column(name = "temporada_id", nullable = false)
    private UUID temporadaId;

    public EquiposAliasEntity() {
    }

    public EquiposAliasEntity(UUID id, TipoFuenteExtraccion fuenteTipo, String nombreExterno,
                               UUID equipoId, UUID temporadaId) {
        this.id = id;
        this.fuenteTipo = fuenteTipo;
        this.nombreExterno = nombreExterno;
        this.equipoId = equipoId;
        this.temporadaId = temporadaId;
    }

    public EquiposAlias toDomainModel() {
        return new EquiposAlias(id, fuenteTipo, nombreExterno, equipoId, temporadaId);
    }

    public static EquiposAliasEntity fromDomainModel(EquiposAlias alias) {
        return new EquiposAliasEntity(alias.id(), alias.fuenteTipo(), alias.nombreExterno(),
                alias.equipoId(), alias.temporadaId());
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TipoFuenteExtraccion getFuenteTipo() { return fuenteTipo; }
    public void setFuenteTipo(TipoFuenteExtraccion fuenteTipo) { this.fuenteTipo = fuenteTipo; }
    public String getNombreExterno() { return nombreExterno; }
    public void setNombreExterno(String nombreExterno) { this.nombreExterno = nombreExterno; }
    public UUID getEquipoId() { return equipoId; }
    public void setEquipoId(UUID equipoId) { this.equipoId = equipoId; }
    public UUID getTemporadaId() { return temporadaId; }
    public void setTemporadaId(UUID temporadaId) { this.temporadaId = temporadaId; }
}
