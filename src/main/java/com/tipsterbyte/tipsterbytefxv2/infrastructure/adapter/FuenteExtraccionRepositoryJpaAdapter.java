// ─────────────────────────────────────────────
// [QUÉ]: Adapter JPA del puerto FuenteExtraccionRepository. Convierte entre el entity
//        FuenteExtraccion del dominio y FuenteExtraccionEntity de persistencia (mapper).
// [POR QUÉ]: Implementa el puerto definido en application sin que el dominio conozca
//            JPA. Las operaciones de lectura se marcan @Transactional(readOnly = true).
// [ALTERNATIVAS]: @DataJpaTest con mapeo manual; se descarta: la convención del
//                 proyecto (FASE 8) es adapters @Component + mapper explícito.
// [RELACIONES]: Implementa application.port.FuenteExtraccionRepository (CU-11).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.FuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.FuenteExtraccionEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.FuenteExtraccionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FuenteExtraccionRepositoryJpaAdapter implements FuenteExtraccionRepository {

    private final FuenteExtraccionJpaRepository jpaRepository;

    public FuenteExtraccionRepositoryJpaAdapter(FuenteExtraccionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FuenteExtraccion> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(this::toDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FuenteExtraccion> buscarPorTipo(TipoFuenteExtraccion tipo) {
        return jpaRepository.findByTipo(tipo).map(this::toDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FuenteExtraccion> buscarTodas() {
        return jpaRepository.findAll().stream().map(this::toDominio).toList();
    }

    @Override
    @Transactional
    public void guardar(FuenteExtraccion fuente) {
        jpaRepository.save(toEntity(fuente));
    }

    private FuenteExtraccion toDominio(FuenteExtraccionEntity entidad) {
        return new FuenteExtraccion(
                entidad.getId(), entidad.getNombre(), entidad.getTipo(), entidad.isActiva());
    }

    private FuenteExtraccionEntity toEntity(FuenteExtraccion fuente) {
        return new FuenteExtraccionEntity(
                fuente.id(), fuente.nombre(), fuente.tipo(), fuente.activa());
    }
}
