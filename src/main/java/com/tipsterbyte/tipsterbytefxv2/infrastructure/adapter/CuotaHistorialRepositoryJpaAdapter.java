package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.CuotaHistorialRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.CuotaHistorial;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.CuotaHistorialEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.CuotaHistorialJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class CuotaHistorialRepositoryJpaAdapter implements CuotaHistorialRepository {

    private final CuotaHistorialJpaRepository jpaRepository;

    public CuotaHistorialRepositoryJpaAdapter(CuotaHistorialJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void guardar(CuotaHistorial cuota) {
        jpaRepository.save(CuotaHistorialEntity.fromDomainModel(cuota));
    }

    @Override
    public void guardarLote(List<CuotaHistorial> cuotas) {
        List<CuotaHistorialEntity> entities = cuotas.stream()
                .map(CuotaHistorialEntity::fromDomainModel)
                .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }

    @Override
    public List<CuotaHistorial> buscarPorPartidoYRango(UUID partidoId, Instant desde, Instant hasta) {
        return jpaRepository.findByPartidoIdAndCapturadaEnBetweenOrderByCapturadaEnAsc(partidoId, desde, hasta)
                .stream().map(CuotaHistorialEntity::toDomainModel).collect(Collectors.toList());
    }

    @Override
    public List<CuotaHistorial> buscarPorPartidosYRango(List<UUID> partidoIds, Instant desde, Instant hasta) {
        if (partidoIds == null || partidoIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByPartidoIdInAndCapturadaEnBetweenOrderByPartidoIdAscCapturadaEnAsc(
                        partidoIds, desde, hasta)
                .stream().map(CuotaHistorialEntity::toDomainModel).collect(Collectors.toList());
    }
}
