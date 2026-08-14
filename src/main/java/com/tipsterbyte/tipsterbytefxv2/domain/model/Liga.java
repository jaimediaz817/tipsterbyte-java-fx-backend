// ─────────────────────────────────────────────
// [QUÉ]: Aggregate root que representa una competición de fútbol, con sus
//        equipos, temporada, estado y tabla de posiciones.
// [POR QUÉ]: Es la frontera de consistencia del negocio de ligas. Protege sus
//            reglas: activación solo con fuentes operativas (BR-001), no extraer
//            para ligas inactivas (BR-002) y consistencia de posiciones (BR-008).
// [ALTERNATIVAS]: Entidad anémica con getters/setters; se descarta porque dejaría
//                 las reglas de negocio fuera del dominio (service anémico).
// [RELACIONES]: Aggregate de CU-01 (posiciones), CU-02 (calendario) y CU-04 (activar).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.event.LigaActivada;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Liga {

    private final UUID id;
    private final String nombre;
    private final String pais;
    private final Temporada temporada;
    private final List<Equipo> equipos;
    private final List<PosicionTabla> posiciones;
    private EstadoLiga estado;
    private final List<DomainEvent> eventos;

    // [QUÉ]: Construye una liga en estado BORRADOR, sin equipos ni posiciones.
    public Liga(String nombre, String pais, Temporada temporada) {
        this(UUID.randomUUID(), nombre, pais, temporada, EstadoLiga.BORRADOR);
    }

    // [QUÉ]: Construye una liga con identidad y estado provistos (reconstrucción desde persistencia).
    public Liga(UUID id, String nombre, String pais, Temporada temporada, EstadoLiga estado) {
        this(id, nombre, pais, temporada, estado, List.of(), List.of());
    }

    // [QUÉ]: Factory de reconstrucción completa del aggregate desde persistencia (FASE 8).
    // [POR QUÉ]: Al cargar una liga de la BD se deben restaurar también sus equipos y
    //            posiciones. No se usan agregarEquipo/actualizarPosiciones porque son
    //            transiciones de negocio (actualizarPosiciones exige ACTIVA, BR-002);
    //            reconstruir no debe emitir eventos ni aplicar reglas de transición,
    //            solo invariantes estructurales. Patrón DDD: reconstrucción ≠ transición.
    // [ALTERNATIVAS]: Setear listas vía métodos de negocio; se descarta porque
    //                 re-emitiría reglas/validaciones incorrectas al hidratar desde BD.
    // [RELACIONES]: Usado por LigaRepositoryJpaAdapter (FASE 8).
    public static Liga reconstruir(UUID id, String nombre, String pais, Temporada temporada,
                                   EstadoLiga estado, List<Equipo> equipos, List<PosicionTabla> posiciones) {
        return new Liga(id, nombre, pais, temporada, estado, equipos, posiciones);
    }

    private Liga(UUID id, String nombre, String pais, Temporada temporada, EstadoLiga estado,
                 List<Equipo> equipos, List<PosicionTabla> posiciones) {
        if (id == null) {
            throw new DomainException("Liga requiere id");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("Liga requiere nombre");
        }
        if (pais == null || pais.isBlank()) {
            throw new DomainException("Liga requiere país");
        }
        if (temporada == null) {
            throw new DomainException("Liga requiere temporada");
        }
        this.id = id;
        this.nombre = nombre;
        this.pais = pais;
        this.temporada = temporada;
        this.equipos = new ArrayList<>(equipos != null ? equipos : List.of());
        this.posiciones = new ArrayList<>(posiciones != null ? posiciones : List.of());
        this.estado = estado;
        this.eventos = new ArrayList<>();
    }

    // [QUÉ]: Activa la liga si las fuentes de datos están operativas (BR-001).
    // [POR QUÉ]: Una liga activa inicia el proceso de extracción; si las fuentes
    //            no están listas, los datos llegarían incompletos.
    public void activar(boolean posicionesDisponible, boolean calendarioDisponible, boolean cuotasDisponibles) {
        if (estado == EstadoLiga.ACTIVA) {
            return;
        }
        if (!(posicionesDisponible && calendarioDisponible && cuotasDisponibles)) {
            throw new DomainException("Liga no activable: fuentes de datos no operativas (BR-001)");
        }
        this.estado = EstadoLiga.ACTIVA;
        this.eventos.add(new LigaActivada(this.id));
    }

    // [QUÉ]: Agrega un equipo a la liga.
    public void agregarEquipo(Equipo equipo) {
        if (equipo == null) {
            throw new DomainException("Equipo no puede ser nulo");
        }
        if (equipos.contains(equipo)) {
            throw new DomainException("Equipo ya está registrado en la liga");
        }
        this.equipos.add(equipo);
    }

    // [QUÉ]: Reemplaza la tabla de posiciones con los datos sincronizados (CU-01).
    // [POR QUÉ]: La tabla se recalcula desde la fuente; no se muta fila a fila.
    //            Exige liga ACTIVA: no se extrae para ligas inactivas (BR-002).
    public void actualizarPosiciones(List<PosicionTabla> nuevasPosiciones) {
        if (estado != EstadoLiga.ACTIVA) {
            throw new DomainException("No se puede extraer posiciones de una liga inactiva (BR-002)");
        }
        if (nuevasPosiciones == null || nuevasPosiciones.isEmpty()) {
            throw new DomainException("La sincronización de posiciones no puede estar vacía");
        }
        this.posiciones.clear();
        this.posiciones.addAll(nuevasPosiciones);
    }

    public UUID id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public String pais() {
        return pais;
    }

    public Temporada temporada() {
        return temporada;
    }

    public EstadoLiga estado() {
        return estado;
    }

    // [QUÉ]: Devuelve copia inmutable de los equipos.
    public List<Equipo> equipos() {
        return Collections.unmodifiableList(equipos);
    }

    // [QUÉ]: Devuelve copia inmutable de las posiciones.
    public List<PosicionTabla> posiciones() {
        return Collections.unmodifiableList(posiciones);
    }

    // [QUÉ]: Entrega y limpia los eventos de dominio recolectados.
    // [POR QUÉ]: El caso de uso decide cuándo publicarlos (FASE 13 RabbitMQ).
    public List<DomainEvent> pullEventos() {
        List<DomainEvent> copia = new ArrayList<>(eventos);
        eventos.clear();
        return copia;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Liga liga)) return false;
        return id.equals(liga.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}