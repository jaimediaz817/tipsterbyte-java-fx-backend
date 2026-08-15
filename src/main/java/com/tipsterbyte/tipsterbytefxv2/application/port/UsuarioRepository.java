// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia de Usuario: buscar por email (clave de login) y guardar.
// [POR QUÉ]: La capa application depende de esta interfaz (Dependency Rule); la
//            implementa UsuarioRepositoryJpaAdapter en infrastructure (FASE 11).
// [ALTERNATIVAS]: Acceder directo a Spring Data; se descarta porque acoplaría
//                 application a JPA.
// [RELACIONES]: CU-12 (registro), CU-13 (login) → UsuarioRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Email;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Usuario;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository {

    // [QUÉ]: Busca un usuario por su email (único). Login de CU-13.
    Optional<Usuario> buscarPorEmail(Email email);

    // [QUÉ]: Busca un usuario por su id. Autorización en requests autenticados.
    Optional<Usuario> buscarPorId(UUID id);

    // [QUÉ]: Persiste un usuario (registro de CU-12).
    void guardar(Usuario usuario);
}
