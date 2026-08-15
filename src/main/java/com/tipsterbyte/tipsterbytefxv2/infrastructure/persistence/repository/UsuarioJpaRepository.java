// ─────────────────────────────────────────────
// [QUÉ]: Repositorio Spring Data JPA de usuarios (tabla usuarios).
// [POR QUÉ]: Expone findByEmail (clave de login de CU-13) sobre UsuarioEntity.
// [RELACIONES]: Usado por UsuarioRepositoryJpaAdapter (FASE 11).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository;

import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {

    // [QUÉ]: Busca un usuario por email (único). Login de CU-13.
    Optional<UsuarioEntity> findByEmail(String email);
}
