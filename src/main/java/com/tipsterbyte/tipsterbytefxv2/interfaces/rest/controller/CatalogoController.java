// ─────────────────────────────────────────────
// [QUÉ]: Controller REST del catálogo geográfico (CU-10 + HU-12 granular): dispara el
//        poblamiento COMPLETO ASÍNCRONO (FASE T3) y el granular por pasos (países sync
//        200 + ligas por país async 202), y expone estado/progreso por executionId.
// [POR QUÉ]: El poblamiento completo tarda 10-30 min: 202 + polling evita timeouts.
//            El granular permite al SUPERADMIN entender cada etapa sin recorrer 176
//            países: poblar-paises es síncrono (176 filas, <2s), poblar-ligas/{iso}
//            es async por país (30s-3min, scraping #5 + #6).
// [ALTERNATIVAS]: Mantener solo el recorrido completo; se descarta porque el frontend
//                 necesita granular para el panel Geografía (banner Vacío + ⟳ por país).
// [RELACIONES]: CU-10 → SincronizarCatalogoAsyncUseCase + ConsultarEstadoCatalogoUseCase;
//               HU-12 → CU-17 SincronizarPaisesUseCase (sync) + CU-18
//               SincronizarLigasPorPaisAsyncUseCase (async por iso). Roles: SUPERADMIN.
//               Anti-solapamiento → 409 vía PoblamientoEnCursoException.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.port.ProgresoPoblamiento;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaLogRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarEstadoCatalogoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCatalogoAsyncUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarLigasPorPaisAsyncUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarPaisesUseCase;
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
    private final SincronizarPaisesUseCase sincronizarPaisesUseCase;
    private final SincronizarLigasPorPaisAsyncUseCase sincronizarLigasPorPaisAsyncUseCase;

    public CatalogoController(SincronizarCatalogoAsyncUseCase sincronizarCatalogoAsyncUseCase,
                              ConsultarEstadoCatalogoUseCase consultarEstadoCatalogoUseCase,
                              TareaLogRepository tareaLogRepository,
                              ProgresoPoblamiento progresoPoblamiento,
                              SincronizarPaisesUseCase sincronizarPaisesUseCase,
                              SincronizarLigasPorPaisAsyncUseCase sincronizarLigasPorPaisAsyncUseCase) {
        this.sincronizarCatalogoAsyncUseCase = sincronizarCatalogoAsyncUseCase;
        this.consultarEstadoCatalogoUseCase = consultarEstadoCatalogoUseCase;
        this.tareaLogRepository = tareaLogRepository;
        this.progresoPoblamiento = progresoPoblamiento;
        this.sincronizarPaisesUseCase = sincronizarPaisesUseCase;
        this.sincronizarLigasPorPaisAsyncUseCase = sincronizarLigasPorPaisAsyncUseCase;
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

    // [QUÉ]: HU-12 paso 1 — POST /api/v1/catalogo/poblar-paises (200 síncrono).
    // [POR QUÉ]: ~176 filas de #1 sin scraping pesado: síncrono evita polling innecesario
    //            y da feedback inmediato (void para el frontend + GET /estado refresca).
    @PostMapping("/poblar-paises")
    public ResponseEntity<Void> poblarPaises() {
        sincronizarPaisesUseCase.ejecutar();
        return ResponseEntity.ok().build();
    }

    // [QUÉ]: HU-12 paso 2 — POST /api/v1/catalogo/poblar-ligas/{isoAlpha2} (202 async).
    // [POR QUÉ]: Scraping #5 + temporadas + #6 por liga puede tardar 30s-3min: async con
    //            executionId reutiliza el polling GET /activar/{executionId} (TareaLog).
    @PostMapping("/poblar-ligas/{isoAlpha2}")
    public ResponseEntity<PoblamientoIniciadoResponse> poblarLigas(@PathVariable String isoAlpha2) {
        String executionId = sincronizarLigasPorPaisAsyncUseCase.ejecutarAsync(isoAlpha2);
        return ResponseEntity.accepted().body(new PoblamientoIniciadoResponse(
                executionId,
                SincronizarLigasPorPaisAsyncUseCase.ESTADO_RUNNING,
                "/api/v1/catalogo/activar/" + executionId));
    }

    private CatalogoEstadoResponse toResponse(com.tipsterbyte.tipsterbytefxv2.application.dto.CatalogoEstadoDto dto) {
        return new CatalogoEstadoResponse(dto.estado(), dto.totalPaises(), dto.totalLigas());
    }
}
