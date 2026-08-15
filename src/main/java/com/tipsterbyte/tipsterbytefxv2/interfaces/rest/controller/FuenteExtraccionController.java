// ─────────────────────────────────────────────
// [QUÉ]: Controller REST del catálogo de fuentes de extracción: expone CU-11
//        (registrar y listar fuentes) y la gestión de la URL de una fuente por liga.
// [POR QUÉ]: Es la puerta de entrada HTTP de la gestión del catálogo de fuentes. Los
//            endpoints coinciden con los que consumirá el formulario Angular del
//            usuario (POST/GET /api/v1/fuentes; PUT/GET /api/v1/ligas/{ligaId}/fuentes).
//            Traduce request DTOs a comandos de application.
// [ALTERNATIVAS]: Un controller por recurso; se descarta porque agrupar por recurso
//                 (fuentes) mantiene la API REST coherente. El bean solo se registra
//                 si app.api.rest.enabled=true (FASE 8.5 habilita el wiring).
// [RELACIONES]: CU-11 → GestionarFuenteExtraccionUseCase; → FuenteExtraccionResponse.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.AsociarUrlFuenteComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarFuenteComando;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.GestionarFuenteExtraccionUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request.AsociarUrlFuenteRequest;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request.RegistrarFuenteRequest;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.FuenteExtraccionResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class FuenteExtraccionController {

    private final GestionarFuenteExtraccionUseCase gestionarFuenteUseCase;

    // [QUÉ]: Construye el controller con su caso de uso (inyección por constructor).
    public FuenteExtraccionController(GestionarFuenteExtraccionUseCase gestionarFuenteUseCase) {
        this.gestionarFuenteUseCase = gestionarFuenteUseCase;
    }

    // [QUÉ]: Endpoint POST /api/v1/fuentes — registra una fuente en el catálogo (CU-11).
    @PostMapping("/fuentes")
    public ResponseEntity<Void> registrarFuente(@Valid @RequestBody RegistrarFuenteRequest request) {
        gestionarFuenteUseCase.registrarFuente(new RegistrarFuenteComando(
                request.nombre(), request.tipo(), request.activa()));
        return ResponseEntity.created(URI.create("/api/v1/fuentes")).build();
    }

    // [QUÉ]: Endpoint GET /api/v1/fuentes — lista el catálogo de fuentes (CU-11).
    @GetMapping("/fuentes")
    public ResponseEntity<List<FuenteExtraccionResponse>> listarFuentes() {
        List<FuenteExtraccionResponse> respuestas = gestionarFuenteUseCase.listarFuentes().stream()
                .map(f -> new FuenteExtraccionResponse(f.id(), f.nombre(), f.tipo(), null, f.activa()))
                .toList();
        return ResponseEntity.ok(respuestas);
    }

    // [QUÉ]: Endpoint PUT /api/v1/ligas/{ligaId}/fuentes/{tipo} — asocia la URL de una
    //        fuente a una liga (CU-11).
    @PutMapping("/ligas/{ligaId}/fuentes/{tipo}")
    public ResponseEntity<Void> asociarUrlFuente(@PathVariable UUID ligaId,
                                                 @PathVariable TipoFuenteExtraccion tipo,
                                                 @Valid @RequestBody AsociarUrlFuenteRequest request) {
        gestionarFuenteUseCase.asociarUrlFuente(new AsociarUrlFuenteComando(
                ligaId, tipo, request.url(), request.activa()));
        return ResponseEntity.noContent().build();
    }

    // [QUÉ]: Endpoint GET /api/v1/ligas/{ligaId}/fuentes — lista las URLs de fuentes
    //        asociadas a una liga (CU-11).
    @GetMapping("/ligas/{ligaId}/fuentes")
    public ResponseEntity<List<FuenteExtraccionResponse>> listarFuentesDeLiga(@PathVariable UUID ligaId) {
        List<FuenteExtraccionResponse> respuestas = gestionarFuenteUseCase.listarDetallesDeLiga(ligaId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(respuestas);
    }

    // [QUÉ]: Convierte un DetalleFuenteExtraccion del dominio en el response DTO.
    private FuenteExtraccionResponse toResponse(DetalleFuenteExtraccion detalle) {
        FuenteExtraccion fuente = detalle.fuente();
        return new FuenteExtraccionResponse(
                fuente.id(), fuente.nombre(), fuente.tipo(), detalle.url(), detalle.activa());
    }
}
