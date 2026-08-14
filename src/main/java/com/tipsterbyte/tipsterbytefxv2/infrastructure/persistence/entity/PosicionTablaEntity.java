// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA de una fila de la tabla de posiciones (tabla posiciones_tabla).
// [POR QUÉ]: PosicionTabla es un VO del dominio sin identidad; al persistirse necesita
//            un id técnico de fila. Se guarda con referencia al equipo al que pertenece.
// [ALTERNATIVAS]: Embeddable en LigaEntity; se descarta porque la cantidad de filas
//                 por liga es dinámica y conviene una tabla propia con FK a equipo.
// [RELACIONES]: Mapea domain.model.PosicionTabla. Compuesta por LigaEntity; refiere
//               a EquipoEntity.
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

@Entity
@Table(name = "posiciones_tabla")
public class PosicionTablaEntity {

    // Id técnico de fila: el VO de dominio no tiene identidad, pero JPA la requiere.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liga_id", nullable = false)
    private LigaEntity liga;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    private EquipoEntity equipo;

    @Column(name = "posicion", nullable = false)
    private int posicion;

    @Column(name = "jugados", nullable = false)
    private int jugados;

    @Column(name = "ganados", nullable = false)
    private int ganados;

    @Column(name = "empatados", nullable = false)
    private int empatados;

    @Column(name = "perdidos", nullable = false)
    private int perdidos;

    @Column(name = "goles_favor", nullable = false)
    private int golesFavor;

    @Column(name = "goles_contra", nullable = false)
    private int golesContra;

    @Column(name = "puntos", nullable = false)
    private int puntos;

    protected PosicionTablaEntity() {
    }

    public PosicionTablaEntity(EquipoEntity equipo, int posicion, int jugados, int ganados,
                               int empatados, int perdidos, int golesFavor, int golesContra, int puntos) {
        this.equipo = equipo;
        this.posicion = posicion;
        this.jugados = jugados;
        this.ganados = ganados;
        this.empatados = empatados;
        this.perdidos = perdidos;
        this.golesFavor = golesFavor;
        this.golesContra = golesContra;
        this.puntos = puntos;
    }

    public void setLiga(LigaEntity liga) {
        this.liga = liga;
    }

    public EquipoEntity getEquipo() {
        return equipo;
    }

    public int getPosicion() {
        return posicion;
    }

    public int getJugados() {
        return jugados;
    }

    public int getGanados() {
        return ganados;
    }

    public int getEmpatados() {
        return empatados;
    }

    public int getPerdidos() {
        return perdidos;
    }

    public int getGolesFavor() {
        return golesFavor;
    }

    public int getGolesContra() {
        return golesContra;
    }

    public int getPuntos() {
        return puntos;
    }
}