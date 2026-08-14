// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA del aggregate Suscripcion (tabla suscripciones).
// [POR QUÉ]: Representación persistente del aggregate Suscripcion. El plan (nombre,
//            precio, duración) se guarda como columnas planas porque es un VO sin
//            colecciones. Cliente y tipster se referencian por id.
// [ALTERNATIVAS]: Tabla separada para el plan; se descarta porque no aporta flexibilidad
//                 y añade un join innecesario para un VO embebido.
// [RELACIONES]: Mapea domain.model.Suscripcion (CU-08, CU-09). Convertida por
//               SuscripcionRepositoryJpaAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoSuscripcion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "suscripciones")
public class SuscripcionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Referencias por id entre agregados (Suscripcion → Cliente/Tipster).
    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "tipster_id", nullable = false)
    private UUID tipsterId;

    @Column(name = "plan_nombre", nullable = false, length = 60)
    private String planNombre;

    @Column(name = "plan_precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal planPrecio;

    @Column(name = "plan_duracion_dias", nullable = false)
    private int planDuracionDias;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSuscripcion estado;

    protected SuscripcionEntity() {
    }

    public SuscripcionEntity(UUID id, UUID clienteId, UUID tipsterId, String planNombre,
                             BigDecimal planPrecio, int planDuracionDias,
                             LocalDateTime fechaInicio, LocalDateTime fechaFin,
                             EstadoSuscripcion estado) {
        this.id = id;
        this.clienteId = clienteId;
        this.tipsterId = tipsterId;
        this.planNombre = planNombre;
        this.planPrecio = planPrecio;
        this.planDuracionDias = planDuracionDias;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public UUID getTipsterId() {
        return tipsterId;
    }

    public String getPlanNombre() {
        return planNombre;
    }

    public BigDecimal getPlanPrecio() {
        return planPrecio;
    }

    public int getPlanDuracionDias() {
        return planDuracionDias;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public EstadoSuscripcion getEstado() {
        return estado;
    }

    public void setEstado(EstadoSuscripcion estado) {
        this.estado = estado;
    }
}