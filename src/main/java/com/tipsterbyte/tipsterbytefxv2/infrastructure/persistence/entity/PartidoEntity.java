// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA del aggregate Partido (tabla partidos).
// [POR QUÉ]: Es la representación persistente del aggregate Partido del dominio. Los
//            equipos local/visitante se persisten denormalizados (id + nombre) para
//            respetar la regla de "referencias por id entre agregados" y evitar ciclos
//            JPA (Equipo ya pertenece a Liga). El resultado se guarda como columnas
//            anulables (solo existe al finalizar, BR-003).
// [ALTERNATIVAS]: @ManyToOne a EquipoEntity para local/visitante; se descarta porque
//                 crea un ciclo de asociaciones Liga→Equipo→Partido y acopla agregados
//                 que el dominio relaciona por id.
// [RELACIONES]: Mapea domain.model.Partido (CU-02, CU-03, CU-05). Convertida por
//               PartidoRepositoryJpaAdapter. Compone CuotaEntity.
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

    // Referencias por id entre agregados (Partido → Liga).
    @Column(name = "liga_id", nullable = false)
    private UUID ligaId;

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

    public PartidoEntity(UUID id, UUID ligaId, UUID equipoLocalId, String equipoLocalNombre,
                         UUID equipoVisitanteId, String equipoVisitanteNombre,
                         LocalDateTime fechaHora, EstadoPartido estado,
                         Integer resultadoGolesLocal, Integer resultadoGolesVisitante) {
        this.id = id;
        this.ligaId = ligaId;
        this.equipoLocalId = equipoLocalId;
        this.equipoLocalNombre = equipoLocalNombre;
        this.equipoVisitanteId = equipoVisitanteId;
        this.equipoVisitanteNombre = equipoVisitanteNombre;
        this.fechaHora = fechaHora;
        this.estado = estado;
        this.resultadoGolesLocal = resultadoGolesLocal;
        this.resultadoGolesVisitante = resultadoGolesVisitante;
    }

    public void agregarCuota(CuotaEntity cuota) {
        cuota.setPartido(this);
        this.cuotas.add(cuota);
    }

    public UUID getId() {
        return id;
    }

    public UUID getLigaId() {
        return ligaId;
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