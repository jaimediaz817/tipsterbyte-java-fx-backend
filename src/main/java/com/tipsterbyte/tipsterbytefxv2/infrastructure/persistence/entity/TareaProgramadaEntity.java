package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "tareas_programadas")
public class TareaProgramadaEntity {

    @Id
    private UUID id;

    @Column(name = "liga_id")
    private UUID ligaId;

    @Column(name = "tipo_fuente")
    @Enumerated(EnumType.STRING)
    private TipoFuenteExtraccion tipoFuente;

    @Column(name = "prioridad")
    private String prioridad;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "activa")
    private boolean activa;

    @Column(name = "created_at")
    private String createdAt;

    // Constructors
    public TareaProgramadaEntity() {
    }

    public TareaProgramadaEntity(UUID id, UUID ligaId, TipoFuenteExtraccion tipoFuente, String prioridad, String cronExpression, boolean activa, String createdAt) {
        this.id = id;
        this.ligaId = ligaId;
        this.tipoFuente = tipoFuente;
        this.prioridad = prioridad;
        this.cronExpression = cronExpression;
        this.activa = activa;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLigaId() {
        return ligaId;
    }

    public void setLigaId(UUID ligaId) {
        this.ligaId = ligaId;
    }

    public TipoFuenteExtraccion getTipoFuente() {
        return tipoFuente;
    }

    public void setTipoFuente(TipoFuenteExtraccion tipoFuente) {
        this.tipoFuente = tipoFuente;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}