// ─────────────────────────────────────────────
// [QUÉ]: Aggregate root que representa una competición de fútbol, con sus temporadas
//        y estado. Los equipos y la tabla de posiciones son de la TEMPORADA (delegan).
// [POR QUÉ]: Es la frontera de consistencia del negocio de ligas. Protege sus reglas:
//            activación solo con fuentes operativas (BR-001), no extraer para ligas
//            inactivas (BR-002). La plantilla de equipos y la tabla de posiciones son
//            de una temporada concreta (un equipo que desciende en 2024 no está en la
//            tabla 2025), por eso Liga delega en su temporada vigente (activa o, en su
//            defecto, la primera registrada) y ya no las posee directamente.
// [ALTERNATIVAS]: Entidad anémica con getters/setters; se descarta porque dejaría
//                 las reglas de negocio fuera del dominio (service anémico).
//                 Conservar colecciones propias; se descarta porque con múltiples
//                 temporadas el dato queda ambiguo.
// [RELACIONES]: Aggregate de CU-01 (posiciones), CU-02 (calendario) y CU-04 (activar).
//               Compone Temporada (1:N); referencia Pais por identidad (pais_id).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.event.LigaActivada;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class Liga {

    private final UUID id;
    private final String nombre;
    private final String pais;
    // Referencia por identidad al país del catálogo (tabla paises). Nullable para
    // ligas creadas manualmente sin catálogo previo.
    private final UUID paisId;
    private final Set<Temporada> temporadas;
    private final String urlSoccerway;
    private final String apiId;
    private EstadoLiga estado;
    private final List<DomainEvent> eventos;

    public Liga(String nombre, String pais) {
        this(UUID.randomUUID(), nombre, pais, null, EstadoLiga.BORRADOR, null, null, new HashSet<>());
    }

    public Liga(String nombre, String pais, String urlSoccerway, String apiId) {
        this(UUID.randomUUID(), nombre, pais, null, EstadoLiga.BORRADOR, urlSoccerway, apiId, new HashSet<>());
    }

    // [QUÉ]: Alta de liga de catálogo (CU-10) con vínculo real al país del catálogo.
    public Liga(String nombre, String pais, UUID paisId, String urlSoccerway, String apiId) {
        this(UUID.randomUUID(), nombre, pais, paisId, EstadoLiga.BORRADOR, urlSoccerway, apiId, new HashSet<>());
    }

    public Liga(UUID id, String nombre, String pais, EstadoLiga estado) {
        this(id, nombre, pais, null, estado, null, null, new HashSet<>());
    }

    public static Liga reconstruir(UUID id, String nombre, String pais, UUID paisId, EstadoLiga estado,
                                   Set<Temporada> temporadas) {
        return new Liga(id, nombre, pais, paisId, estado, null, null, temporadas);
    }

    public static Liga reconstruir(UUID id, String nombre, String pais, UUID paisId, EstadoLiga estado,
                                   String urlSoccerway, String apiId, Set<Temporada> temporadas) {
        return new Liga(id, nombre, pais, paisId, estado, urlSoccerway, apiId, temporadas);
    }

    private Liga(UUID id, String nombre, String pais, UUID paisId, EstadoLiga estado,
                 String urlSoccerway, String apiId, Set<Temporada> temporadas) {
        if (id == null) {
            throw new DomainException("Liga requiere id");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("Liga requiere nombre");
        }
        if (pais == null || pais.isBlank()) {
            throw new DomainException("Liga requiere país");
        }
        this.id = id;
        this.nombre = nombre;
        this.pais = pais;
        this.paisId = paisId;
        this.temporadas = new HashSet<>(temporadas != null ? temporadas : new HashSet<>());
        this.urlSoccerway = urlSoccerway;
        this.apiId = apiId;
        this.estado = estado;
        this.eventos = new ArrayList<>();
    }

    public void activar(boolean posicionesDisponible, boolean calendarioDisponible, boolean cuotasDisponibles) {
        if (estado == EstadoLiga.ACTIVA) {
            return;
        }
        if (!(posicionesDisponible && calendarioDisponible && cuotasDisponibles)) {
            throw new DomainException("Liga no activable: fuentes de datos no operativas (BR-001)");
        }
        this.estado = EstadoLiga.ACTIVA;
        temporadaVigente().activar();
        this.eventos.add(new LigaActivada(this.id));
    }

    // [QUÉ]: Registra un equipo en la temporada vigente de la liga.
    // [POR QUÉ]: La plantilla es de una temporada concreta; el aggregate mantiene la
    //            frontera (el caso de uso sigue hablando "con la liga").
    public void agregarEquipo(Equipo equipo) {
        temporadaVigente().agregarEquipo(equipo);
    }

    // [QUÉ]: Reemplaza la tabla de posiciones de la temporada vigente.
    // [POR QUÉ]: BR-002 se exige a nivel liga (estado ACTIVA); el reemplazo en bloque
    //            aplica sobre la temporada vigente (activa o primera registrada).
    public void actualizarPosiciones(List<PosicionTabla> nuevasPosiciones) {
        if (estado != EstadoLiga.ACTIVA) {
            throw new DomainException("No se puede extraer posiciones de una liga inactiva (BR-002)");
        }
        temporadaVigente().actualizarPosiciones(nuevasPosiciones);
    }

    public void addTemporada(Temporada temporada) {
        if (temporada == null) {
            throw new DomainException("Temporada no puede ser nula");
        }
        if (!temporada.ligaId().equals(this.id)) {
            throw new DomainException("La temporada no pertenece a esta liga");
        }
        this.temporadas.add(temporada);
    }

    public void removeTemporada(Temporada temporada) {
        this.temporadas.remove(temporada);
    }

    public Set<Temporada> getTemporadas() {
        return Set.copyOf(temporadas);
    }

    public Optional<Temporada> getTemporadaActual() {
        return temporadas.stream()
                .filter(t -> t.estado() == EstadoTemporada.ACTIVA)
                .findFirst();
    }

    public Optional<Temporada> getTemporadaPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return Optional.empty();
        }
        return temporadas.stream()
                .filter(t -> nombre.equalsIgnoreCase(t.nombre()))
                .findFirst();
    }

    // [QUÉ]: Temporada sobre la que operan equipos/posiciones: la ACTIVA o, en su
    //        defecto, la primera registrada (catálogo recién poblado está PLANIFICADA).
    // [POR QUÉ]: Mientras exista una sola temporada por liga (realidad del catálogo),
    //            la delegación es transparente para los casos de uso.
    private Temporada temporadaVigente() {
        return getTemporadaActual()
                .or(() -> temporadas.stream().findFirst())
                .orElseThrow(() -> new DomainException(
                        "La liga no tiene temporadas registradas: " + id));
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

    public UUID paisId() {
        return paisId;
    }

    @Deprecated
    public Temporada temporada() {
        if (temporadas.size() != 1) {
            throw new IllegalStateException("La liga no tiene exactamente una temporada; use getTemporadaActual() o getTemporadas()");
        }
        return temporadas.iterator().next();
    }

    public String urlSoccerway() {
        return urlSoccerway;
    }

    public String apiId() {
        return apiId;
    }

    public EstadoLiga estado() {
        return estado;
    }

    // [QUÉ]: Vista de solo lectura de los equipos de la temporada vigente.
    public List<Equipo> equipos() {
        return List.copyOf(temporadaVigente().equipos());
    }

    // [QUÉ]: Vista de solo lectura de la tabla de posiciones de la temporada vigente.
    public List<PosicionTabla> posiciones() {
        return List.copyOf(temporadaVigente().posiciones());
    }

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
