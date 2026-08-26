package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.EquiposAliasRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EquiposAlias;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.EquiposAliasEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.EquiposAliasJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class EquiposAliasRepositoryJpaAdapter implements EquiposAliasRepository {

    private final EquiposAliasJpaRepository jpaRepository;

    public EquiposAliasRepositoryJpaAdapter(EquiposAliasJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<EquiposAlias> buscarPorNombreExternoYTemporada(String nombreExterno, UUID temporadaId) {
        return jpaRepository.findByNombreExternoIgnoreCaseAndTemporadaId(nombreExterno, temporadaId)
                .stream().map(EquiposAliasEntity::toDomainModel).collect(Collectors.toList());
    }

    @Override
    public List<EquiposAlias> buscarPorTemporadaYFuente(UUID temporadaId, TipoFuenteExtraccion fuenteTipo) {
        return jpaRepository.findByTemporadaIdAndFuenteTipo(temporadaId, fuenteTipo)
                .stream().map(EquiposAliasEntity::toDomainModel).collect(Collectors.toList());
    }

    @Override
    public EquiposAlias guardar(EquiposAlias alias) {
        EquiposAliasEntity entity = EquiposAliasEntity.fromDomainModel(alias);
        EquiposAliasEntity saved = jpaRepository.save(entity);
        return saved.toDomainModel();
    }

    @Override
    public void eliminarPorId(UUID id) {
        jpaRepository.deleteById(id);
    }
}
