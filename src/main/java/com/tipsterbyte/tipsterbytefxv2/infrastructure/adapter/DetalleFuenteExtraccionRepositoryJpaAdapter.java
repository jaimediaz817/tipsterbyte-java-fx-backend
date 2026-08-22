// ─────────────────────────────────────────────
// [QUÉ]: Adapter JPA del puerto DetalleFuenteExtraccionRepository. Convierte entre el
//        entity DetalleFuenteExtraccion del dominio y DetalleFuenteExtraccionEntity de
//        persistencia (mapper).
// [POR QUÉ]: Implementa el puerto definido en application sin que el dominio conozca
//            JPA. Resuelve la URL de una fuente por temporada (o por liga vía JOIN)
//            para los adapters HTTP y persiste los detalles creados por CU-04/CU-11.
//            El detalle referencia su temporada (Bridge Fix Torneos/Temporadas): al
//            guardar se resuelve la TemporadaEntity por id (referencia perezosa).
// [ALTERNATIVAS]: @DataJpaTest con mapeo manual; se descarta: la convención del
//                 proyecto (FASE 8) es adapters @Component + mapper explícito.
// [RELACIONES]: Implementa application.port.DetalleFuenteExtraccionRepository (CU-04, CU-11);
//               consultado por los adapters de fuentes (CU-01/02/03). Refiere
//               TemporadaJpaRepository para resolver la FK temporada_id.
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
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.TemporadaJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DetalleFuenteExtraccionRepositoryJpaAdapter implements DetalleFuenteExtraccionRepository {

    private final DetalleFuenteExtraccionJpaRepository jpaRepository;
    private final FuenteExtraccionJpaRepository fuenteJpaRepository;
    private final TemporadaJpaRepository temporadaJpaRepository;

    public DetalleFuenteExtraccionRepositoryJpaAdapter(DetalleFuenteExtraccionJpaRepository jpaRepository,
                                                       FuenteExtraccionJpaRepository fuenteJpaRepository,
                                                       TemporadaJpaRepository temporadaJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.fuenteJpaRepository = fuenteJpaRepository;
        this.temporadaJpaRepository = temporadaJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DetalleFuenteExtraccion> buscarPorLigaYTipo(UUID ligaId, TipoFuenteExtraccion tipo) {
        return jpaRepository.findByLigaIdAndTipo(ligaId, tipo).map(this::toDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DetalleFuenteExtraccion> buscarPorTemporadaYTipo(UUID temporadaId, TipoFuenteExtraccion tipo) {
        return jpaRepository.findByTemporada_IdAndTipo(temporadaId, tipo).map(this::toDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DetalleFuenteExtraccion> buscarPorLiga(UUID ligaId) {
        return jpaRepository.findByLigaId(ligaId).stream().map(this::toDominio).toList();
    }

    @Override
    @Transactional
    public void guardar(DetalleFuenteExtraccion detalle) {
        // Referencia perezosa a la temporada: no requiere SELECT previo (la FK basta).
        jpaRepository.save(new DetalleFuenteExtraccionEntity(
                detalle.id(), temporadaJpaRepository.getReferenceById(detalle.temporadaId()),
                fuenteJpaRepository.getReferenceById(detalle.fuente().id()),
                detalle.tipo(), detalle.url(), detalle.activa()));
    }

    private DetalleFuenteExtraccion toDominio(DetalleFuenteExtraccionEntity entidad) {
        FuenteExtraccionEntity fuenteEntity = entidad.getFuente();
        return new DetalleFuenteExtraccion(
                entidad.getId(), entidad.getTemporada().getId(),
                new FuenteExtraccion(
                        fuenteEntity.getId(), fuenteEntity.getNombre(),
                        fuenteEntity.getTipo(), fuenteEntity.isActiva()),
                entidad.getUrl(), entidad.isActiva());
    }
}
