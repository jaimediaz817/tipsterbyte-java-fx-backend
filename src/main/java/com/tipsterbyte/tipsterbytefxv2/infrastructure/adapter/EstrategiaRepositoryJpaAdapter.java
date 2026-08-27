// ─────────────────────────────────────────────
// [QUÉ]: Adapter JPA del puerto EstrategiaRepository.
// [POR QUÉ]: HU-16 — persiste el aggregate Estrategia con criterios serializados en JSONB.
// [RELACIONES]: Implementa EstrategiaRepository, usa EstrategiaJpaRepository + EstrategiaEntity.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import tools.jackson.databind.ObjectMapper;
import com.tipsterbyte.tipsterbytefxv2.application.port.EstrategiaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Criterio;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Estrategia;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.EstrategiaEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.EstrategiaJpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class EstrategiaRepositoryJpaAdapter implements EstrategiaRepository {

    private final EstrategiaJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public EstrategiaRepositoryJpaAdapter(EstrategiaJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Estrategia> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(this::toDomainModel);
    }

    @Override
    public List<Estrategia> buscarPorTipsterId(UUID tipsterId) {
        return jpaRepository.findByTipsterIdOrderByCreatedAtDesc(tipsterId)
                .stream().map(this::toDomainModel).collect(Collectors.toList());
    }

    @Override
    public List<Estrategia> buscarActivas() {
        return jpaRepository.findByActivaTrue()
                .stream().map(this::toDomainModel).collect(Collectors.toList());
    }

    @Override
    public List<Estrategia> buscarActivasPorTipsterId(UUID tipsterId) {
        return jpaRepository.findByTipsterIdAndActivaTrue(tipsterId)
                .stream().map(this::toDomainModel).collect(Collectors.toList());
    }

    @Override
    public long contarActivasPorTipsterId(UUID tipsterId) {
        return jpaRepository.countByTipsterIdAndActivaTrue(tipsterId);
    }

    @Override
    public void guardar(Estrategia estrategia) {
        jpaRepository.save(toEntity(estrategia));
    }

    @Override
    public void eliminar(UUID id) {
        jpaRepository.deleteById(id);
    }

    private EstrategiaEntity toEntity(Estrategia domain) {
        String criteriosJson;
        try {
            criteriosJson = objectMapper.writeValueAsString(domain.criterios());
        } catch (Exception e) {
            criteriosJson = "[]";
        }
        return new EstrategiaEntity(
                domain.id(), domain.nombre(), domain.tipsterId(),
                domain.mercado().name(), domain.maxPartidos(),
                domain.confianzaMinima(), domain.activa(),
                criteriosJson,
                domain.ligaIds().toArray(UUID[]::new),
                domain.createdAt());
    }

    private Estrategia toDomainModel(EstrategiaEntity entity) {
        List<Criterio> criterios;
        try {
            List<CriterioDto> dtos = objectMapper.readValue(
                    entity.getCriteriosJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CriterioDto.class));
            criterios = dtos.stream().map(this::toCriterio).collect(Collectors.toList());
        } catch (Exception e) {
            criterios = new ArrayList<>();
        }

        List<UUID> ligaIds = entity.getLigaIds() != null
                ? Arrays.asList(entity.getLigaIds())
                : List.of();

        return new Estrategia(
                entity.getId(), entity.getNombre(), entity.getTipsterId(),
                Mercado.valueOf(entity.getMercado()), entity.getMaxPartidos(),
                entity.getConfianzaMinima(), entity.isActiva(),
                criterios, ligaIds, entity.getCreatedAt());
    }

    private Criterio toCriterio(CriterioDto dto) {
        return new Criterio(
                Criterio.FuenteCriterio.valueOf(dto.fuente),
                dto.campo,
                Criterio.OperadorCriterio.valueOf(dto.operador),
                dto.valor,
                Criterio.ReferenciaCriterio.valueOf(dto.referencia),
                new BigDecimal(dto.peso),
                dto.orden);
    }

    // DTO interno para deserialización JSONB → dominio
    static class CriterioDto {
        public String fuente;
        public String campo;
        public String operador;
        public String valor;
        public String referencia;
        public String peso;
        public Integer orden;
    }
}
