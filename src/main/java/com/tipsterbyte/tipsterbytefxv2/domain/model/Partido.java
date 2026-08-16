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
    private final UUID ligaId;
    private final Equipo equipoLocal;
    private final Equipo equipoVisitante;
    private final FechaProgramada fechaProgramada;
    private final Integer jornada;
    private final List<Cuota> cuotas;
    private Resultado resultado;
    private EstadoPartido estado;
    private final List<DomainEvent> eventos;

    // [QUÉ]: Construye un partido programado sin cuotas, sin resultado y sin jornada
    //        conocida (legado/otras fuentes sin dato de jornada).
    public Partido(UUID ligaId, Equipo equipoLocal, Equipo equipoVisitante, FechaProgramada fechaProgramada) {
        this(ligaId, equipoLocal, equipoVisitante, fechaProgramada, null);
    }

    // [QUÉ]: Construye un partido programado con su jornada (CU-02, fuente #4).
    // [POR QUÉ]: La fuente #4 entrega "Jornada N" por partido; se persiste para que el
    //            frontend muestre el indicador cronológico por liga.
    public Partido(UUID ligaId, Equipo equipoLocal, Equipo equipoVisitante, FechaProgramada fechaProgramada,
                   Integer jornada) {
        this(UUID.randomUUID(), ligaId, equipoLocal, equipoVisitante, fechaProgramada, EstadoPartido.PROGRAMADO,
                List.of(), null, jornada);
        this.eventos.add(new PartidoProgramado(this.id));
    }

    // [QUÉ]: Construye un partido con identidad y estado provistos (reconstrucción desde persistencia).
    public Partido(UUID id, UUID ligaId, Equipo equipoLocal, Equipo equipoVisitante,
                   FechaProgramada fechaProgramada, EstadoPartido estado) {
        this(id, ligaId, equipoLocal, equipoVisitante, fechaProgramada, estado, List.of(), null, null);
        if (estado == EstadoPartido.PROGRAMADO) {
            this.eventos.add(new PartidoProgramado(this.id));
        }
    }

    // [QUÉ]: Factory de reconstrucción completa del aggregate desde persistencia (FASE 8).
    // [POR QUÉ]: Al cargar un partido de la BD se deben restaurar también sus cuotas,
    //            resultado y jornada. No se usan actualizarCuotas/asignarResultado porque
    //            son transiciones de negocio (asignarResultado exige FINALIZADO, BR-003)
    //            y actualizarCuotas emite evento. Reconstruir no debe emitir eventos
    //            (no es una nueva programación) ni aplicar reglas de transición.
    // [ALTERNATIVAS]: Hidratar vía setters de negocio; se descarta porque re-emitiría
    //                 PartidoProgramado/CuotaActualizada al leer de BD.
    // [RELACIONES]: Usado por PartidoRepositoryJpaAdapter (FASE 8).
    public static Partido reconstruir(UUID id, UUID ligaId, Equipo equipoLocal, Equipo equipoVisitante,
                                      FechaProgramada fechaProgramada, EstadoPartido estado,
                                      List<Cuota> cuotas, Resultado resultado, Integer jornada) {
        return new Partido(id, ligaId, equipoLocal, equipoVisitante, fechaProgramada, estado, cuotas, resultado, jornada);
    }

    private Partido(UUID id, UUID ligaId, Equipo equipoLocal, Equipo equipoVisitante,
                    FechaProgramada fechaProgramada, EstadoPartido estado,
                    List<Cuota> cuotas, Resultado resultado, Integer jornada) {
        if (id == null) {
            throw new DomainException("Partido requiere id");
        }
        if (ligaId == null) {
            throw new DomainException("Partido requiere liga");
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
        this.ligaId = ligaId;
        this.equipoLocal = equipoLocal;
        this.equipoVisitante = equipoVisitante;
        this.fechaProgramada = fechaProgramada;
        this.jornada = jornada;
        this.cuotas = new ArrayList<>(cuotas != null ? cuotas : List.of());
        this.resultado = resultado;
        this.estado = estado;
        this.eventos = new ArrayList<>();
    }

    // [QUÉ]: Reemplaza las cuotas con las recibidas de la fuente de odds (CU-03).
    // [POR QUÉ]: Las cuotas nunca se inventan localmente; llegan del ProveedorCuotas.
    //            Solo se aceptan cuotas válidas (BR-007 valida valor > 1.0 en el VO).
    public void actualizarCuotas(List<Cuota> nuevasCuotas) {
        if (nuevasCuotas == null || nuevasCuotas.isEmpty()) {
            throw new DomainException("La sincronización de cuotas no puede estar vacía");
        }
        this.cuotas.clear();
        this.cuotas.addAll(nuevasCuotas);
        this.eventos.add(new CuotaActualizada(this.id));
    }

    // [QUÉ]: Marca el partido como EN_VIVO cuando llega su hora.
    public void iniciar() {
        if (estado != EstadoPartido.PROGRAMADO) {
            throw new DomainException("Solo un partido programado puede iniciar");
        }
        this.estado = EstadoPartido.EN_VIVO;
    }

    // [QUÉ]: Finaliza el partido (paso previo a asignar el resultado, BR-003).
    public void finalizar() {
        if (estado != EstadoPartido.EN_VIVO && estado != EstadoPartido.PROGRAMADO) {
            throw new DomainException("Partido no puede finalizar desde estado " + estado);
        }
        this.estado = EstadoPartido.FINALIZADO;
    }

    // [QUÉ]: Asigna el resultado final del partido (BR-003).
    // [POR QUÉ]: El resultado solo es válido una vez que el partido finalizó.
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

    public UUID ligaId() {
        return ligaId;
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