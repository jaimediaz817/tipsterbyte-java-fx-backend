// ─────────────────────────────────────────────
// [QUÉ]: Adapter JPA del puerto PaisRepository. Convierte entre el entity Pais del
//        dominio y PaisEntity de persistencia (mapper).
// [POR QUÉ]: Implementa el puerto definido en application sin que el dominio conozca
//            JPA. Las operaciones de lectura se marcan @Transactional(readOnly = true).
// [ALTERNATIVAS]: @DataJpaTest con mapeo manual; se descarta: la convención del
//                 proyecto (FASE 8) es adapters @Component + mapper explícito.
// [RELACIONES]: Implementa application.port.PaisRepository (CU-10).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pais;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.PaisEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.PaisJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaisRepositoryJpaAdapter implements PaisRepository {

    private final PaisJpaRepository jpaRepository;

    public PaisRepositoryJpaAdapter(PaisJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Pais> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(this::toDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Pais> buscarPorIsoAlpha2(String isoAlpha2) {
        return jpaRepository.findByIsoAlpha2(isoAlpha2).map(this::toDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pais> buscarTodos() {
        return jpaRepository.findAll().stream().map(this::toDominio).toList();
    }

    @Override
    @Transactional
    public void guardar(Pais pais) {
        jpaRepository.save(toEntity(pais));
    }

    private Pais toDominio(PaisEntity entidad) {
        return new Pais(
                entidad.getId(), entidad.getNombre(), entidad.getIsoAlpha2(),
                entidad.getContinente(), entidad.getCode(), entidad.getHref(), entidad.isMapeado());
    }

    private PaisEntity toEntity(Pais pais) {
        return new PaisEntity(
                pais.id(), pais.nombre(), pais.isoAlpha2(), pais.continente(),
                pais.code(), pais.href(), pais.mapeado());
    }
}