// ─────────────────────────────────────────────
// [QUÉ]: Controller REST del catálogo geográfico (CU-10): dispara el poblamiento
//        ASÍNCRONO (FASE T3, H-02) y expone el estado del catálogo y el progreso de la
//        ejecución por executionId.
// [POR QUÉ]: El poblamiento tarda 10-30 min: responder 202 con executionId y dejar que
//            el frontend haga polling evita timeouts, spinner infinito y doble
//            ejecución. La vía programada (tarea global) ya usaba este patrón.
// [ALTERNATIVAS]: Mantener síncrono; se descarta en hallazgos-arquitectura.md (H-02).
// [RELACIONES]: CU-10 → SincronizarCatalogoAsyncUseCase + ConsultarEstadoCatalogoUseCase.
//               Roles: SUPERADMIN. Anti-solapamiento → 409 vía PoblamientoEnCursoException.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.port.ProgresoPoblamiento;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaLogRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarEstadoCatalogoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCatalogoAsyncUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.CatalogoEstadoResponse;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.PoblamientoEstadoResponse;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.PoblamientoIniciadoResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalogo")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class CatalogoController {

    private final SincronizarCatalogoAsyncUseCase sincronizarCatalogoAsyncUseCase;
    private final ConsultarEstadoCatalogoUseCase consultarEstadoCatalogoUseCase;
    private final TareaLogRepository tareaLogRepository;
    private final ProgresoPoblamiento progresoPoblamiento;

    public CatalogoController(SincronizarCatalogoAsyncUseCase sincronizarCatalogoAsyncUseCase,
                              ConsultarEstadoCatalogoUseCase consultarEstadoCatalogoUseCase,
                              TareaLogRepository tareaLogRepository,
                              ProgresoPoblamiento progresoPoblamiento) {
        this.sincronizarCatalogoAsyncUseCase = sincronizarCatalogoAsyncUseCase;
        this.consultarEstadoCatalogoUseCase = consultarEstadoCatalogoUseCase;
        this.tareaLogRepository = tareaLogRepository;
        this.progresoPoblamiento = progresoPoblamiento;
    }

    // [QUÉ]: Endpoint POST /api/v1/catalogo/activar — lanza el poblamiento geográfico
    //        en background (FASE T3) y devuelve 202 con el executionId para polling.
    // [POR QUÉ]: Evita timeouts y doble ejecución: si ya hay una en curso responde 409
    //            (PoblamientoEnCursoException) sin lanzar otra.
    @PostMapping("/activar")
    public ResponseEntity<PoblamientoIniciadoResponse> activar() {
        String executionId = sincronizarCatalogoAsyncUseCase.ejecutarAsync();
        return ResponseEntity.accepted().body(new PoblamientoIniciadoResponse(
                executionId,
                SincronizarCatalogoAsyncUseCase.ESTADO_RUNNING,
                "/api/v1/catalogo/activar/" + executionId));
    }

    // [QUÉ]: Endpoint GET /api/v1/catalogo/activar/{executionId} — estado/progreso de
    //        una ejecución manual de poblamiento para el polling del frontend.
    // [POR QUÉ]: RUNNING lee el snapshot en memoria (país en curso); SUCCESS/ERROR leen
    //            el TareaLog finalizado (duración, mensaje de error).
    @GetMapping("/activar/{executionId}")
    public ResponseEntity<PoblamientoEstadoResponse> estadoActivacion(@PathVariable String executionId) {
        var logs = tareaLogRepository.buscarPorExecutionId(executionId);
        if (logs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TareaLog log = logs.get(0);
        boolean running = SincronizarCatalogoAsyncUseCase.ESTADO_RUNNING.equals(log.status());
        java.util.Optional<ProgresoPoblamiento.Progreso> snapshot = running
                ? progresoPoblamiento.snapshot()
                : java.util.Optional.empty();
        return ResponseEntity.ok(new PoblamientoEstadoResponse(
                log.executionId(),
                log.status(),
                snapshot.map(p -> p.paisActual()).orElse(null),
                snapshot.map(p -> p.paisesProcesados()).orElse(null),
                log.timestamp() != null ? log.timestamp().toString() : null,
                log.durationMs(),
                log.mensaje()));
    }

    // [QUÉ]: Endpoint GET /api/v1/catalogo/estado — devuelve el estado actual del
    //        catálogo (VACIO/POBLADO) con los conteos de países y ligas.
    // [POR QUÉ]: El frontend consulta este estado al cargar el panel para saber si
    //            el catálogo ya fue poblado y habilitar el flujo de activación.
    @GetMapping("/estado")
    public ResponseEntity<CatalogoEstadoResponse> estado() {
        return ResponseEntity.ok(toResponse(consultarEstadoCatalogoUseCase.ejecutar()));
    }

    private CatalogoEstadoResponse toResponse(com.tipsterbyte.tipsterbytefxv2.application.dto.CatalogoEstadoDto dto) {
        return new CatalogoEstadoResponse(dto.estado(), dto.totalPaises(), dto.totalLigas());
    }
}
