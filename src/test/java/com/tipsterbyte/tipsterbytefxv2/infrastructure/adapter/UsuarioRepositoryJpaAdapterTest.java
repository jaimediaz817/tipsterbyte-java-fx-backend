// ─────────────────────────────────────────────
// [QUÉ]: Test de integración de UsuarioRepositoryJpaAdapter contra PostgreSQL
//        (Testcontainers): ciclo guardar → recuperar por email y por id.
// [POR QUÉ]: Verifica el mapeo del usuario autenticable (CU-12/CU-13) con su email
//            único y hash de contraseña en la BD real.
// [RELACIONES]: CU-12/CU-13 → UsuarioRepository → UsuarioRepositoryJpaAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.UsuarioRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Email;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Rol;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Usuario;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioRepositoryJpaAdapterTest extends AbstractRepositoryJpaAdapterTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioJpaRepository jpaRepository;

    @BeforeEach
    void limpiar() {
        jpaRepository.deleteAll();
    }

    @Test
    void debe_guardar_y_recuperar_usuario_por_email() {
        Usuario usuario = new Usuario("Ana", new Email("ana@example.com"), "hash-bcrypt", Rol.TIPSTER);

        usuarioRepository.guardar(usuario);

        Usuario recuperado = usuarioRepository.buscarPorEmail(new Email("ana@example.com")).orElseThrow();
        assertEquals("Ana", recuperado.nombre());
        assertEquals("hash-bcrypt", recuperado.passwordHash());
        assertEquals(Rol.TIPSTER, recuperado.rol());
        assertTrue(recuperado.activo());
    }

    @Test
    void debe_recuperar_usuario_por_id() {
        Usuario usuario = new Usuario("Luis", new Email("luis@example.com"), "hash", Rol.CLIENTE);
        usuarioRepository.guardar(usuario);

        Usuario recuperado = usuarioRepository.buscarPorId(usuario.id()).orElseThrow();
        assertEquals("Luis", recuperado.nombre());
        assertEquals(Rol.CLIENTE, recuperado.rol());
    }

    @Test
    void debe_devolver_vacio_si_email_no_existe() {
        assertTrue(usuarioRepository.buscarPorEmail(new Email("nadie@example.com")).isEmpty());
    }
}
