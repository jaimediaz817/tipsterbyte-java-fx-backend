// ─────────────────────────────────────────────
// [QUÉ]: Controller REST de suscripciones: expone CU-09 (crear suscripción de un
//        cliente a un tipster) y consulta de suscripciones activas del cliente.
// [POR QUÉ]: Es la puerta de entrada HTTP del flujo de suscripción y del dashboard
//            del cliente. Traduce request DTOs al dominio, mapea aggregates a
//            response DTOs y aplica autorización de propiedad de datos.
// [ALTERNATIVAS]: Recibir un id de plan persistido; se descarta porque el catálogo de
//                 planes aún no tiene tabla propia (FASE 8). El bean solo se registra
//                 si app.api.rest.enabled=true (FASE 8 habilita el wiring); hasta
//                 entonces se ejercita con MockMvc standalone.
// [RELACIONES]: CU-09 → CrearSuscripcionUseCase + Plan (domain.model);
//               consulta → SuscripcionRepository; → SuscripcionResponse.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.port.SuscripcionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.CrearSuscripcionUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.SuscripcionCreada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoSuscripcion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Plan;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Suscripcion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Usuario;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request.CrearSuscripcionRequest;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.SuscripcionResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suscripciones")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class SuscripcionController {

    private final CrearSuscripcionUseCase crearSuscripcionUseCase;
    private final SuscripcionRepository suscripcionRepository;

    // [QUÉ]: Construye el controller con su caso de uso y repositorio de consulta
    //        (inyección por constructor).
    public SuscripcionController(CrearSuscripcionUseCase crearSuscripcionUseCase,
                                 SuscripcionRepository suscripcionRepository) {
        this.crearSuscripcionUseCase = crearSuscripcionUseCase;
        this.suscripcionRepository = suscripcionRepository;
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

    // [QUÉ]: Endpoint GET /api/v1/suscripciones?clienteId={id} — suscripciones activas.
    // [POR QUÉ]: El cliente necesita ver sus suscripciones activas en su dashboard
    //            (HU-04). Se aplica autorización: un cliente solo ve sus propias
    //            suscripciones para evitar fugas de datos entre usuarios.
    // [ALTERNATIVAS]: Sin filtro de clienteId (devolver todo del autenticado); se
    //                 descarta porque el path param explícito documenta la intención
    //                 y facilita auditoría.
    @GetMapping
    public ResponseEntity<List<SuscripcionResponse>> listarSuscripciones(@RequestParam UUID clienteId) {
        UUID usuarioAutenticadoId = obtenerUsuarioAutenticadoId();
        if (!usuarioAutenticadoId.equals(clienteId)) {
            throw new DomainException("Acceso denegado: solo puedes consultar tus propias suscripciones");
        }
        List<SuscripcionResponse> response = suscripcionRepository.buscarActivasPorCliente(clienteId).stream()
                .map(this::toSuscripcionResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    private UUID obtenerUsuarioAutenticadoId() {
        org.springframework.security.core.context.SecurityContext context =
                org.springframework.security.core.context.SecurityContextHolder.getContext();
        org.springframework.security.core.Authentication authentication = context.getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Usuario usuario)) {
            throw new DomainException("Usuario no autenticado");
        }
        return usuario.id();
    }

    private SuscripcionResponse toSuscripcionResponse(Suscripcion suscripcion) {
        return new SuscripcionResponse(
                suscripcion.id(),
                suscripcion.clienteId(),
                suscripcion.tipsterId(),
                suscripcion.plan().nombre(),
                suscripcion.fechaInicio(),
                suscripcion.fechaFin(),
                suscripcion.estado());
    }
}