// ---------------------------------------------
// [QUÉ]: Entidad JPA de una temporada deportiva específica (tabla temporadas), con su
//        plantilla de equipos y su tabla de posiciones.
// [POR QUÉ]: Es la representación persistente del entity Temporada del dominio.
//            Vive en infraestructura para que el dominio no conozca JPA ni PostgreSQL.
//            Usa tabla propia (temporadas) para almacenar información de temporadas.
//            Los equipos y las posiciones SON de una temporada (un descendido no está
//            en la tabla siguiente), por eso las colecciones viven aquí y ya no en
//            LigaEntity (fase dedicada de temporadas).
// [ALTERNATIVAS]: Almacenar como columnas en ligas; se descarta porque una liga puede
//                 tener múltiples temporadas y necesitamos historial.
//                 Colecciones en LigaEntity (modelo anterior); se descartan porque el
//                 dato quedaba ambiguo con varias temporadas por liga.
// [RELACIONES]: Mapea el entity Temporada (uno-a-muchos desde Liga). Compone
//               EquipoEntity y PosicionTablaEntity. Referida por PartidoEntity y
//               DetalleFuenteExtraccionEntity.
// ---------------------------------------------
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoTemporada;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "temporadas",
       uniqueConstraints = @UniqueConstraint(columnNames = {"liga_id", "nombre"}))
public class TemporadaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "liga_id", nullable = false)
    private LigaEntity liga;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre; // nombre_torneo de fuente #5: Apertura, Clausura, etc.

    @Column(name = "semestre")
    private Integer semestre; // 1 o 2, de fuente #5

    @Column(name = "anio_inicio", nullable = false)
    private int anioInicio;

    @Column(name = "anio_fin", nullable = false)
    private int anioFin;

    @Column(name = "estado", nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoTemporada estado;

    // Equipos de la plantilla de ESTA temporada (owned side: EquipoEntity lleva la FK).
    @OneToMany(mappedBy = "temporada", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EquipoEntity> equipos = new ArrayList<>();

    // Tabla de posiciones de ESTA temporada.
    @OneToMany(mappedBy = "temporada", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("posicion ASC")
    private List<PosicionTablaEntity> posiciones = new ArrayList<>();

    // Constructores
    protected TemporadaEntity() {
    }

    public TemporadaEntity(UUID id, LigaEntity liga, String nombre, Integer semestre,
                           int anioInicio, int anioFin, EstadoTemporada estado) {
        this.id = id;
        this.liga = liga;
        this.nombre = nombre;
        this.semestre = semestre;
        this.anioInicio = anioInicio;
        this.anioFin = anioFin;
        this.estado = estado;
    }

    public void agregarEquipo(EquipoEntity equipo) {
        equipo.setTemporada(this);
        this.equipos.add(equipo);
    }

    public void agregarPosicion(PosicionTablaEntity posicion) {
        posicion.setTemporada(this);
        this.posiciones.add(posicion);
    }

    // Getters y Setters
    public UUID getId() {
        return id;
    }

    public LigaEntity getLiga() {
        return liga;
    }

    public void setLiga(LigaEntity liga) {
        this.liga = liga;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getSemestre() {
        return semestre;
    }

    public void setSemestre(Integer semestre) {
        this.semestre = semestre;
    }

    public int getAnioInicio() {
        return anioInicio;
    }

    public void setAnioInicio(int anioInicio) {
        this.anioInicio = anioInicio;
    }

    public int getAnioFin() {
        return anioFin;
    }

    public void setAnioFin(int anioFin) {
        this.anioFin = anioFin;
    }

    public EstadoTemporada getEstado() {
        return estado;
    }

    public void setEstado(EstadoTemporada estado) {
        this.estado = estado;
    }

    public List<EquipoEntity> getEquipos() {
        return equipos;
    }

    public List<PosicionTablaEntity> getPosiciones() {
        return posiciones;
    }

    // Equals y HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemporadaEntity that)) return false;
        return id.equals(that.id);
    }
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
