// ─────────────────────────────────────────────
// [QUÉ]: Entidad JPA del catálogo de fuentes de extracción (tabla fuentes_extraccion).
// [POR QUÉ]: Es la representación persistente del entity FuenteExtraccion del dominio.
//            Vive en infraestructura para que el dominio no conozca JPA ni PostgreSQL.
//            Usa tabla propia (fuentes_extraccion) para no colisionar con el esquema
//            del proyecto Python (fuente_extraccion) en la misma BD compartida.
// [ALTERNATIVAS]: Reutilizar la tabla fuente_extraccion de Python; se descartó porque
//                 el esquema difiere (robot_type_enum, torneo_id) y el usuario decidió
//                 mantener el modelo JPA independiente.
// [RELACIONES]: Mapea el entity FuenteExtraccion (CU-11). Convertida por
//               FuenteExtraccionRepositoryJpaAdapter. Referida por DetalleFuenteExtraccionEntity.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "fuentes_extraccion")
public class FuenteExtraccionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    // Clave natural por tipo: una sola fuente registrada por tipo (STANDINGS/ODDS_WPLAY/CALENDAR).
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, unique = true, length = 20)
    private TipoFuenteExtraccion tipo;

    @Column(name = "activa", nullable = false)
    private boolean activa;

    protected FuenteExtraccionEntity() {
    }

    public FuenteExtraccionEntity(UUID id, String nombre, TipoFuenteExtraccion tipo, boolean activa) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.activa = activa;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoFuenteExtraccion getTipo() {
        return tipo;
    }

    public boolean isActiva() {
        return activa;
    }
}
