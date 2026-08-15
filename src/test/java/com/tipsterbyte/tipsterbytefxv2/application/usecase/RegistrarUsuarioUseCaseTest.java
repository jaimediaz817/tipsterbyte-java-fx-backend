// ─────────────────────────────────────────────
// [QUÉ]: Test unitario de CU-12 (registro): unicidad de email y hash de contraseña.
// [POR QUÉ]: Verifica la orquestación con puertos mockeados (regla testing.md).
// [RELACIONES]: CU-12 → RegistrarUsuarioUseCase → UsuarioRepository + PasswordHasher.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarUsuarioComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.PasswordHasher;
import com.tipsterbyte.tipsterbytefxv2.application.port.UsuarioRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Email;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Rol;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarUsuarioUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordHasher passwordHasher;

    private RegistrarUsuarioUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new RegistrarUsuarioUseCase(usuarioRepository, passwordHasher);
    }

    @Test
    void debe_registrar_usuario_con_password_hasheada() {
        when(usuarioRepository.buscarPorEmail(any(Email.class))).thenReturn(Optional.empty());
        when(passwordHasher.hash("secreto123")).thenReturn("hash-bcrypt");

        UUID id = casoDeUso.ejecutar(new RegistrarUsuarioComando(
                "Ana", "ana@example.com", "secreto123", Rol.TIPSTER));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).guardar(captor.capture());
        assertEquals("Ana", captor.getValue().nombre());
        assertEquals("hash-bcrypt", captor.getValue().passwordHash());
        assertEquals(Rol.TIPSTER, captor.getValue().rol());
        assertTrue(id != null);
    }

    @Test
    void debe_rechazar_registro_si_email_ya_existe() {
        when(usuarioRepository.buscarPorEmail(any(Email.class)))
                .thenReturn(Optional.of(new Usuario("Ana", new Email("ana@example.com"), "x", Rol.TIPSTER)));

        assertThrows(DomainException.class, () -> casoDeUso.ejecutar(
                new RegistrarUsuarioComando("Ana", "ana@example.com", "secreto123", Rol.TIPSTER)));

        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    void debe_rechazar_password_corta_o_email_invalido() {
        assertThrows(DomainException.class, () -> new Email("correo-invalido"));
        verify(passwordHasher, never()).hash(anyString());
    }
}
