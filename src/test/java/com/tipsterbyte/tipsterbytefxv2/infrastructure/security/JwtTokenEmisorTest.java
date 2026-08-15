// ─────────────────────────────────────────────
// [QUÉ]: Test del adapter JwtTokenEmisor: emite y lee tokens JWT HS256.
// [POR QUÉ]: Verifica la firma/claims del token que autentica cada request (CU-13).
// [RELACIONES]: JwtTokenEmisor → TokenEmisor (application.port).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.security;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Email;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Rol;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenEmisorTest {

    private static final String SECRETO =
            "tipsterbyte-fx-v2-clave-secreta-dev-muy-larga-para-hs256-0123456789abcdef";

    private JwtTokenEmisor emisor;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        emisor = new JwtTokenEmisor(SECRETO, 86400000);
        usuario = new Usuario(
                UUID.randomUUID(), "Ana", new Email("ana@example.com"),
                "hash-bcrypt", Rol.ADMIN, true);
    }

    @Test
    void debe_emitir_token_y_recuperar_id_del_usuario() {
        String token = emisor.emitirToken(usuario);
        assertEquals(usuario.id().toString(), emisor.extraerIdUsuario(token));
    }

    @Test
    void debe_rechazar_token_con_firma_invalida() {
        JwtTokenEmisor otroEmisor = new JwtTokenEmisor(
                "otra-clave-secreta-distinta-para-hs256-de-256-bits-abcdef", 86400000);
        String token = otroEmisor.emitirToken(usuario);
        assertThrows(Exception.class, () -> emisor.extraerIdUsuario(token));
    }

    @Test
    void debe_rechazar_token_no_es_jwt() {
        assertThrows(Exception.class, () -> emisor.extraerIdUsuario("no-es-un-jwt"));
    }
}
