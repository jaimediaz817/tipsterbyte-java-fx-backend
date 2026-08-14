// ─────────────────────────────────────────────
// [QUÉ]: Controller REST de suscripciones: expone CU-09 (crear suscripción de un
//        cliente a un tipster).
// [POR QUÉ]: Es la puerta de entrada HTTP del flujo de suscripción, traduciendo el
//            request DTO al aggregate Suscripcion (a través del VO Plan).
// [ALTERNATIVAS]: Recibir un id de plan persistido; se descarta porque el catálogo de
//                 planes aún no tiene tabla propia (FASE 8). El bean solo se registra
//                 si app.api.rest.enabled=true (FASE 8 habilita el wiring); hasta
//                 entonces se ejercita con MockMvc standalone.
// [RELACIONES]: CU-09 → CrearSuscripcionUseCase + Plan (domain.model); →
//               SuscripcionResponse.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.usecase.CrearSuscripcionUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.event.SuscripcionCreada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoSuscripcion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Plan;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request.CrearSuscripcionRequest;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.SuscripcionResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suscripciones")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class SuscripcionController {

    private final CrearSuscripcionUseCase crearSuscripcionUseCase;

    // [QUÉ]: Construye el controller con su caso de uso (inyección por constructor).
    public SuscripcionController(CrearSuscripcionUseCase crearSuscripcionUseCase) {
        this.crearSuscripcionUseCase = crearSuscripcionUseCase;
    }

    // [QUÉ]: Endpoint POST /api/v1/suscripciones — crea una suscripción ACTIVA (CU-09).
    @PostMapping
    public ResponseEntity<SuscripcionResponse> crearSuscripcion(@Valid @RequestBody CrearSuscripcionRequest request) {
        Plan plan = new Plan(request.planNombre(), request.planPrecio(), request.planDuracionDias());
        UUID suscripcionId = crearSuscripcionUseCase
                .ejecutar(request.clienteId(), request.tipsterId(), plan, request.fechaInicio())
                .stream()
                .filter(SuscripcionCreada.class::isInstance)
                .map(SuscripcionCreada.class::cast)
                .map(SuscripcionCreada::aggregateId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se pudo crear la suscripción"));

        SuscripcionResponse response = new SuscripcionResponse(
                suscripcionId,
                request.clienteId(),
                request.tipsterId(),
                plan.nombre(),
                request.fechaInicio(),
                request.fechaInicio().plusDays(plan.duracionDias()),
                EstadoSuscripcion.ACTIVA);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}