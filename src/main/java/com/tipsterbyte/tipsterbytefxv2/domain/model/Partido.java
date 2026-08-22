// ─────────────────────────────────────────────
// [QUÉ]: Aggregate root que representa un partido de fútbol entre dos equipos,
//        con su fecha programada, jornada, estado, cuotas y resultado.
// [POR QUÉ]: Es la frontera de consistencia de un enfrentamiento: el resultado
//            solo se asigna al finalizar (BR-003) y las cuotas llegan solo desde
//            la fuente de odds (nunca se mutan localmente). La jornada se persiste
//            desde la fuente #4 (Soccerway, label "Jornada N") para el indicador
//            cronológico por liga del frontend.
// [ALTERNATIVAS]: Entidad con setters; se descarta porque permitiría asignar
//                 un resultado sin finalizar o cuotas inventadas. Jornada como VO;
//                 se descarta por simplicidad (Integer con validación >= 1).
// [RELACIONES]: Aggregate de CU-02 (calendario), CU-03 (cuotas) y CU-05 (resultado).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.CuotaActualizada;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.event.PartidoProgramado;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Partido {

    private final UUID id;
    private final UUID temporadaId;
    private final Equipo equipoLocal;
    private final Equipo equipoVisitante;
    private final FechaProgramada fechaProgramada;
    private final Integer jornada;
    private final List<Cuota> cuotas;
    private Resultado resultado;
    private EstadoPartido estado;
    private final List<DomainEvent> eventos;

    public Partido(UUID temporadaId, Equipo equipoLocal, Equipo equipoVisitante, FechaProgramada fechaProgramada) {
        this(temporadaId, equipoLocal, equipoVisitante, fechaProgramada, null);
    }

    public Partido(UUID temporadaId, Equipo equipoLocal, Equipo equipoVisitante, FechaProgramada fechaProgramada,
                   Integer jornada) {
        this(UUID.randomUUID(), temporadaId, equipoLocal, equipoVisitante, fechaProgramada, EstadoPartido.PROGRAMADO,
                List.of(), null, jornada);
        this.eventos.add(new PartidoProgramado(this.id));
    }

    public Partido(UUID id, UUID temporadaId, Equipo equipoLocal, Equipo equipoVisitante,
                   FechaProgramada fechaProgramada, EstadoPartido estado) {
        this(id, temporadaId, equipoLocal, equipoVisitante, fechaProgramada, estado, List.of(), null, null);
        if (estado == EstadoPartido.PROGRAMADO) {
            this.eventos.add(new PartidoProgramado(this.id));
        }
    }

    public static Partido reconstruir(UUID id, UUID temporadaId, Equipo equipoLocal, Equipo equipoVisitante,
                                      FechaProgramada fechaProgramada, EstadoPartido estado,
                                      List<Cuota> cuotas, Resultado resultado, Integer jornada) {
        return new Partido(id, temporadaId, equipoLocal, equipoVisitante, fechaProgramada, estado, cuotas, resultado, jornada);
    }

    private Partido(UUID id, UUID temporadaId, Equipo equipoLocal, Equipo equipoVisitante,
                    FechaProgramada fechaProgramada, EstadoPartido estado,
                    List<Cuota> cuotas, Resultado resultado, Integer jornada) {
        if (id == null) {
            throw new DomainException("Partido requiere id");
        }
        if (temporadaId == null) {
            throw new DomainException("Partido requiere temporadaId");
        }
        if (equipoLocal == null || equipoVisitante == null) {
            throw new DomainException("Partido requiere equipos local y visitante");
        }
        if (equipoLocal.equals(equipoVisitante)) {
            throw new DomainException("Un equipo no puede enfrentarse a sí mismo");
        }
        if (fechaProgramada == null) {
            throw new DomainException("Partido requiere fecha programada");
        }
        if (jornada != null && jornada < 1) {
            throw new DomainException("Jornada debe ser un número positivo");
        }
        this.id = id;
        this.temporadaId = temporadaId;
        this.equipoLocal = equipoLocal;
        this.equipoVisitante = equipoVisitante;
        this.fechaProgramada = fechaProgramada;
        this.jornada = jornada;
        this.cuotas = new ArrayList<>(cuotas != null ? cuotas : List.of());
        this.resultado = resultado;
        this.estado = estado;
        this.eventos = new ArrayList<>();
    }

    public void actualizarCuotas(List<Cuota> nuevasCuotas) {
        if (nuevasCuotas == null || nuevasCuotas.isEmpty()) {
            throw new DomainException("La sincronización de cuotas no puede estar vacía");
        }
        this.cuotas.clear();
        this.cuotas.addAll(nuevasCuotas);
        this.eventos.add(new CuotaActualizada(this.id));
    }

    public void iniciar() {
        if (estado != EstadoPartido.PROGRAMADO) {
            throw new DomainException("Solo un partido programado puede iniciar");
        }
        this.estado = EstadoPartido.EN_VIVO;
    }

    public void finalizar() {
        if (estado != EstadoPartido.EN_VIVO && estado != EstadoPartido.PROGRAMADO) {
            throw new DomainException("Partido no puede finalizar desde estado " + estado);
        }
        this.estado = EstadoPartido.FINALIZADO;
    }

    public void asignarResultado(Resultado resultado) {
        if (resultado == null) {
            throw new DomainException("Resultado no puede ser nulo");
        }
        if (estado != EstadoPartido.FINALIZADO) {
            throw new DomainException("Resultado solo se asigna cuando el partido está finalizado (BR-003)");
        }
        this.resultado = resultado;
    }

    public UUID id() {
        return id;
    }

    public UUID temporadaId() {
        return temporadaId;
    }

    @Deprecated
    public UUID ligaId() {
        return null;
    }

    public Equipo equipoLocal() {
        return equipoLocal;
    }

    public Equipo equipoVisitante() {
        return equipoVisitante;
    }

    public FechaProgramada fechaProgramada() {
        return fechaProgramada;
    }

    public Integer jornada() {
        return jornada;
    }

    public EstadoPartido estado() {
        return estado;
    }

    public Resultado resultado() {
        return resultado;
    }

    public List<Cuota> cuotas() {
        return Collections.unmodifiableList(cuotas);
    }

    public List<DomainEvent> pullEventos() {
        List<DomainEvent> copia = new ArrayList<>(eventos);
        eventos.clear();
        return copia;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Partido partido)) return false;
        return id.equals(partido.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
