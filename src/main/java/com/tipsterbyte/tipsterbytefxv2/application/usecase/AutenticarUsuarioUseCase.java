// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-13 (FASE 11): autentica un usuario por email+contraseña y
//        emite el token JWT de sesión.
// [POR QUÉ]: Verifica la contraseña contra el hash almacenado (BCrypt) y, si es
//            válida, delega la emisión del token al puerto TokenEmisor. Un email
//            inexistente o contraseña inválida producen DomainException genérica
//            para no revelar qué campo falló (seguridad).
// [ALTERNATIVAS]: Devolver la excepción específica (email no existe vs password
//                 incorrecta); se descarta porque facilita el enumerado de usuarios.
// [RELACIONES]: CU-13 → UsuarioRepository + PasswordHasher + TokenEmisor.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.AutenticarUsuarioComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.AutenticacionResultado;
import com.tipsterbyte.tipsterbytefxv2.application.port.PasswordHasher;
import com.tipsterbyte.tipsterbytefxv2.application.port.TokenEmisor;
import com.tipsterbyte.tipsterbytefxv2.application.port.UsuarioRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Email;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Usuario;

public final class AutenticarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordHasher passwordHasher;
    private final TokenEmisor tokenEmisor;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public AutenticarUsuarioUseCase(UsuarioRepository usuarioRepository,
                                    PasswordHasher passwordHasher,
                                    TokenEmisor tokenEmisor) {
        this.usuarioRepository = usuarioRepository;
        this.passwordHasher = passwordHasher;
        this.tokenEmisor = tokenEmisor;
    }

    // [QUÉ]: Ejecuta CU-13: busca por email, verifica la contraseña y emite el JWT.
    public AutenticacionResultado ejecutar(AutenticarUsuarioComando comando) {
        Usuario usuario = usuarioRepository.buscarPorEmail(new Email(comando.email()))
                .orElseThrow(() -> new DomainException("Credenciales inválidas"));
        if (!usuario.activo() || !passwordHasher.verificar(comando.password(), usuario.passwordHash())) {
            throw new DomainException("Credenciales inválidas");
        }
        String token = tokenEmisor.emitirToken(usuario);
        return new AutenticacionResultado(
                usuario.id(), usuario.nombre(), usuario.email().direccion(), usuario.rol(), token);
    }
}
