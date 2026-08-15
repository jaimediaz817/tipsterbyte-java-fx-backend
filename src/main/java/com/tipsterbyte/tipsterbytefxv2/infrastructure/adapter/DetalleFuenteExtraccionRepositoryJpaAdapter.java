// ─────────────────────────────────────────────
// [QUÉ]: Adapter JPA del puerto DetalleFuenteExtraccionRepository. Convierte entre el
//        entity DetalleFuenteExtraccion del dominio y DetalleFuenteExtraccionEntity de
//        persistencia (mapper).
// [POR QUÉ]: Implementa el puerto definido en application sin que el dominio conozca
//            JPA. Resuelve la URL de una fuente por liga para los adapters HTTP y
//            persiste los detalles creados por CU-04/CU-11.
// [ALTERNATIVAS]: @DataJpaTest con mapeo manual; se descarta: la convención del
//                 proyecto (FASE 8) es adapters @Component + mapper explícito.
// [RELACIONES]: Implementa application.port.DetalleFuenteExtraccionRepository (CU-04, CU-11);
//               consultado por los adapters de fuentes (CU-01/02/03).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.DetalleFuenteExtraccionEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.FuenteExtraccionEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.DetalleFuenteExtraccionJpaRepository;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.FuenteExtraccionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DetalleFuenteExtraccionRepositoryJpaAdapter implements DetalleFuenteExtraccionRepository {

    private final DetalleFuenteExtraccionJpaRepository jpaRepository;
    private final FuenteExtraccionJpaRepository fuenteJpaRepository;

    public DetalleFuenteExtraccionRepositoryJpaAdapter(DetalleFuenteExtraccionJpaRepository jpaRepository,
                                                       FuenteExtraccionJpaRepository fuenteJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.fuenteJpaRepository = fuenteJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DetalleFuenteExtraccion> buscarPorLigaYTipo(UUID ligaId, TipoFuenteExtraccion tipo) {
        return jpaRepository.findByLigaIdAndTipo(ligaId, tipo).map(this::toDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DetalleFuenteExtraccion> buscarPorLiga(UUID ligaId) {
        return jpaRepository.findByLigaId(ligaId).stream().map(this::toDominio).toList();
    }

    @Override
    @Transactional
    public void guardar(DetalleFuenteExtraccion detalle) {
        jpaRepository.save(new DetalleFuenteExtraccionEntity(
                detalle.id(), detalle.ligaId(),
                fuenteJpaRepository.getReferenceById(detalle.fuente().id()),
                detalle.tipo(), detalle.url(), detalle.activa()));
    }

    private DetalleFuenteExtraccion toDominio(DetalleFuenteExtraccionEntity entidad) {
        FuenteExtraccionEntity fuenteEntity = entidad.getFuente();
        return new DetalleFuenteExtraccion(
                entidad.getId(), entidad.getLigaId(),
                new FuenteExtraccion(
                        fuenteEntity.getId(), fuenteEntity.getNombre(),
                        fuenteEntity.getTipo(), fuenteEntity.isActiva()),
                entidad.getUrl(), entidad.isActiva());
    }
}
