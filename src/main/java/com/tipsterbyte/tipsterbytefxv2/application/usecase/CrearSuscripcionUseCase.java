// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-09 (HU-09): crea una suscripción ACTIVA de un cliente a un
//        tipster con su plan y fechas de vigencia.
// [POR QUÉ]: Construye el aggregate Suscripcion, que calcula fechaFin desde el plan y
//            emite SuscripcionCreada. El caso de uso solo orquesta y persiste.
// [ALTERNATIVAS]: Calcular fechas en application; se descarta porque el aggregate ya
//                 protege la coherencia del periodo de vigencia.
// [RELACIONES]: HU-09 → CU-09 → SuscripcionRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.SuscripcionRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Plan;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Suscripcion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class CrearSuscripcionUseCase {

    private final SuscripcionRepository suscripcionRepository;

    // [QUÉ]: Construye el caso de uso con su puerto (inyección por constructor).
    public CrearSuscripcionUseCase(SuscripcionRepository suscripcionRepository) {
        this.suscripcionRepository = suscripcionRepository;
    }

    // [QUÉ]: Ejecuta CU-09: crea la suscripción ACTIVA y la persiste. Devuelve el
    //        evento SuscripcionCreada.
    public List<DomainEvent> ejecutar(UUID clienteId, UUID tipsterId, Plan plan, LocalDateTime fechaInicio) {
        Suscripcion suscripcion = new Suscripcion(clienteId, tipsterId, plan, fechaInicio);
        suscripcionRepository.guardar(suscripcion);
        return suscripcion.pullEventos();
    }
}