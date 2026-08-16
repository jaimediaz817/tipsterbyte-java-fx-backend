// ─────────────────────────────────────────────
// [QUÉ]: Test del entity Usuario: invariantes (id, nombre, email, hash, rol).
// [POR QUÉ]: Verifica que la entidad de autenticación rechaza estados inválidos
//            (FASE 11, CU-12/CU-13).
// [RELACIONES]: Usuario → Email (domain.model).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioTest {

    private final Email email = new Email("tipster@example.com");
    private final String hash = "$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGH";

    @Test
    void debe_crear_usuario_activo_con_rol_y_fecha_creacion() {
        Usuario usuario = new Usuario("Ana", email, hash, Rol.TIPSTER);
        assertEquals("Ana", usuario.nombre());
        assertEquals(email, usuario.email());
        assertEquals(hash, usuario.passwordHash());
        assertEquals(Rol.TIPSTER, usuario.rol());
        assertTrue(usuario.activo());
        assertTrue(usuario.fechaCreacion() != null);
    }

    @Test
    void debe_reconstruir_usuario_inactivo() {
        LocalDateTime fecha = LocalDateTime.of(2026, 8, 16, 10, 30);
        Usuario usuario = new Usuario(UUID.randomUUID(), "Ana", email, hash, Rol.SUPERADMIN, false, fecha);
        assertFalse(usuario.activo());
        assertEquals(Rol.SUPERADMIN, usuario.rol());
        assertEquals(fecha, usuario.fechaCreacion());
    }

    @Test
    void debe_rechazar_nombre_en_blanco() {
        assertThrows(DomainException.class, () -> new Usuario(" ", email, hash, Rol.CLIENTE));
    }

    @Test
    void debe_rechazar_email_nulo() {
        assertThrows(DomainException.class, () -> new Usuario("Ana", null, hash, Rol.CLIENTE));
    }

    @Test
    void debe_rechazar_password_hash_vacio() {
        assertThrows(DomainException.class, () -> new Usuario("Ana", email, "  ", Rol.CLIENTE));
    }

    @Test
    void debe_rechazar_rol_nulo() {
        assertThrows(DomainException.class, () -> new Usuario("Ana", email, hash, null));
    }

    @Test
    void debe_rechazar_id_nulo_en_reconstruccion() {
        assertThrows(DomainException.class, () -> new Usuario(null, "Ana", email, hash, Rol.CLIENTE, true,
                LocalDateTime.now()));
    }

    @Test
    void debe_rechazar_fecha_creacion_nula_en_reconstruccion() {
        assertThrows(DomainException.class, () -> new Usuario(UUID.randomUUID(), "Ana", email, hash, Rol.CLIENTE,
                true, null));
    }
}
