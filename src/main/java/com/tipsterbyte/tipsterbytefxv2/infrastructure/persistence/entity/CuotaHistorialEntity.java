package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import com.tipsterbyte.tipsterbytefxv2.domain.model.CuotaHistorial;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cuota_historial")
public class CuotaHistorialEntity {

    @Id
    private UUID id;

    @Column(name = "partido_id", nullable = false)
    private UUID partidoId;

    @Column(name = "mercado", nullable = false)
    @Enumerated(EnumType.STRING)
    private Mercado mercado;

    @Column(name = "seleccion")
    private String seleccion;

    @Column(name = "valor", nullable = false)
    private BigDecimal valor;

    @Column(name = "fuente", nullable = false)
    private String fuente;

    @Column(name = "capturada_en", nullable = false)
    private Instant capturadaEn;

    public CuotaHistorialEntity() {
    }

    public CuotaHistorialEntity(UUID id, UUID partidoId, Mercado mercado, String seleccion,
                                 BigDecimal valor, String fuente, Instant capturadaEn) {
        this.id = id;
        this.partidoId = partidoId;
        this.mercado = mercado;
        this.seleccion = seleccion;
        this.valor = valor;
        this.fuente = fuente;
        this.capturadaEn = capturadaEn;
    }

    public CuotaHistorial toDomainModel() {
        return new CuotaHistorial(id, partidoId, mercado, seleccion, valor, fuente, capturadaEn);
    }

    public static CuotaHistorialEntity fromDomainModel(CuotaHistorial cuota) {
        return new CuotaHistorialEntity(cuota.id(), cuota.partidoId(), cuota.mercado(),
                cuota.seleccion(), cuota.valor(), cuota.fuente(), cuota.capturadaEn());
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPartidoId() { return partidoId; }
    public void setPartidoId(UUID partidoId) { this.partidoId = partidoId; }
    public Mercado getMercado() { return mercado; }
    public void setMercado(Mercado mercado) { this.mercado = mercado; }
    public String getSeleccion() { return seleccion; }
    public void setSeleccion(String seleccion) { this.seleccion = seleccion; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }
    public Instant getCapturadaEn() { return capturadaEn; }
    public void setCapturadaEn(Instant capturadaEn) { this.capturadaEn = capturadaEn; }
}
