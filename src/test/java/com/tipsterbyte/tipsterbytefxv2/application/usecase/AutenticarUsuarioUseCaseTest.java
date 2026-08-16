// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de CU-13 (login): verificación de credenciales y emisión JWT.
// [POR QUÉ]: Verifica la orquestación con puertos mockeados y el mensaje genérico de
//            credenciales inválidas (seguridad: no revela si falló email o password).
// [RELACIONES]: CU-13 → AutenticarUsuarioUseCase → UsuarioRepository + PasswordHasher
//               + TokenEmisor.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.AutenticarUsuarioComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.AutenticacionResultado;
import com.tipsterbyte.tipsterbytefxv2.application.port.PasswordHasher;
import com.tipsterbyte.tipsterbytefxv2.application.port.TokenEmisor;
import com.tipsterbyte.tipsterbytefxv2.application.port.UsuarioRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Email;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Rol;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticarUsuarioUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private TokenEmisor tokenEmisor;

    private AutenticarUsuarioUseCase casoDeUso;

    private final Usuario usuario = new Usuario(
            UUID.randomUUID(), "Ana", new Email("ana@example.com"), "hash-bcrypt", Rol.CLIENTE, true,
            LocalDateTime.now());

    @BeforeEach
    void setUp() {
        casoDeUso = new AutenticarUsuarioUseCase(usuarioRepository, passwordHasher, tokenEmisor);
    }

    @Test
    void debe_autenticar_y_emitir_token() {
        when(usuarioRepository.buscarPorEmail(any(Email.class))).thenReturn(Optional.of(usuario));
        when(passwordHasher.verificar("clave123", "hash-bcrypt")).thenReturn(true);
        when(tokenEmisor.emitirToken(usuario)).thenReturn("jwt.token");

        AutenticacionResultado resultado = casoDeUso.ejecutar(
                new AutenticarUsuarioComando("ana@example.com", "clave123"));

        assertEquals(usuario.id(), resultado.usuarioId());
        assertEquals("jwt.token", resultado.token());
        assertEquals(Rol.CLIENTE, resultado.rol());
        verify(tokenEmisor).emitirToken(usuario);
    }

    @Test
    void debe_rechazar_credenciales_si_email_no_existe() {
        when(usuarioRepository.buscarPorEmail(any(Email.class))).thenReturn(Optional.empty());

        DomainException ex = assertThrows(DomainException.class, () -> casoDeUso.ejecutar(
                new AutenticarUsuarioComando("ana@example.com", "clave123")));

        assertEquals("Credenciales inválidas", ex.getMessage());
        verify(passwordHasher, never()).verificar(anyString(), anyString());
    }

    @Test
    void debe_rechazar_credenciales_si_password_no_coincide() {
        when(usuarioRepository.buscarPorEmail(any(Email.class))).thenReturn(Optional.of(usuario));
        when(passwordHasher.verificar("clave-mala", "hash-bcrypt")).thenReturn(false);

        DomainException ex = assertThrows(DomainException.class, () -> casoDeUso.ejecutar(
                new AutenticarUsuarioComando("ana@example.com", "clave-mala")));

        assertEquals("Credenciales inválidas", ex.getMessage());
        verify(tokenEmisor, never()).emitirToken(any());
    }

    @Test
    void debe_rechazar_credenciales_si_usuario_inactivo() {
        Usuario inactivo = new Usuario(
                UUID.randomUUID(), "Ana", new Email("ana@example.com"), "hash", Rol.CLIENTE, false,
                LocalDateTime.now());
        when(usuarioRepository.buscarPorEmail(any(Email.class))).thenReturn(Optional.of(inactivo));

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(
                new AutenticarUsuarioComando("ana@example.com", "clave123")));

        verify(tokenEmisor, never()).emitirToken(any());
    }

    @Test
    void debe_rechazar_email_invalido_en_login() {
        assertThrows(DomainException.class, () -> new Email("mal"));
    }
}
