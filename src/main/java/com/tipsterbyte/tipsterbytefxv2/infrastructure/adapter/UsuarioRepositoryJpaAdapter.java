// ─────────────────────────────────────────────
// [QUÉ]: Adapter JPA del puerto UsuarioRepository. Convierte entre Usuario del
//        dominio y UsuarioEntity de persistencia (mapper).
// [POR QUÉ]: Implementa el puerto definido en application sin que el dominio conozca
//            JPA. El email es la clave de búsqueda del login (CU-13).
// [ALTERNATIVAS]: @DataJpaTest con mapeo manual; se descarta: la convención del
//                 proyecto (FASE 8) es adapters @Component + mapper explícito.
// [RELACIONES]: Implementa application.port.UsuarioRepository (CU-12/CU-13).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.UsuarioRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Email;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Usuario;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.UsuarioEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class UsuarioRepositoryJpaAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository jpaRepository;

    public UsuarioRepositoryJpaAdapter(UsuarioJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(Email email) {
        return jpaRepository.findByEmail(email.direccion()).map(this::toDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(this::toDominio);
    }

    @Override
    @Transactional
    public void guardar(Usuario usuario) {
        jpaRepository.save(toEntity(usuario));
    }

    private Usuario toDominio(UsuarioEntity entidad) {
        return new Usuario(
                entidad.getId(), entidad.getNombre(), new Email(entidad.getEmail()),
                entidad.getPasswordHash(), entidad.getRol(), entidad.isActivo(), entidad.getFechaCreacion());
    }

    private UsuarioEntity toEntity(Usuario usuario) {
        return new UsuarioEntity(
                usuario.id(), usuario.nombre(), usuario.email().direccion(),
                usuario.passwordHash(), usuario.rol(), usuario.activo(), usuario.fechaCreacion());
    }
}
