// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA de una fila de la tabla de posiciones (tabla posiciones_tabla).
// [POR QUÉ]: PosicionTabla es un VO del dominio sin identidad; al persistirse necesita
//            un id técnico de fila. Se guarda con referencia al equipo al que pertenece.
//            La racha de últimos 5 resultados se persiste como columna delimitada
//            (ej: "G,E,P,G,G", índice 0 = más reciente) por decisión de FASE 8.5:
//            la fuente #3 la entrega como diccionario {1..5} y no necesita relacional.
// [ALTERNATIVAS]: Tabla de resultados; se descartó por simplicidad (máx. 5 valores fijos).
// [RELACIONES]: Mapea domain.model.PosicionTabla. Compuesta por LigaEntity; refiere
//               a EquipoEntity.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.ResultadoReciente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    // Racha de los últimos 5 partidos en formato delimitado: "G,E,P,G,G" (índice 0 = más reciente).
    @Column(name = "ultimos_resultados", length = 9)
    private String ultimosResultados;

    protected PosicionTablaEntity() {
    }

    public PosicionTablaEntity(EquipoEntity equipo, int posicion, int jugados, int ganados,
                               int empatados, int perdidos, int golesFavor, int golesContra, int puntos,
                               List<ResultadoReciente> ultimosResultados) {
        this.equipo = equipo;
        this.posicion = posicion;
        this.jugados = jugados;
        this.ganados = ganados;
        this.empatados = empatados;
        this.perdidos = perdidos;
        this.golesFavor = golesFavor;
        this.golesContra = golesContra;
        this.puntos = puntos;
        this.ultimosResultados = codificarRacha(ultimosResultados);
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

    // [QUÉ]: Devuelve la racha decodificada (lista ordenada, índice 0 = más reciente).
    public List<ResultadoReciente> getUltimosResultados() {
        return decodificarRacha(ultimosResultados);
    }

    // [QUÉ]: Codifica la racha a "G,E,P" (G=GANADO, E=EMPATE, P=PERDIDO).
    private String codificarRacha(List<ResultadoReciente> racha) {
        if (racha == null || racha.isEmpty()) {
            return null;
        }
        return racha.stream()
                .map(r -> switch (r) {
                    case GANADO -> "G";
                    case EMPATE -> "E";
                    case PERDIDO -> "P";
                })
                .collect(Collectors.joining(","));
    }

    // [QUÉ]: Decodifica la cadena "G,E,P" en la lista de ResultadoReciente.
    private List<ResultadoReciente> decodificarRacha(String racha) {
        if (racha == null || racha.isBlank()) {
            return List.of();
        }
        return Arrays.stream(racha.split(","))
                .map(c -> switch (c) {
                    case "G" -> ResultadoReciente.GANADO;
                    case "E" -> ResultadoReciente.EMPATE;
                    case "P" -> ResultadoReciente.PERDIDO;
                    default -> throw new DomainException("Resultado reciente inválido en BD: " + c);
                })
                .toList();
    }
}