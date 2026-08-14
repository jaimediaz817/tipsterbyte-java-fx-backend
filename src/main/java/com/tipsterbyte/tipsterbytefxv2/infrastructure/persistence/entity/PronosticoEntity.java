// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA del aggregate Pronostico (tabla pronosticos).
// [POR QUÉ]: Representación persistente del aggregate Pronostico. La selección
//            (mercado + resultado esperado) y la cuota se guardan como columnas planas
//            porque son VOs de un solo valor persistible cada uno. Partido y tipster
//            se referencian por id (regla de referencias por id entre agregados).
// [ALTERNATIVAS]: Tablas separadas para selección/cuota; se descarta porque no tienen
//                 colecciones ni identidad propia que justifique tablas extra.
// [RELACIONES]: Mapea domain.model.Pronostico (CU-06, CU-07, CU-08). Convertida por
//               PronosticoRepositoryJpaAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPronostico;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pronosticos")
public class PronosticoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // Referencias por id entre agregados (Pronostico → Tipster).
    @Column(name = "tipster_id", nullable = false)
    private UUID tipsterId;

    // Referencias por id entre agregados (Pronostico → Partido).
    @Column(name = "partido_id", nullable = false)
    private UUID partidoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mercado", nullable = false, length = 20)
    private Mercado mercado;

    @Column(name = "resultado_esperado", nullable = false, length = 10)
    private String resultadoEsperado;

    @Column(name = "cuota_valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal cuotaValor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPronostico estado;

    // Resultado final (CU-05) verificado del pronóstico: columnas anulables.
    @Column(name = "resultado_final_goles_local")
    private Integer resultadoFinalGolesLocal;

    @Column(name = "resultado_final_goles_visitante")
    private Integer resultadoFinalGolesVisitante;

    protected PronosticoEntity() {
    }

    public PronosticoEntity(UUID id, UUID tipsterId, UUID partidoId, Mercado mercado,
                            String resultadoEsperado, BigDecimal cuotaValor, EstadoPronostico estado,
                            Integer resultadoFinalGolesLocal, Integer resultadoFinalGolesVisitante) {
        this.id = id;
        this.tipsterId = tipsterId;
        this.partidoId = partidoId;
        this.mercado = mercado;
        this.resultadoEsperado = resultadoEsperado;
        this.cuotaValor = cuotaValor;
        this.estado = estado;
        this.resultadoFinalGolesLocal = resultadoFinalGolesLocal;
        this.resultadoFinalGolesVisitante = resultadoFinalGolesVisitante;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTipsterId() {
        return tipsterId;
    }

    public UUID getPartidoId() {
        return partidoId;
    }

    public Mercado getMercado() {
        return mercado;
    }

    public String getResultadoEsperado() {
        return resultadoEsperado;
    }

    public BigDecimal getCuotaValor() {
        return cuotaValor;
    }

    public EstadoPronostico getEstado() {
        return estado;
    }

    public Integer getResultadoFinalGolesLocal() {
        return resultadoFinalGolesLocal;
    }

    public Integer getResultadoFinalGolesVisitante() {
        return resultadoFinalGolesVisitante;
    }

    public void setEstado(EstadoPronostico estado) {
        this.estado = estado;
    }

    public void setResultadoFinalGolesLocal(Integer resultadoFinalGolesLocal) {
        this.resultadoFinalGolesLocal = resultadoFinalGolesLocal;
    }

    public void setResultadoFinalGolesVisitante(Integer resultadoFinalGolesVisitante) {
        this.resultadoFinalGolesVisitante = resultadoFinalGolesVisitante;
    }
}