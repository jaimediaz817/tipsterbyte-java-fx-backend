// ─────────────────────────────────────────────
// [QUÉ]: Aggregate root que representa la relación paga entre un cliente y un
//        tipster, con su plan, fechas y estado.
// [POR QUÉ]: Es la frontera de consistencia de la suscripción: el acceso a
//            pronósticos (BR-006) depende de que la suscripción esté activa y
//            dentro del periodo de vigencia. Expira al llegar fechaFin.
// [ALTERNATIVAS]: Estado calculado en cada consulta; se descarta porque el ciclo
//                 de vida de la suscripción tiene transiciones explícitas que el
//                 negocio necesita registrar (cancelación, expiración).
// [RELACIONES]: Aggregate de CU-09 (suscribirse). Habilita BR-006 (CU-08).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.event.SuscripcionCreada;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Suscripcion {

    private final UUID id;
    private final UUID clienteId;
    private final UUID tipsterId;
    private final Plan plan;
    private final LocalDateTime fechaInicio;
    private final LocalDateTime fechaFin;
    private EstadoSuscripcion estado;
    private final List<DomainEvent> eventos;

    // [QUÉ]: Construye una suscripción ACTIVA entre cliente y tipster (CU-09).
    public Suscripcion(UUID clienteId, UUID tipsterId, Plan plan, LocalDateTime fechaInicio) {
        this(UUID.randomUUID(), clienteId, tipsterId, plan, fechaInicio,
                fechaInicio.plusDays(plan.duracionDias()), EstadoSuscripcion.ACTIVA);
    }

    // [QUÉ]: Construye una suscripción con identidad y estado provistos (reconstrucción).
    public Suscripcion(UUID id, UUID clienteId, UUID tipsterId, Plan plan,
                       LocalDateTime fechaInicio, LocalDateTime fechaFin, EstadoSuscripcion estado) {
        if (id == null) {
            throw new DomainException("Suscripción requiere id");
        }
        if (clienteId == null) {
            throw new DomainException("Suscripción requiere cliente");
        }
        if (tipsterId == null) {
            throw new DomainException("Suscripción requiere tipster");
        }
        if (plan == null) {
            throw new DomainException("Suscripción requiere plan");
        }
        if (fechaInicio == null || fechaFin == null) {
            throw new DomainException("Suscripción requiere fechas de inicio y fin");
        }
        if (fechaFin.isBefore(fechaInicio)) {
            throw new DomainException("Fecha de fin no puede ser anterior a la de inicio");
        }
        this.id = id;
        this.clienteId = clienteId;
        this.tipsterId = tipsterId;
        this.plan = plan;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
        this.eventos = new ArrayList<>();
        if (estado == EstadoSuscripcion.ACTIVA) {
            this.eventos.add(new SuscripcionCreada(this.id));
        }
    }

    // [QUÉ]: Indica si la suscripción da acceso hoy (BR-006).
    // [POR QUÉ]: Un cliente solo consume pronósticos de tipsters con suscripción
    //            activa y dentro de su periodo de vigencia.
    public boolean estaActiva(LocalDateTime momento) {
        if (estado != EstadoSuscripcion.ACTIVA) {
            return false;
        }
        return momento != null && !momento.isAfter(fechaFin);
    }

    // [QUÉ]: Cancela una suscripción activa (transición explícita del negocio).
    public void cancelar() {
        if (estado != EstadoSuscripcion.ACTIVA) {
            throw new DomainException("Solo una suscripción activa puede cancelarse");
        }
        this.estado = EstadoSuscripcion.CANCELADA;
    }

    // [QUÉ]: Expira la suscripción cuando llega la fecha de fin.
    public void expirar(LocalDateTime momento) {
        if (estado == EstadoSuscripcion.ACTIVA && momento != null && momento.isAfter(fechaFin)) {
            this.estado = EstadoSuscripcion.EXPIRADA;
        }
    }

    public UUID id() {
        return id;
    }

    public UUID clienteId() {
        return clienteId;
    }

    public UUID tipsterId() {
        return tipsterId;
    }

    public Plan plan() {
        return plan;
    }

    public LocalDateTime fechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime fechaFin() {
        return fechaFin;
    }

    public EstadoSuscripcion estado() {
        return estado;
    }

    public List<DomainEvent> pullEventos() {
        List<DomainEvent> copia = new ArrayList<>(eventos);
        eventos.clear();
        return copia;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Suscripcion suscripcion)) return false;
        return id.equals(suscripcion.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}