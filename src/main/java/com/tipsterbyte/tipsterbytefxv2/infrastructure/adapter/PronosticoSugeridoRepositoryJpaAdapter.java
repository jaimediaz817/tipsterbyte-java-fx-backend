// ─────────────────────────────────────────────
// [QUÉ]: Adapter JPA del puerto PronosticoSugeridoRepository.
// [POR QUÉ]: HU-16 AC10/13 — persiste y consulta pronósticos sugeridos.
// [RELACIONES]: Implementa PronosticoSugeridoRepository, usa PronosticoSugeridoJpaRepository + Entity.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.PronosticoSugeridoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PronosticoSugerido;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.PronosticoSugeridoEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.PronosticoSugeridoJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class PronosticoSugeridoRepositoryJpaAdapter implements PronosticoSugeridoRepository {

    private final PronosticoSugeridoJpaRepository jpaRepository;

    public PronosticoSugeridoRepositoryJpaAdapter(PronosticoSugeridoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<PronosticoSugerido> buscarPorEstrategiaId(UUID estrategiaId) {
        return jpaRepository.findByEstrategiaIdOrderByCreatedAtDesc(estrategiaId)
                .stream().map(this::toDomainModel).collect(Collectors.toList());
    }

    @Override
    public void guardar(PronosticoSugerido sugerido) {
        jpaRepository.save(toEntity(sugerido));
    }

    @Override
    public void guardarLote(List<PronosticoSugerido> sugeridos) {
        jpaRepository.saveAll(sugeridos.stream().map(this::toEntity).collect(Collectors.toList()));
    }

    @Override
    public void eliminarPorEstrategiaId(UUID estrategiaId) {
        jpaRepository.deleteByEstrategiaId(estrategiaId);
    }

    private PronosticoSugerido toDomainModel(PronosticoSugeridoEntity entity) {
        return new PronosticoSugerido(entity.getId(), entity.getEstrategiaId(),
                entity.getPartidoId(), entity.getScore(),
                entity.getCriteriosCumplidos(), entity.getCriteriosFallidos(),
                entity.getCreatedAt());
    }

    private PronosticoSugeridoEntity toEntity(PronosticoSugerido domain) {
        return new PronosticoSugeridoEntity(domain.id(), domain.estrategiaId(),
                domain.partidoId(), domain.score(),
                domain.criteriosCumplidos(), domain.criteriosFallidos(),
                domain.createdAt());
    }
}
