// ─────────────────────────────────────────────
// [QUÉ]: Entity que representa a un tipster: usuario que crea y publica pronósticos.
// [POR QUÉ]: Usuario con identidad propia y rol fijo TIPSTER. Se modela la
//            identidad mínima ahora; auth completa llega en FASE 11 (Security/JWT).
// [ALTERNATIVAS]: Un solo tipo "Usuario" con rol; se descarta porque tipster y
//                 cliente tienen comportamientos y reglas distintos en el negocio.
// [RELACIONES]: Referenciado por Pronostico y Suscripcion (CU-06, CU-07, CU-09).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.util.Objects;
import java.util.UUID;

public final class Tipster {

    private final UUID id;
    private final String nombre;
    private final Email email;
    private final Rol rol;

    // [QUÉ]: Construye un tipster con rol fijo.
    public Tipster(String nombre, Email email) {
        this(UUID.randomUUID(), nombre, email, Rol.TIPSTER);
    }

    // [QUÉ]: Construye un tipster con identidad provista (reconstrucción desde persistencia).
    public Tipster(UUID id, String nombre, Email email, Rol rol) {
        if (id == null) {
            throw new DomainException("Tipster requiere id");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("Tipster requiere nombre");
        }
        if (email == null) {
            throw new DomainException("Tipster requiere email");
        }
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
    }

    public UUID id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public Email email() {
        return email;
    }

    public Rol rol() {
        return rol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tipster tipster)) return false;
        return id.equals(tipster.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}