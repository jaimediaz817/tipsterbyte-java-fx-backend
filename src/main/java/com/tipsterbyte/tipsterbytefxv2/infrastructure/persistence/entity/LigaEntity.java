// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA del aggregate Liga (tabla ligas).
// [POR QUÉ]: Es la representación persistente del aggregate Liga del dominio. Vive en
//            infraestructura para que el dominio no conozca JPA ni PostgreSQL. El
//            adapter/mapper convierte entre LigaEntity y el aggregate Liga.
// [ALTERNATIVAS]: Anotar el aggregate de dominio con @Entity; se descarta porque
//                 acoplaría el dominio a JPA, violando la Dependency Rule.
// [RELACIONES]: Mapea el aggregate Liga (CU-01, CU-02, CU-04). Convertida por
//               LigaRepositoryJpaAdapter. Compone EquipoEntity y PosicionTablaEntity.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ligas")
public class LigaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "pais", nullable = false, length = 60)
    private String pais;

    // Temporada embebida como columnas planas (año inicio/fin).
    @Column(name = "temporada_anio_inicio", nullable = false)
    private int temporadaAnioInicio;

    @Column(name = "temporada_anio_fin", nullable = false)
    private int temporadaAnioFin;

    // Datos de la fuente #5 (catálogo): URL de Soccerway (path_to_scrape del calendario #4)
    // y api_id opcional de API-Football. Pueden ser nulos si la liga se creó manualmente.
    @Column(name = "url_soccerway", length = 300)
    private String urlSoccerway;

    @Column(name = "api_id", length = 50)
    private String apiId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoLiga estado;

    // Equipos que pertenecen a la liga (owned side: EquipoEntity lleva la FK liga_id).
    @OneToMany(mappedBy = "liga", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EquipoEntity> equipos = new ArrayList<>();

    // Posiciones de la tabla de la liga.
    @OneToMany(mappedBy = "liga", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("posicion ASC")
    private List<PosicionTablaEntity> posiciones = new ArrayList<>();

    protected LigaEntity() {
    }

    public LigaEntity(UUID id, String nombre, String pais, int temporadaAnioInicio,
                      int temporadaAnioFin, EstadoLiga estado) {
        this.id = id;
        this.nombre = nombre;
        this.pais = pais;
        this.temporadaAnioInicio = temporadaAnioInicio;
        this.temporadaAnioFin = temporadaAnioFin;
        this.urlSoccerway = null;
        this.apiId = null;
        this.estado = estado;
    }

    public LigaEntity(UUID id, String nombre, String pais, int temporadaAnioInicio,
                      int temporadaAnioFin, String urlSoccerway, String apiId, EstadoLiga estado) {
        this.id = id;
        this.nombre = nombre;
        this.pais = pais;
        this.temporadaAnioInicio = temporadaAnioInicio;
        this.temporadaAnioFin = temporadaAnioFin;
        this.urlSoccerway = urlSoccerway;
        this.apiId = apiId;
        this.estado = estado;
    }

    public void agregarEquipo(EquipoEntity equipo) {
        equipo.setLiga(this);
        this.equipos.add(equipo);
    }

    public void agregarPosicion(PosicionTablaEntity posicion) {
        posicion.setLiga(this);
        this.posiciones.add(posicion);
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPais() {
        return pais;
    }

    public int getTemporadaAnioInicio() {
        return temporadaAnioInicio;
    }

    public int getTemporadaAnioFin() {
        return temporadaAnioFin;
    }

    public EstadoLiga getEstado() {
        return estado;
    }

    public String getUrlSoccerway() {
        return urlSoccerway;
    }

    public String getApiId() {
        return apiId;
    }

    public List<EquipoEntity> getEquipos() {
        return equipos;
    }

    public List<PosicionTablaEntity> getPosiciones() {
        return posiciones;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public void setTemporadaAnioInicio(int temporadaAnioInicio) {
        this.temporadaAnioInicio = temporadaAnioInicio;
    }

    public void setTemporadaAnioFin(int temporadaAnioFin) {
        this.temporadaAnioFin = temporadaAnioFin;
    }

    public void setEstado(EstadoLiga estado) {
        this.estado = estado;
    }

    public void setUrlSoccerway(String urlSoccerway) {
        this.urlSoccerway = urlSoccerway;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }
}