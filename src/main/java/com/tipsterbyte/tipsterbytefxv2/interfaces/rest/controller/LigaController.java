// ─────────────────────────────────────────────
// [QUÉ]: Controller REST de ligas: expone CU-04 (activar liga con URLs de fuentes)
//        y las sincronizaciones CU-01 (posiciones), CU-02 (calendario) y CU-03 (cuotas).
// [POR QUÉ]: Es la puerta de entrada HTTP de los casos de uso del administrador sobre
//            la ingesta de datos. Traduce request DTOs a comandos/DTOs de application.
//            El bean solo se registra si app.api.rest.enabled=true (FASE 8.5 habilita
//            el wiring); hasta entonces se ejercita con MockMvc standalone.
// [RELACIONES]: CU-04 → ActivarLigaUseCase + ActivarLigaComando; CU-01/CU-02/CU-03 →
//               use cases de sincronización; → SincronizacionResponse.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.ActivarLigaComando;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ActivarLigaUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCalendarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCuotasUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarPosicionesUseCase;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request.ActivarLigaRequest;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.SincronizacionResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ligas")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class LigaController {

    private final ActivarLigaUseCase activarLigaUseCase;
    private final SincronizarPosicionesUseCase sincronizarPosicionesUseCase;
    private final SincronizarCalendarioUseCase sincronizarCalendarioUseCase;
    private final SincronizarCuotasUseCase sincronizarCuotasUseCase;

    // [QUÉ]: Construye el controller con sus casos de uso (inyección por constructor).
    public LigaController(ActivarLigaUseCase activarLigaUseCase,
                          SincronizarPosicionesUseCase sincronizarPosicionesUseCase,
                          SincronizarCalendarioUseCase sincronizarCalendarioUseCase,
                          SincronizarCuotasUseCase sincronizarCuotasUseCase) {
        this.activarLigaUseCase = activarLigaUseCase;
        this.sincronizarPosicionesUseCase = sincronizarPosicionesUseCase;
        this.sincronizarCalendarioUseCase = sincronizarCalendarioUseCase;
        this.sincronizarCuotasUseCase = sincronizarCuotasUseCase;
    }

    // [QUÉ]: Endpoint POST /api/v1/ligas/{ligaId}/activacion — activa una liga (CU-04)
    //        asociando las URLs de sus fuentes de extracción.
    @PostMapping("/{ligaId}/activacion")
    public ResponseEntity<Void> activarLiga(@PathVariable UUID ligaId,
                                            @Valid @RequestBody ActivarLigaRequest request) {
        ActivarLigaComando comando = new ActivarLigaComando(
                request.urlPosiciones(), request.urlCalendario(), request.urlCuotas());
        activarLigaUseCase.ejecutar(ligaId, comando);
        return ResponseEntity.noContent().build();
    }

    // [QUÉ]: Endpoint POST /api/v1/ligas/{ligaId}/sincronizaciones/posiciones (CU-01).
    @PostMapping("/{ligaId}/sincronizaciones/posiciones")
    public ResponseEntity<SincronizacionResponse> sincronizarPosiciones(@PathVariable UUID ligaId) {
        int eventos = sincronizarPosicionesUseCase.ejecutar(ligaId).size();
        return ResponseEntity.ok(new SincronizacionResponse(eventos));
    }

    // [QUÉ]: Endpoint POST /api/v1/ligas/{ligaId}/sincronizaciones/calendario (CU-02).
    @PostMapping("/{ligaId}/sincronizaciones/calendario")
    public ResponseEntity<SincronizacionResponse> sincronizarCalendario(@PathVariable UUID ligaId) {
        int eventos = sincronizarCalendarioUseCase.ejecutar(ligaId).size();
        return ResponseEntity.ok(new SincronizacionResponse(eventos));
    }

    // [QUÉ]: Endpoint POST /api/v1/ligas/{ligaId}/sincronizaciones/cuotas (CU-03).
    @PostMapping("/{ligaId}/sincronizaciones/cuotas")
    public ResponseEntity<SincronizacionResponse> sincronizarCuotas(@PathVariable UUID ligaId) {
        int eventos = sincronizarCuotasUseCase.ejecutar(ligaId).size();
        return ResponseEntity.ok(new SincronizacionResponse(eventos));
    }
}