// ─────────────────────────────────────────────
// [QUÉ]: Controller REST de partidos: expone CU-05 (registrar resultado de partido).
// [POR QUÉ]: Es la puerta de entrada HTTP para registrar el marcador final, traduciendo
//            el request DTO al VO Resultado del dominio.
// [ALTERNATIVAS]: Incluirlo en LigaController; se descarta porque el partido es un
//                 recurso propio con su ruta (API REST coherente). El bean solo se
//                 registra si app.api.rest.enabled=true (FASE 8 habilita el wiring);
//                 hasta entonces se ejercita con MockMvc standalone.
// [RELACIONES]: CU-05 → RegistrarResultadoUseCase + Resultado (domain.model).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.usecase.RegistrarResultadoUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Resultado;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request.RegistrarResultadoRequest;
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
@RequestMapping("/api/v1/partidos")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class PartidoController {

    private final RegistrarResultadoUseCase registrarResultadoUseCase;

    // [QUÉ]: Construye el controller con su caso de uso (inyección por constructor).
    public PartidoController(RegistrarResultadoUseCase registrarResultadoUseCase) {
        this.registrarResultadoUseCase = registrarResultadoUseCase;
    }

    // [QUÉ]: Endpoint POST /api/v1/partidos/{partidoId}/resultado — registra el
    //        resultado final de un partido (CU-05, BR-003).
    @PostMapping("/{partidoId}/resultado")
    public ResponseEntity<Void> registrarResultado(@PathVariable UUID partidoId,
                                                   @Valid @RequestBody RegistrarResultadoRequest request) {
        Resultado resultado = new Resultado(request.golesLocal(), request.golesVisitante());
        registrarResultadoUseCase.ejecutar(partidoId, resultado);
        return ResponseEntity.noContent().build();
    }
}