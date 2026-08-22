// ─────────────────────────────────────────────
// [QUÉ]: Entity que representa una temporada deportiva (torneo) dentro de una liga,
//        con su plantilla de equipos y su tabla de posiciones.
// [POR QUÉ]: Una liga puede tener múltiples temporadas (ej: Apertura/Clausura,
//            2024/2025). Los equipos y la tabla de posiciones SON de una temporada
//            concreta: un equipo que desciende en la temporada 2024 no aparece en la
//            de 2025, y cada temporada conserva su historial completo. Por eso las
//            colecciones viven aquí y ya no en Liga (fase dedicada de temporadas).
//            Tiene identidad propia (UUID) y referencia a su liga padre.
// [ALTERNATIVAS]: Mantener equipos/posiciones en Liga; se descarta porque con múltiples
//                 temporadas el dato queda ambiguo (¿de qué temporada es la tabla?).
// [RELACIONES]: Aggregate de Liga (1:N). Compone Equipo y PosicionTabla. Referida por
//               Partido, DetalleFuenteExtraccion, EquipoEntity y PosicionTablaEntity.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Temporada {

    private final UUID id;
    private final UUID ligaId;
    private final String nombre;
    private final Integer semestre;
    private final int anioInicio;
    private final int anioFin;
    private EstadoTemporada estado;
    private final List<Equipo> equipos;
    private final List<PosicionTabla> posiciones;

    // [QUÉ]: Construye una temporada generando su identidad (alta desde catálogo/CU-10).
    // [POR QUÉ]: Para alta de nueva temporada (CU-10 catálogo + CU-04 activación).
    public Temporada(UUID ligaId, String nombre, Integer semestre, int anioInicio, int anioFin, EstadoTemporada estado) {
        this(UUID.randomUUID(), ligaId, nombre, semestre, anioInicio, anioFin, estado);
    }

    // [QUÉ]: Construye una temporada con identidad provista (sin datos deportivos).
    public Temporada(UUID id, UUID ligaId, String nombre, Integer semestre, int anioInicio, int anioFin, EstadoTemporada estado) {
        this(id, ligaId, nombre, semestre, anioInicio, anioFin, estado, List.of(), List.of());
    }

    // [QUÉ]: Reconstrucción completa desde persistencia (con equipos y posiciones).
    public Temporada(UUID id, UUID ligaId, String nombre, Integer semestre,
                     int anioInicio, int anioFin, EstadoTemporada estado,
                     List<Equipo> equipos, List<PosicionTabla> posiciones) {
        if (id == null) {
            throw new DomainException("Temporada requiere id");
        }
        if (ligaId == null) {
            throw new DomainException("Temporada requiere ligaId");
        }
        if (anioInicio <= 0 || anioFin <= 0) {
            throw new DomainException("Temporada requiere años válidos");
        }
        if (anioFin <= anioInicio) {
            throw new DomainException("Temporada inválida: año de fin debe ser mayor al de inicio");
        }
        if (semestre != null && (semestre < 1 || semestre > 2)) {
            throw new DomainException("Semestre debe ser 1 o 2");
        }
        this.id = id;
        this.ligaId = ligaId;
        this.nombre = nombre != null ? nombre.trim() : null;
        this.semestre = semestre;
        this.anioInicio = anioInicio;
        this.anioFin = anioFin;
        this.estado = estado != null ? estado : EstadoTemporada.PLANIFICADA;
        this.equipos = new ArrayList<>(equipos != null ? equipos : List.of());
        this.posiciones = new ArrayList<>(posiciones != null ? posiciones : List.of());
    }

    // [QUÉ]: Registra un equipo en ESTA temporada (plantilla de la temporada).
    // [POR QUÉ]: Un equipo participa de una temporada concreta: si desciende, no estará
    //            en la siguiente. El duplicado se rechaza por identidad (BR consistencia).
    public void agregarEquipo(Equipo equipo) {
        if (equipo == null) {
            throw new DomainException("Equipo no puede ser nulo");
        }
        if (equipos.contains(equipo)) {
            throw new DomainException("Equipo ya está registrado en la temporada");
        }
        this.equipos.add(equipo);
    }

    // [QUÉ]: Reemplaza la tabla de posiciones de ESTA temporada (sincronización CU-01).
    // [POR QUÉ]: La fuente #3 entrega la tabla completa de la temporada vigente; el
    //            reemplazo en bloque garantiza consistencia entre filas.
    public void actualizarPosiciones(List<PosicionTabla> nuevasPosiciones) {
        if (nuevasPosiciones == null || nuevasPosiciones.isEmpty()) {
            throw new DomainException("La sincronización de posiciones no puede estar vacía");
        }
        this.posiciones.clear();
        this.posiciones.addAll(nuevasPosiciones);
    }

    // [QUÉ]: Transita PLANIFICADA → ACTIVA (idempotente si ya está ACTIVA).
    // [POR QUÉ]: La temporada en ejecución es ACTIVA; se activa al activar su liga
    //            (CU-04 con BR-001). Centraliza la regla: solo PLANIFICADA puede activarse.
    public void activar() {
        if (estado == EstadoTemporada.ACTIVA) {
            return;
        }
        if (estado != EstadoTemporada.PLANIFICADA) {
            throw new DomainException("Solo una temporada PLANIFICADA puede activarse (estado actual: " + estado + ")");
        }
        this.estado = EstadoTemporada.ACTIVA;
    }

    // [QUÉ]: Transita ACTIVA → FINALIZADA.
    public void finalizar() {
        if (estado != EstadoTemporada.ACTIVA) {
            throw new DomainException("Solo una temporada ACTIVA puede finalizarse (estado actual: " + estado + ")");
        }
        this.estado = EstadoTemporada.FINALIZADA;
    }

    // [QUÉ]: Actualiza el escudo (logo_url) de un equipo de ESTA temporada por su id.
    // [POR QUÉ]: Equipo es inmutable: la actualización crea una instancia nueva con el
    //            MISMO id, de modo que las referencias desde posiciones/partidos
    //            permanecen válidas. Silencioso si el id no está en la plantilla.
    public void actualizarEscudo(UUID equipoId, String logoUrl) {
        for (int i = 0; i < equipos.size(); i++) {
            Equipo actual = equipos.get(i);
            if (actual.id().equals(equipoId)) {
                equipos.set(i, new Equipo(actual.id(), actual.nombre(), logoUrl));
                return;
            }
        }
    }

    public UUID id() {
        return id;
    }

    public UUID ligaId() {
        return ligaId;
    }

    public String nombre() {
        return nombre;
    }

    public Integer semestre() {
        return semestre;
    }

    public int anioInicio() {
        return anioInicio;
    }

    public int anioFin() {
        return anioFin;
    }

    public EstadoTemporada estado() {
        return estado;
    }

    public List<Equipo> equipos() {
        return Collections.unmodifiableList(equipos);
    }

    public List<PosicionTabla> posiciones() {
        return Collections.unmodifiableList(posiciones);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Temporada that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Temporada{" +
                "id=" + id +
                ", ligaId=" + ligaId +
                ", nombre='" + nombre + '\'' +
                ", semestre=" + semestre +
                ", anioInicio=" + anioInicio +
                ", anioFin=" + anioFin +
                ", estado=" + estado +
                ", equipos=" + equipos.size() +
                ", posiciones=" + posiciones.size() +
                '}';
    }
}
