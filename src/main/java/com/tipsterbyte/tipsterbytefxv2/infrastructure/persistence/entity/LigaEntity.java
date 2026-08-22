// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA del aggregate Liga (tabla ligas), con referencia real a su país.
// [POR QUÉ]: Es la representación persistente del aggregate Liga del dominio. Vive en
//            infraestructura para que el dominio no conozca JPA ni PostgreSQL. El
//            adapter/mapper convierte entre LigaEntity y el aggregate Liga.
//            Las temporadas viven en tabla propia (temporadas) con relación 1:N, y SON
//            quienes componen equipos y posiciones (fase dedicada de temporadas): una
//            liga puede tener varias temporadas y cada una conserva su plantilla e
//            historial de tablas.
//            pais_id es FK real a paises.id (integridad referencial); el nombre del
//            país se conserva denormalizado para display.
// [ALTERNATIVAS]: Anotar el aggregate de dominio con @Entity; se descarta porque
//                 acoplaría el dominio a JPA, violando la Dependency Rule.
//                 Columnas planas temporada_anio_inicio/fin; se descartan porque no
//                 soportan múltiples temporadas por liga.
//                 Colecciones equipos/posiciones aquí (modelo anterior); se descartan
//                 porque son de la temporada, no de la liga genérica.
// [RELACIONES]: Mapea el aggregate Liga (CU-01, CU-02, CU-04). Convertida por
//               LigaRepositoryJpaAdapter. Compone TemporadaEntity (FK
//               temporadas.liga_id → ligas.id). Refiere PaisEntity (pais_id).
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "ligas")
public class LigaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    // Nombre del país denormalizado para display (la relación real es pais_id).
    @Column(name = "pais", nullable = false, length = 60)
    private String paisNombre;

    // País del catálogo (FK ligas.pais_id → paises.id). Nullable para ligas creadas
    // manualmente sin catálogo previo; CU-10 siempre lo provee.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pais_id")
    private PaisEntity pais;

    // Datos de la fuente #5 (catálogo): URL de Soccerway (path_to_scrape del calendario #4)
    // y api_id opcional de API-Football. Pueden ser nulos si la liga se creó manualmente.
    @Column(name = "url_soccerway", length = 300)
    private String urlSoccerway;

    @Column(name = "api_id", length = 50)
    private String apiId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoLiga estado;

    // Temporadas de la liga (owned side: TemporadaEntity lleva la FK liga_id).
    // Los equipos y las posiciones cuelgan de TemporadaEntity, no de aquí.
    @OneToMany(mappedBy = "liga", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<TemporadaEntity> temporadas = new HashSet<>();

    protected LigaEntity() {
    }

    public LigaEntity(UUID id, String nombre, String pais, EstadoLiga estado) {
        this.id = id;
        this.nombre = nombre;
        this.paisNombre = pais;
        this.urlSoccerway = null;
        this.apiId = null;
        this.estado = estado;
    }

    public LigaEntity(UUID id, String nombre, String pais, String urlSoccerway,
                      String apiId, EstadoLiga estado) {
        this.id = id;
        this.nombre = nombre;
        this.paisNombre = pais;
        this.urlSoccerway = urlSoccerway;
        this.apiId = apiId;
        this.estado = estado;
    }

    public void agregarTemporada(TemporadaEntity temporada) {
        temporada.setLiga(this);
        this.temporadas.add(temporada);
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    // Nombre del país denormalizado (display).
    public String getPais() {
        return paisNombre;
    }

    public PaisEntity getPaisRef() {
        return pais;
    }

    public void setPaisRef(PaisEntity pais) {
        this.pais = pais;
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

    public Set<TemporadaEntity> getTemporadas() {
        return temporadas;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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
