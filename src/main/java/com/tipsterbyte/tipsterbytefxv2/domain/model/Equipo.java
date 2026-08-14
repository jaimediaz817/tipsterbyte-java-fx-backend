// ─────────────────────────────────────────────
// [QUÉ]: Entity que representa un club que participa en una liga.
// [POR QUÉ]: Un equipo tiene identidad propia que permanece (su id), aunque sus
//            atributos (nombre, estadio) cambien. Por eso es Entity y no VO.
// [ALTERNATIVAS]: VO sin identidad; se descarta porque dos equipos con el mismo
//                 nombre serían el mismo, lo cual es incorrecto en el negocio.
// [RELACIONES]: Miembro del aggregate Liga; referenciado por Partido y PosicionTabla.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.util.Objects;
import java.util.UUID;

public final class Equipo {

    private final UUID id;
    private final String nombre;

    // [QUÉ]: Construye un equipo generando su identidad.
    public Equipo(String nombre) {
        this(UUID.randomUUID(), nombre);
    }

    // [QUÉ]: Construye un equipo con identidad provista (reconstrucción desde persistencia).
    // [POR QUÉ]: En FASE 8 el id puede venir de la BD; este constructor lo soporta.
    public Equipo(UUID id, String nombre) {
        if (id == null) {
            throw new DomainException("Equipo requiere id");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("Equipo requiere nombre");
        }
        this.id = id;
        this.nombre = nombre;
    }

    public UUID id() {
        return id;
    }

    public String nombre() {
        return nombre;
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