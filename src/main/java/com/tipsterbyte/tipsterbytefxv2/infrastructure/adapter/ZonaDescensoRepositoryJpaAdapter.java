// ─────────────────────────────────────────────
// [QUÉ]: Adapter JPA del puerto ZonaDescensoRepository.
// [POR QUÉ]: HU-16 AC5 — persiste la configuración de zona de descenso por temporada.
// [RELACIONES]: Implementa ZonaDescensoRepository, usa ZonaDescensoJpaRepository + ZonaDescensoEntity.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.ZonaDescensoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.ZonaDescenso;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.ZonaDescensoEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.ZonaDescensoJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ZonaDescensoRepositoryJpaAdapter implements ZonaDescensoRepository {

    private final ZonaDescensoJpaRepository jpaRepository;

    public ZonaDescensoRepositoryJpaAdapter(ZonaDescensoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ZonaDescenso> buscarPorTemporadaId(UUID temporadaId) {
        return jpaRepository.findByTemporadaId(temporadaId).map(this::toDomainModel);
    }

    @Override
    public void guardar(ZonaDescenso zonaDescenso) {
        jpaRepository.save(toEntity(zonaDescenso));
    }

    private ZonaDescenso toDomainModel(ZonaDescensoEntity entity) {
        return new ZonaDescenso(entity.getId(), entity.getTemporadaId(),
                entity.getPosicionDescenso(), entity.getDescripcion());
    }

    private ZonaDescensoEntity toEntity(ZonaDescenso domain) {
        return new ZonaDescensoEntity(domain.id(), domain.temporadaId(),
                domain.posicionDescenso(), domain.descripcion());
    }
}
