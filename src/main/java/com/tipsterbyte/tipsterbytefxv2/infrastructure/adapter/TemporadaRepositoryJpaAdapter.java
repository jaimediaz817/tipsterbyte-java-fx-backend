// ─────────────────────────────────────────────
// [QUÉ]: Adapter JPA del puerto TemporadaRepository. Convierte entre el entity
//        Temporada del dominio y TemporadaEntity de persistencia (mapper).
// [POR QUÉ]: Implementa el puerto definido en application sin que el dominio conozca
//            JPA. Las lecturas se marcan @Transactional(readOnly = true).
// [ALTERNATIVAS]: Consultas directas en el caso de uso; se descartan porque la
//                 persistencia siempre pasa por un adapter del puerto.
// [RELACIONES]: Implementa application.port.TemporadaRepository; consumido por
//               CU-10, CU-04 y adapters de fuentes.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.TemporadaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoTemporada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.TemporadaEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.LigaJpaRepository;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.TemporadaJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TemporadaRepositoryJpaAdapter implements TemporadaRepository {

    private final TemporadaJpaRepository jpaRepository;
    private final LigaJpaRepository ligaJpaRepository;

    public TemporadaRepositoryJpaAdapter(TemporadaJpaRepository jpaRepository,
                                         LigaJpaRepository ligaJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.ligaJpaRepository = ligaJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Temporada> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(this::toDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Temporada> buscarPorLigaId(UUID ligaId) {
        return jpaRepository.findByLigaId(ligaId).stream()
                .map(this::toDominio)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Temporada> buscarPorLigaIdYNombre(UUID ligaId, String nombre) {
        return jpaRepository.findByLigaIdAndNombre(ligaId, nombre).map(this::toDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Temporada> buscarActivaPorLigaId(UUID ligaId) {
        return jpaRepository.findByLigaIdAndEstado(ligaId, EstadoTemporada.ACTIVA).map(this::toDominio);
    }

    @Override
    @Transactional
    public void guardar(Temporada temporada) {
        // Referencia perezosa a la liga: no requiere SELECT previo (la FK basta).
        jpaRepository.save(new TemporadaEntity(
                temporada.id(), ligaJpaRepository.getReferenceById(temporada.ligaId()),
                temporada.nombre(), temporada.semestre(),
                temporada.anioInicio(), temporada.anioFin(), temporada.estado()));
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        jpaRepository.deleteById(id);
    }

    private Temporada toDominio(TemporadaEntity entidad) {
        return new Temporada(
                entidad.getId(), entidad.getLiga().getId(), entidad.getNombre(), entidad.getSemestre(),
                entidad.getAnioInicio(), entidad.getAnioFin(), entidad.getEstado());
    }
}
