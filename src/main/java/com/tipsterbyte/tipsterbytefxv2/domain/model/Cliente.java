// ─────────────────────────────────────────────
// [QUÉ]: Entity que representa a un cliente: usuario que consume pronósticos de
//        tipsters mediante suscripciones.
// [POR QUÉ]: Usuario con identidad propia y rol fijo CLIENTE. La regla BR-006
//            (consumir solo pronósticos de tipsters con suscripción activa) se
//            aplica al cruzar Cliente con Suscripcion.
// [ALTERNATIVAS]: Un solo tipo "Usuario"; se descarta por comportamientos distintos.
// [RELACIONES]: Referenciado por Suscripcion (CU-09). Con BR-006 (CU-08).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.util.Objects;
import java.util.UUID;

public final class Cliente {

    private final UUID id;
    private final String nombre;
    private final Email email;
    private final Rol rol;

    // [QUÉ]: Construye un cliente con rol fijo.
    public Cliente(String nombre, Email email) {
        this(UUID.randomUUID(), nombre, email, Rol.CLIENTE);
    }

    // [QUÉ]: Construye un cliente con identidad provista (reconstrucción desde persistencia).
    public Cliente(UUID id, String nombre, Email email, Rol rol) {
        if (id == null) {
            throw new DomainException("Cliente requiere id");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("Cliente requiere nombre");
        }
        if (email == null) {
            throw new DomainException("Cliente requiere email");
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
        if (!(o instanceof Cliente cliente)) return false;
        return id.equals(cliente.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}