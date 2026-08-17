// ─────────────────────────────────────────────
// [QUÉ]: Adapter JPA del puerto PaisInteresRepository. Convierte entre el entity
//        PaisInteres del dominio y PaisInteresEntity de persistencia (mapper).
// [POR QUÉ]: Implementa el puerto definido en application sin que el dominio conozca
//            JPA. Las lecturas se marcan @Transactional(readOnly = true).
// [ALTERNATIVAS]: Consultas directas en el caso de uso; se descartan porque la
//                 persistencia siempre pasa por un adapter del puerto.
// [RELACIONES]: Implementa application.port.PaisInteresRepository (CU-14); CU-10
//               lee la lista ordenada por prioridad para poblar con preferencia.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.PaisInteresRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PaisInteres;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.PaisInteresEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.PaisInteresJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PaisInteresRepositoryJpaAdapter implements PaisInteresRepository {

    private final PaisInteresJpaRepository jpaRepository;

    public PaisInteresRepositoryJpaAdapter(PaisInteresJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaisInteres> buscarPorIsoAlpha2(String isoAlpha2) {
        return jpaRepository.findByIsoAlpha2(isoAlpha2).map(this::toDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaisInteres> listarPorPrioridad() {
        return jpaRepository.findAllByOrderByPrioridadAsc().stream()
                .map(this::toDominio)
                .toList();
    }

    @Override
    @Transactional
    public void guardar(PaisInteres paisInteres) {
        jpaRepository.save(new PaisInteresEntity(
                paisInteres.id(), paisInteres.isoAlpha2(), paisInteres.nombre(), paisInteres.prioridad()));
    }

    @Override
    @Transactional
    public void eliminar(String isoAlpha2) {
        jpaRepository.deleteByIsoAlpha2(isoAlpha2);
    }

    private PaisInteres toDominio(PaisInteresEntity entidad) {
        return new PaisInteres(
                entidad.getId(), entidad.getIsoAlpha2(), entidad.getNombre(), entidad.getPrioridad());
    }
}