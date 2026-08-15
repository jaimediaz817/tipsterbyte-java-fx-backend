// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA de una cuota de un partido (tabla cuotas).
// [POR QUÉ]: El aggregate Partido mantiene una lista de cuotas (CU-03); cada cuota es
//            un VO del dominio sin identidad, así que aquí recibe un id técnico de fila.
//            Se relaciona con PartidoEntity como colección owned. La columna mercado
//            (FASE 8.5) persiste el mercado real de la cuota: la fuente #2 entrega 3 de
//            UNO_X_DOS y 3 de DOBLE_OPORTUNIDAD por partido; sin ella las cuotas de doble
//            oportunidad se perderían.
// [ALTERNATIVAS]: Embeddable en PartidoEntity; se descarta porque la cantidad de cuotas
//                 por partido es dinámica y conviene una tabla propia.
// [RELACIONES]: Mapea domain.model.Cuota (mercado + valor). Compuesta por PartidoEntity.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "cuotas")
public class CuotaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partido_id", nullable = false)
    private PartidoEntity partido;

    @Enumerated(EnumType.STRING)
    @Column(name = "mercado", nullable = false, length = 20)
    private Mercado mercado;

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    protected CuotaEntity() {
    }

    public CuotaEntity(Mercado mercado, BigDecimal valor) {
        this.mercado = mercado;
        this.valor = valor;
    }

    public void setPartido(PartidoEntity partido) {
        this.partido = partido;
    }

    public Mercado getMercado() {
        return mercado;
    }

    public BigDecimal getValor() {
        return valor;
    }
}