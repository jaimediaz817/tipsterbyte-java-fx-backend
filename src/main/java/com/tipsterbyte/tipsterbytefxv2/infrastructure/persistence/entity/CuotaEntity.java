// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA de una cuota de un partido (tabla cuotas).
// [POR QUÉ]: El aggregate Partido mantiene una lista de cuotas (CU-03); cada cuota es
//            un VO del dominio sin identidad, así que aquí recibe un id técnico de fila.
//            Se relaciona con PartidoEntity como colección owned.
// [ALTERNATIVAS]: Embeddable en PartidoEntity; se descarta porque la cantidad de cuotas
//                 por partido es dinámica y conviene una tabla propia.
// [RELACIONES]: Mapea domain.model.Cuota. Compuesta por PartidoEntity.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    protected CuotaEntity() {
    }

    public CuotaEntity(BigDecimal valor) {
        this.valor = valor;
    }

    public void setPartido(PartidoEntity partido) {
        this.partido = partido;
    }

    public BigDecimal getValor() {
        return valor;
    }
}