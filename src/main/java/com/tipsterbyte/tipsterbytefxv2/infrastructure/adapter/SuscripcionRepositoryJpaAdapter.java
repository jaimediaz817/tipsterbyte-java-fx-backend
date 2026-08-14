// ─────────────────────────────────────────────
// [QUÉ]: Adapter JPA del puerto SuscripcionRepository. Convierte entre el aggregate
//        Suscripcion del dominio y SuscripcionEntity de persistencia (mapper).
// [POR QUÉ]: Implementa el puerto definido en application sin que el dominio conozca
//            JPA. El plan se guarda como columnas planas; cliente y tipster se
//            referencian por id.
// [ALTERNATIVAS]: N/A.
// [RELACIONES]: Implementa application.port.SuscripcionRepository (CU-08, CU-09).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.SuscripcionRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoSuscripcion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Plan;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Suscripcion;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.entity.SuscripcionEntity;
import com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence.repository.SuscripcionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class SuscripcionRepositoryJpaAdapter implements SuscripcionRepository {

    private final SuscripcionJpaRepository jpaRepository;

    public SuscripcionRepositoryJpaAdapter(SuscripcionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Suscripcion> buscarActivasPorCliente(UUID clienteId) {
        return jpaRepository.findByClienteIdAndEstado(clienteId, EstadoSuscripcion.ACTIVA).stream()
                .map(this::toDominio)
                .toList();
    }

    @Override
    @Transactional
    public void guardar(Suscripcion suscripcion) {
        jpaRepository.save(toEntity(suscripcion));
    }

    private Suscripcion toDominio(SuscripcionEntity entidad) {
        return Suscripcion.reconstruir(
                entidad.getId(), entidad.getClienteId(), entidad.getTipsterId(),
                new Plan(entidad.getPlanNombre(), entidad.getPlanPrecio(), entidad.getPlanDuracionDias()),
                entidad.getFechaInicio(), entidad.getFechaFin(), entidad.getEstado());
    }

    private SuscripcionEntity toEntity(Suscripcion suscripcion) {
        return new SuscripcionEntity(
                suscripcion.id(), suscripcion.clienteId(), suscripcion.tipsterId(),
                suscripcion.plan().nombre(), suscripcion.plan().precio(), suscripcion.plan().duracionDias(),
                suscripcion.fechaInicio(), suscripcion.fechaFin(), suscripcion.estado());
    }
}