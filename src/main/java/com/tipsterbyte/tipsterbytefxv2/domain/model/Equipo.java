// ─────────────────────────────────────────────
// [QUÉ]: Entity que representa un club que participa en una liga/temporada.
// [POR QUÉ]: Un equipo tiene identidad propia que permanece (su id), aunque sus
//            atributos (nombre, escudo) cambien. Por eso es Entity y no VO.
//            El logoUrl (escudo) llega de la fuente de poblamiento #6
//            (ext-soccerway-teams-by-league) y es opcional: los equipos registrados
//            por las fuentes operativas (#3/#4) pueden no tenerlo.
// [ALTERNATIVAS]: VO sin identidad; se descarta porque dos equipos con el mismo
//                 nombre serían el mismo, lo cual es incorrecto en el negocio.
// [RELACIONES]: Miembro del aggregate Liga vía Temporada; referenciado por Partido,
//               PosicionTabla y PosicionTablaEntity (persistido en equipos.logo_url).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.util.Objects;
import java.util.UUID;

public final class Equipo {

    private final UUID id;
    private final String nombre;
    private final String logoUrl;

    // [QUÉ]: Construye un equipo generando su identidad (sin escudo conocido).
    public Equipo(String nombre) {
        this(UUID.randomUUID(), nombre, null);
    }

    // [QUÉ]: Construye un equipo generando identidad, con escudo (fuente #6).
    public Equipo(String nombre, String logoUrl) {
        this(UUID.randomUUID(), nombre, logoUrl);
    }

    // [QUÉ]: Construye un equipo con identidad provista (reconstrucción desde persistencia).
    public Equipo(UUID id, String nombre) {
        this(id, nombre, null);
    }

    public Equipo(UUID id, String nombre, String logoUrl) {
        if (id == null) {
            throw new DomainException("Equipo requiere id");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("Equipo requiere nombre");
        }
        this.id = id;
        this.nombre = nombre;
        this.logoUrl = logoUrl;
    }

    public UUID id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public String logoUrl() {
        return logoUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Equipo equipo)) return false;
        return id.equals(equipo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
