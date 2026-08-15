// ─────────────────────────────────────────────
// [QUÉ]: Entity que representa un usuario autenticable de la plataforma con sus
//        credenciales (email + password hasheado) y rol (TIPSTER/CLIENTE/ADMIN).
// [POR QUÉ]: FASE 11 (Security + JWT) separa el concern de autenticación del de
//            negocio: Tipster/Cliente son perfiles de negocio sin contraseña; Usuario
//            es quien inicia sesión. El hash nunca se expone (sin getter de password).
// [ALTERNATIVAS]: Añadir password a Tipster/Cliente; se descarta porque mezcla
//                 autenticación con perfiles de negocio y duplica credenciales.
//                 Un solo tipo "Usuario" con rol para todo; se descarta porque
//                 tipster/cliente tienen comportamientos distintos en el negocio.
// [RELACIONES]: CU-12 (registro), CU-13 (login); autenticado por JwtAuthenticationFilter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.util.Objects;
import java.util.UUID;

public final class Usuario {

    private final UUID id;
    private final String nombre;
    private final Email email;
    private final String passwordHash;
    private final Rol rol;
    private final boolean activo;

    // [QUÉ]: Construye un usuario nuevo (id generado, ACTIVO por defecto).
    public Usuario(String nombre, Email email, String passwordHash, Rol rol) {
        this(UUID.randomUUID(), nombre, email, passwordHash, rol, true);
    }

    // [QUÉ]: Construye un usuario con identidad y estado provistos (reconstrucción).
    // [POR QUÉ]: Valida invariantes: nombre, email, hash no vacío y rol no nulo.
    public Usuario(UUID id, String nombre, Email email, String passwordHash, Rol rol, boolean activo) {
        if (id == null) {
            throw new DomainException("Usuario requiere id");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("Usuario requiere nombre");
        }
        if (email == null) {
            throw new DomainException("Usuario requiere email");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainException("Usuario requiere password hasheado");
        }
        if (rol == null) {
            throw new DomainException("Usuario requiere rol");
        }
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
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

    public String passwordHash() {
        return passwordHash;
    }

    public Rol rol() {
        return rol;
    }

    public boolean activo() {
        return activo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario usuario)) return false;
        return id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
