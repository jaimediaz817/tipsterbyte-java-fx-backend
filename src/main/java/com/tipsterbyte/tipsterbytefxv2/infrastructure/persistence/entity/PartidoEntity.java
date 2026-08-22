// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA del aggregate Partido (tabla partidos).
// [POR QUÉ]: Es la representación persistente del aggregate Partido del dominio. Los
//            equipos local/visitante se persisten denormalizados (id + nombre) para
//            respetar la regla de "referencias por id entre agregados" y evitar ciclos
//            JPA (Equipo ya pertenece a Liga). El resultado se guarda como columnas
//            anulables (solo existe al finalizar, BR-003).
//            El partido referencia su temporada (FK partidos.temporada_id →
//            temporadas.id, Bridge Fix Torneos/Temporadas): la liga se resuelve vía
//            JOIN a través de la temporada (temporadas.liga_id), no por columna propia.
// [ALTERNATIVAS]: @ManyToOne a EquipoEntity para local/visitante; se descarta porque
//                 crea un ciclo de asociaciones Liga→Equipo→Partido y acopla agregados
//                 que el dominio relaciona por id.
//                 Columna plana liga_id (modelo anterior); se descarta porque el
//                 partido pertenece a una temporada concreta de la liga.
// [RELACIONES]: Mapea domain.model.Partido (CU-02, CU-03, CU-05). Convertida por
//               PartidoRepositoryJpaAdapter. Compone CuotaEntity. Refiere TemporadaEntity.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "partidos")
public class PartidoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Temporada del partido (FK partidos.temporada_id → temporadas.id). La liga se
    // deriva vía temporada.liga para las consultas por liga (JOIN en el repositorio).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "temporada_id", nullable = false)
    private TemporadaEntity temporada;

    // Equipos denormalizados: solo se necesitan id y nombre para reconstruir el dominio.
    @Column(name = "equipo_local_id", nullable = false)
    private UUID equipoLocalId;

    @Column(name = "equipo_local_nombre", nullable = false, length = 100)
    private String equipoLocalNombre;

    @Column(name = "equipo_visitante_id", nullable = false)
    private UUID equipoVisitanteId;

    @Column(name = "equipo_visitante_nombre", nullable = false, length = 100)
    private String equipoVisitanteNombre;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    // Jornada del torneo (fuente #4, label "Jornada N"); nullable para filas legadas.
    @Column(name = "jornada")
    private Integer jornada;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPartido estado;

    // Resultado solo existe al finalizar (BR-003): columnas anulables.
    @Column(name = "resultado_goles_local")
    private Integer resultadoGolesLocal;

    @Column(name = "resultado_goles_visitante")
    private Integer resultadoGolesVisitante;

    @OneToMany(mappedBy = "partido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CuotaEntity> cuotas = new ArrayList<>();

    protected PartidoEntity() {
    }

    public PartidoEntity(UUID id, TemporadaEntity temporada, UUID equipoLocalId, String equipoLocalNombre,
                         UUID equipoVisitanteId, String equipoVisitanteNombre,
                         LocalDateTime fechaHora, EstadoPartido estado,
                         Integer resultadoGolesLocal, Integer resultadoGolesVisitante,
                         Integer jornada) {
        this.id = id;
        this.temporada = temporada;
        this.equipoLocalId = equipoLocalId;
        this.equipoLocalNombre = equipoLocalNombre;
        this.equipoVisitanteId = equipoVisitanteId;
        this.equipoVisitanteNombre = equipoVisitanteNombre;
        this.fechaHora = fechaHora;
        this.estado = estado;
        this.resultadoGolesLocal = resultadoGolesLocal;
        this.resultadoGolesVisitante = resultadoGolesVisitante;
        this.jornada = jornada;
    }

    public void agregarCuota(CuotaEntity cuota) {
        cuota.setPartido(this);
        this.cuotas.add(cuota);
    }

    public UUID getId() {
        return id;
    }

    public TemporadaEntity getTemporada() {
        return temporada;
    }

    // [QUÉ]: Conveniencia de lectura del id de la temporada (evita inicializar el proxy).
    public UUID getTemporadaId() {
        return temporada != null ? temporada.getId() : null;
    }

    public UUID getEquipoLocalId() {
        return equipoLocalId;
    }

    public String getEquipoLocalNombre() {
        return equipoLocalNombre;
    }

    public UUID getEquipoVisitanteId() {
        return equipoVisitanteId;
    }

    public String getEquipoVisitanteNombre() {
        return equipoVisitanteNombre;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public Integer getJornada() {
        return jornada;
    }

    public EstadoPartido getEstado() {
        return estado;
    }

    public Integer getResultadoGolesLocal() {
        return resultadoGolesLocal;
    }

    public Integer getResultadoGolesVisitante() {
        return resultadoGolesVisitante;
    }

    public List<CuotaEntity> getCuotas() {
        return cuotas;
    }

    public void setEstado(EstadoPartido estado) {
        this.estado = estado;
    }

    public void setResultadoGolesLocal(Integer resultadoGolesLocal) {
        this.resultadoGolesLocal = resultadoGolesLocal;
    }

    public void setResultadoGolesVisitante(Integer resultadoGolesVisitante) {
        this.resultadoGolesVisitante = resultadoGolesVisitante;
    }
}
