// ─────────────────────────────────────────────
// [QUÉ]: Aggregate root que representa la opinión de un tipster sobre un partido:
//        selección (mercado + resultado esperado) y cuota de referencia.
// [POR QUÉ]: Es la frontera de consistencia del negocio de pronósticos: se publica
//            solo sobre partidos jugables con cuota vigente (BR-004) y, una vez
//            publicado, no se edita (BR-005).
// [ALTERNATIVAS]: Service que valida fuera del modelo; se descarta porque las reglas
//                 de publicación son invariantes del propio pronóstico.
// [RELACIONES]: Aggregate de CU-06 (crear) y CU-07 (publicar). Referencia a partido
//               y tipster por id (decisión: referencias por id entre aggregates).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.event.PronosticoPublicado;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Pronostico {

    private final UUID id;
    private final UUID tipsterId;
    private final UUID partidoId;
    private final SeleccionPronostico seleccion;
    private final Cuota cuota;
    private EstadoPronostico estado;
    private Resultado resultadoFinal;
    private final List<DomainEvent> eventos;

    // [QUÉ]: Construye un pronóstico en BORRADOR (no visible para clientes).
    public Pronostico(UUID tipsterId, UUID partidoId, SeleccionPronostico seleccion, Cuota cuota) {
        this(UUID.randomUUID(), tipsterId, partidoId, seleccion, cuota, EstadoPronostico.BORRADOR);
    }

    // [QUÉ]: Construye un pronóstico con identidad y estado provistos (reconstrucción).
    public Pronostico(UUID id, UUID tipsterId, UUID partidoId, SeleccionPronostico seleccion,
                      Cuota cuota, EstadoPronostico estado) {
        this(id, tipsterId, partidoId, seleccion, cuota, estado, null);
    }

    // [QUÉ]: Factory de reconstrucción completa desde persistencia (FASE 8).
    // [POR QUÉ]: Al cargar un pronóstico de la BD se debe restaurar también su
    //            resultado final (asignado por CU-05). No se usa registrarResultadoFinal
    //            por claridad de intención (reconstrucción ≠ transición) y para no
    //            depender del estado del agregado al hidratar.
    // [ALTERNATIVAS]: Setter de negocio; se descarta porque reconstruir no debe
    //                 pasar por reglas de transición.
    // [RELACIONES]: Usado por PronosticoRepositoryJpaAdapter (FASE 8).
    public static Pronostico reconstruir(UUID id, UUID tipsterId, UUID partidoId,
                                         SeleccionPronostico seleccion, Cuota cuota,
                                         EstadoPronostico estado, Resultado resultadoFinal) {
        return new Pronostico(id, tipsterId, partidoId, seleccion, cuota, estado, resultadoFinal);
    }

    private Pronostico(UUID id, UUID tipsterId, UUID partidoId, SeleccionPronostico seleccion,
                       Cuota cuota, EstadoPronostico estado, Resultado resultadoFinal) {
        if (id == null) {
            throw new DomainException("Pronóstico requiere id");
        }
        if (tipsterId == null) {
            throw new DomainException("Pronóstico requiere tipster");
        }
        if (partidoId == null) {
            throw new DomainException("Pronóstico requiere partido");
        }
        if (seleccion == null) {
            throw new DomainException("Pronóstico requiere selección");
        }
        if (cuota == null) {
            throw new DomainException("Pronóstico requiere cuota");
        }
        this.id = id;
        this.tipsterId = tipsterId;
        this.partidoId = partidoId;
        this.seleccion = seleccion;
        this.cuota = cuota;
        this.estado = estado;
        this.resultadoFinal = resultadoFinal;
        this.eventos = new ArrayList<>();
    }

    // [QUÉ]: Publica el pronóstico si el partido es jugable y la cuota vigente (BR-004).
    // [POR QUÉ]: Un pronóstico sobre un partido no programado o una cuota no vigente
    //            no aporta valor y podría inducir a error.
    public void publicar(boolean partidoJugable, boolean cuotaVigente) {
        if (estado != EstadoPronostico.BORRADOR) {
            throw new DomainException("Solo un borrador puede publicarse");
        }
        if (!partidoJugable) {
            throw new DomainException("No se puede publicar sobre un partido no jugable (BR-004)");
        }
        if (!cuotaVigente) {
            throw new DomainException("No se puede publicar con cuota no vigente (BR-004)");
        }
        this.estado = EstadoPronostico.PUBLICADO;
        this.eventos.add(new PronosticoPublicado(this.id));
    }

    // [QUÉ]: Anula un pronóstico ya publicado (BR-005: no se edita, se anula).
    public void anular() {
        if (estado != EstadoPronostico.PUBLICADO) {
            throw new DomainException("Solo un pronóstico publicado puede anularse (BR-005)");
        }
        this.estado = EstadoPronostico.ANULADO;
    }

    // [QUÉ]: Registra el resultado final para verificar el pronóstico (CU-05).
    public void registrarResultadoFinal(Resultado resultadoFinal) {
        if (resultadoFinal == null) {
            throw new DomainException("Resultado final no puede ser nulo");
        }
        this.resultadoFinal = resultadoFinal;
    }

    public UUID id() {
        return id;
    }

    public UUID tipsterId() {
        return tipsterId;
    }

    public UUID partidoId() {
        return partidoId;
    }

    public SeleccionPronostico seleccion() {
        return seleccion;
    }

    public Cuota cuota() {
        return cuota;
    }

    public EstadoPronostico estado() {
        return estado;
    }

    public Resultado resultadoFinal() {
        return resultadoFinal;
    }

    public List<DomainEvent> pullEventos() {
        List<DomainEvent> copia = new ArrayList<>(eventos);
        eventos.clear();
        return copia;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pronostico pronostico)) return false;
        return id.equals(pronostico.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}