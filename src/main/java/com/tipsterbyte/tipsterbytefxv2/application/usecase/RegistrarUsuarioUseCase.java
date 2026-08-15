// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-12 (FASE 11): registra un nuevo usuario autenticable con
//        contraseña hasheada (BCrypt) y rol.
// [POR QUÉ]: Centraliza las reglas de registro: email único, hash de la contraseña
//            (nunca se guarda en claro) y persistencia. Se lanza DomainException si
//            el email ya está registrado.
// [ALTERNATIVAS]: Dejar el registro en el controller; se descarta porque duplicaría
//                 reglas y rompería la Dependency Rule.
// [RELACIONES]: HU nueva → CU-12 → UsuarioRepository + PasswordHasher.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarUsuarioComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.PasswordHasher;
import com.tipsterbyte.tipsterbytefxv2.application.port.UsuarioRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Email;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Usuario;

import java.util.List;
import java.util.UUID;

public final class RegistrarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordHasher passwordHasher;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public RegistrarUsuarioUseCase(UsuarioRepository usuarioRepository, PasswordHasher passwordHasher) {
        this.usuarioRepository = usuarioRepository;
        this.passwordHasher = passwordHasher;
    }

    // [QUÉ]: Ejecuta CU-12: valida unicidad de email, hashea la contraseña y persiste.
    //        Devuelve el id del usuario creado.
    public UUID ejecutar(RegistrarUsuarioComando comando) {
        Email email = new Email(comando.email());
        if (usuarioRepository.buscarPorEmail(email).isPresent()) {
            throw new DomainException("Ya existe un usuario con el email: " + comando.email());
        }
        String hash = passwordHasher.hash(comando.password());
        Usuario usuario = new Usuario(comando.nombre(), email, hash, comando.rol());
        usuarioRepository.guardar(usuario);
        return usuario.id();
    }
}
