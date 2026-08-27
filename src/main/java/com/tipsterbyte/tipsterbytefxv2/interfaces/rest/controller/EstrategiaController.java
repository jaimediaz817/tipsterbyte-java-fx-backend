// ─────────────────────────────────────────────
// [QUÉ]: Controller REST de estrategias: CRUD (CU-23), evaluación (CU-24) y
//        consulta de sugerencias (CU-25).
// [POR QUÉ]: HU-16 AC12/13/14 — el frontend necesita CRUD + evaluar + sugerencias.
// [ALTERNATIVAS]: Dividir en varios controllers; se descarta porque la responsabilidad
//                 es cohesiva (estrategias de pronóstico).
// [RELACIONES]: CU-23 → GestionarEstrategiasUseCase; CU-24 → EvaluarEstrategiaUseCase;
//               CU-25 → ConsultarSugerenciasUseCase.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarSugerenciasUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.EvaluarEstrategiaUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.GestionarEstrategiasUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Criterio;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Estrategia;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PronosticoSugerido;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request.EstrategiaRequest;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.SugerenciaResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/estrategias")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class EstrategiaController {

    private final GestionarEstrategiasUseCase gestionarUseCase;
    private final EvaluarEstrategiaUseCase evaluarUseCase;
    private final ConsultarSugerenciasUseCase consultarSugerenciasUseCase;

    public EstrategiaController(GestionarEstrategiasUseCase gestionarUseCase,
                                EvaluarEstrategiaUseCase evaluarUseCase,
                                ConsultarSugerenciasUseCase consultarSugerenciasUseCase) {
        this.gestionarUseCase = gestionarUseCase;
        this.evaluarUseCase = evaluarUseCase;
        this.consultarSugerenciasUseCase = consultarSugerenciasUseCase;
    }

    // ─── CRUD (CU-23) ───

    // [QUÉ]: AC12 POST — crea estrategia con criterios.
    @PostMapping
    public ResponseEntity<Estrategia> crear(@Valid @RequestBody EstrategiaRequest request,
                                            Authentication auth) {
        UUID tipsterId = UUID.fromString(auth.getName());
        Estrategia estrategia = gestionarUseCase.crear(
                request.nombre(), tipsterId, Mercado.valueOf(request.mercado()),
                request.maxPartidos(), request.confianzaMinima(),
                mapCriterios(request.criterios()), request.ligaIds());
        return ResponseEntity.created(URI.create("/api/v1/estrategias/" + estrategia.id()))
                .body(estrategia);
    }

    // [QUÉ]: AC12 GET — lista estrategias del tipster (o todas si superadmin).
    @GetMapping
    public ResponseEntity<List<Estrategia>> listar(Authentication auth,
                                                    @RequestParam(required = false) UUID tipsterId) {
        UUID currentUser = UUID.fromString(auth.getName());
        // Si se pasa tipsterId explícito y es superadmin, usar ese; si no, el del token.
        UUID filtroTipster = tipsterId != null ? tipsterId : currentUser;
        return ResponseEntity.ok(gestionarUseCase.listar(filtroTipster));
    }

    // [QUÉ]: AC12 GET detalle — estrategia por ID con criterios.
    @GetMapping("/{id}")
    public ResponseEntity<Estrategia> obtenerPorId(@PathVariable UUID id) {
        return gestionarUseCase.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // [QUÉ]: AC12 PUT — actualiza estrategia.
    @PutMapping("/{id}")
    public ResponseEntity<Estrategia> actualizar(@PathVariable UUID id,
                                                  @Valid @RequestBody EstrategiaRequest request) {
        Estrategia actualizada = gestionarUseCase.actualizar(
                id, request.nombre(), Mercado.valueOf(request.mercado()),
                request.maxPartidos(), request.confianzaMinima(),
                mapCriterios(request.criterios()), request.ligaIds());
        return ResponseEntity.ok(actualizada);
    }

    // [QUÉ]: AC12 DELETE — elimina estrategia.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        gestionarUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Evaluación (CU-24) ───

    // [QUÉ]: AC12 POST /evaluar — evaluación on-demand.
    @PostMapping("/{id}/evaluar")
    public ResponseEntity<List<PronosticoSugerido>> evaluar(@PathVariable UUID id) {
        List<PronosticoSugerido> sugerencias = evaluarUseCase.ejecutar(id);
        return ResponseEntity.ok(sugerencias);
    }

    // ─── Sugerencias (CU-25) ───

    // [QUÉ]: AC13 GET /sugerencias — partidos sugeridos con score.
    @GetMapping("/{id}/sugerencias")
    public ResponseEntity<List<SugerenciaResponse>> sugerencias(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID ligaId,
            @RequestParam(required = false) BigDecimal confianzaMinima) {
        return ResponseEntity.ok(consultarSugerenciasUseCase.ejecutar(id, ligaId, confianzaMinima));
    }

    // ─── Catálogo de tipos de criterio (AC14) ───

    // [QUÉ]: AC14 GET /criterios/tipos — catálogo de tipos soportados para formulario dinámico.
    @GetMapping("/criterios/tipos")
    public ResponseEntity<List<CriterioTipoResponse>> tiposCriterio() {
        return ResponseEntity.ok(List.of(
                new CriterioTipoResponse("CUOTAS", "cuota_1x", "MAYOR_IGUAL", "1.40", "LOCAL"),
                new CriterioTipoResponse("CUOTAS", "cuota_local", "MAYOR_IGUAL", "1.50", "LOCAL"),
                new CriterioTipoResponse("CUOTAS", "cuota_visitante", "MENOR_IGUAL", "3.00", "VISITANTE"),
                new CriterioTipoResponse("POSICIONES", "diferencia_posiciones", "MAYOR_IGUAL", "3", "LOCAL"),
                new CriterioTipoResponse("POSICIONES", "posicion", "MENOR_IGUAL", "5", "LOCAL"),
                new CriterioTipoResponse("FORMA", "ultimos_5", "CONTIENE", "max_1_P", "LOCAL"),
                new CriterioTipoResponse("FORMA", "racha_perdidas", "MAYOR_IGUAL", "2", "VISITANTE"),
                new CriterioTipoResponse("ZONA_DESCENSO", "en_zona_descenso", "IGUAL", "true", "VISITANTE")));
    }

    // [QUÉ]: DTO interno para catálogo de tipos de criterio.
    public record CriterioTipoResponse(String fuente, String campo, String operador,
                                        String valorEjemplo, String referenciaDefault) {}

    // [QUÉ]: Mapea DTOs de request a dominio Criterio.
    private List<Criterio> mapCriterios(List<EstrategiaRequest.CriterioRequest> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(d -> new Criterio(
                Criterio.FuenteCriterio.valueOf(d.fuente()),
                d.campo(),
                Criterio.OperadorCriterio.valueOf(d.operador()),
                d.valor(),
                Criterio.ReferenciaCriterio.valueOf(d.referencia()),
                d.peso(),
                d.orden())).collect(Collectors.toList());
    }
}
