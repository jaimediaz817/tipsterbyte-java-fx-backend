// ─────────────────────────────────────────────
// [QUÉ]: Controller REST de cuotas: expone CU-21 (snapshot de cuotas próximas con
//        volatilidad) y CU-22 (historial cronológico por partido).
// [POR QUÉ]: HU-15 — el frontend necesita dos vistas: (1) resumen de partidos con
//            badges de volatilidad para decidir rápido, (2) drill-down de historial
//            para gráficos al expandir un partido.
// [ALTERNATIVAS]: Agregar estos endpoints a LigaController/PartidoController; se
//                 descarta porque agrupa responsabilidad de cuotas en un solo lugar.
// [RELACIONES]: CU-21 → ConsultarCuotasProximasUseCase; CU-22 → ConsultarHistorialCuotasUseCase.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarCuotasProximasUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarHistorialCuotasUseCase;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.CuotaProximaResponse;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.HistorialCuotaResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class CuotasController {

    private final ConsultarCuotasProximasUseCase consultarCuotasProximasUseCase;
    private final ConsultarHistorialCuotasUseCase consultarHistorialCuotasUseCase;

    public CuotasController(ConsultarCuotasProximasUseCase consultarCuotasProximasUseCase,
                            ConsultarHistorialCuotasUseCase consultarHistorialCuotasUseCase) {
        this.consultarCuotasProximasUseCase = consultarCuotasProximasUseCase;
        this.consultarHistorialCuotasUseCase = consultarHistorialCuotasUseCase;
    }

    // [QUÉ]: HU-15 AC1 — snapshot de cuotas próximas con volatilidad.
    //        GET /api/v1/ligas/{ligaId}/cuotas-proximas?ventanaHoras=24
    @GetMapping("/ligas/{ligaId}/cuotas-proximas")
    public ResponseEntity<List<CuotaProximaResponse>> cuotasProximas(
            @PathVariable UUID ligaId,
            @RequestParam(defaultValue = "24") int ventanaHoras) {
        return ResponseEntity.ok(consultarCuotasProximasUseCase.ejecutar(ligaId, ventanaHoras));
    }

    // [QUÉ]: HU-15 AC2 — historial cronológico de cuotas de un partido.
    //        GET /api/v1/partidos/{partidoId}/cuotas/historial?horas=24&mercado=UNO_X_DOS
    @GetMapping("/partidos/{partidoId}/cuotas/historial")
    public ResponseEntity<List<HistorialCuotaResponse>> historialCuotas(
            @PathVariable UUID partidoId,
            @RequestParam(defaultValue = "24") int horas,
            @RequestParam(required = false) String mercado) {
        return ResponseEntity.ok(consultarHistorialCuotasUseCase.ejecutar(partidoId, horas, mercado));
    }
}
