// ─────────────────────────────────────────────
// [QUÉ]: Controller REST de pronósticos: expone CU-06 (crear), CU-07 (publicar) y
//        CU-08 (consultar por liga y fecha).
// [POR QUÉ]: Agrupa las operaciones del recurso pronóstico de los actores tipster
//            (crear/publicar) y cliente (consultar), traduciendo request DTOs a
//            comandos de application y devolviendo DTOs de respuesta.
// [ALTERNATIVAS]: Endpoints separados por actor; se descarta porque el recurso es el
//                 mismo y la consulta pública ya devuelve PronosticoPublicoDto. El bean
//                 solo se registra si app.api.rest.enabled=true (FASE 8 habilita el
//                 wiring); hasta entonces se ejercita con MockMvc standalone.
// [RELACIONES]: CU-06 → CrearPronosticoUseCase + CrearPronosticoComando; CU-07 →
//               PublicarPronosticoUseCase; CU-08 → ConsultarPronosticosUseCase +
//               PronosticoPublicoDto.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.CrearPronosticoComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.PronosticoPublicoDto;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarPronosticosUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.CrearPronosticoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.PublicarPronosticoUseCase;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request.CrearPronosticoRequest;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.PronosticoResponse;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.RecursoCreadoResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pronosticos")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class PronosticoController {

    private final CrearPronosticoUseCase crearPronosticoUseCase;
    private final PublicarPronosticoUseCase publicarPronosticoUseCase;
    private final ConsultarPronosticosUseCase consultarPronosticosUseCase;

    // [QUÉ]: Construye el controller con sus casos de uso (inyección por constructor).
    public PronosticoController(CrearPronosticoUseCase crearPronosticoUseCase,
                                PublicarPronosticoUseCase publicarPronosticoUseCase,
                                ConsultarPronosticosUseCase consultarPronosticosUseCase) {
        this.crearPronosticoUseCase = crearPronosticoUseCase;
        this.publicarPronosticoUseCase = publicarPronosticoUseCase;
        this.consultarPronosticosUseCase = consultarPronosticosUseCase;
    }

    // [QUÉ]: Endpoint POST /api/v1/pronosticos — crea un pronóstico en BORRADOR (CU-06).
    // [POR QUÉ]: Devuelve 201 con el id del recurso creado para que el frontend no
    //            tenga que parsear el header Location.
    @PostMapping
    public ResponseEntity<RecursoCreadoResponse> crearPronostico(@Valid @RequestBody CrearPronosticoRequest request) {
        CrearPronosticoComando comando = new CrearPronosticoComando(
                request.tipsterId(),
                request.partidoId(),
                request.mercado(),
                request.resultadoEsperado(),
                request.cuotaValor());
        UUID pronosticoId = crearPronosticoUseCase.ejecutar(comando);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/api/v1/pronosticos/" + pronosticoId)
                .body(new RecursoCreadoResponse(pronosticoId));
    }

    // [QUÉ]: Endpoint POST /api/v1/pronosticos/{pronosticoId}/publicacion — publica un
    //        pronóstico en BORRADOR (CU-07, BR-004/BR-005).
    @PostMapping("/{pronosticoId}/publicacion")
    public ResponseEntity<Void> publicarPronostico(@PathVariable UUID pronosticoId) {
        publicarPronosticoUseCase.ejecutar(pronosticoId);
        return ResponseEntity.noContent().build();
    }

    // [QUÉ]: Endpoint GET /api/v1/pronosticos?clienteId=&ligaId=&fecha= — consulta los
    //        pronósticos PUBLICADO de tipsters suscritos para una liga y fecha (CU-08,
    //        BR-006).
    @GetMapping
    public ResponseEntity<List<PronosticoResponse>> consultarPronosticos(
            @RequestParam UUID clienteId,
            @RequestParam UUID ligaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<PronosticoResponse> pronosticos = consultarPronosticosUseCase
                .ejecutar(clienteId, ligaId, fecha, LocalDateTime.now())
                .stream()
                .map(this::toPronosticoResponse)
                .toList();
        return ResponseEntity.ok(pronosticos);
    }

    private PronosticoResponse toPronosticoResponse(PronosticoPublicoDto dto) {
        return new PronosticoResponse(
                dto.pronosticoId(), dto.tipsterId(), dto.partidoId(),
                dto.equipoLocal(), dto.equipoVisitante(), dto.fechaHora(),
                dto.mercado(), dto.resultadoEsperado(), dto.cuotaValor());
    }
}