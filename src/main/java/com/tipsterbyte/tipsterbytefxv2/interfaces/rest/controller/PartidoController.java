// ─────────────────────────────────────────────
// [QUÉ]: Controller REST de partidos: expone CU-05 (registrar resultado de partido)
//        y endpoints GET de consulta (por liga, fecha, próximos y cuotas).
// [POR QUÉ]: Es la puerta de entrada HTTP para registro de resultados y para que
//            el frontend Angular consulte calendarios, próximos partidos y cuotas
//            (HU-02, HU-03). Traduce request DTOs al dominio y mapea aggregates a
//            response DTOs.
// [ALTERNATIVAS]: Incluirlo en LigaController; se descarta porque el partido es un
//                 recurso propio con su ruta (API REST coherente). El bean solo se
//                 registra si app.api.rest.enabled=true (FASE 8 habilita el wiring);
//                 hasta entonces se ejercita con MockMvc standalone.
// [RELACIONES]: CU-05 → RegistrarResultadoUseCase + Resultado (domain.model);
//               consultas → PartidoRepository; → PartidoResponse, CuotaResponse.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.RegistrarResultadoUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Cuota;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Resultado;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request.RegistrarResultadoRequest;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.CuotaResponse;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.PartidoResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/partidos")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class PartidoController {

    private final RegistrarResultadoUseCase registrarResultadoUseCase;
    private final PartidoRepository partidoRepository;

    // [QUÉ]: Construye el controller con su caso de uso y repositorio de consulta
    //        (inyección por constructor).
    public PartidoController(RegistrarResultadoUseCase registrarResultadoUseCase,
                             PartidoRepository partidoRepository) {
        this.registrarResultadoUseCase = registrarResultadoUseCase;
        this.partidoRepository = partidoRepository;
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

    // [QUÉ]: Endpoint GET /api/v1/partidos?ligaId={id} — partidos de una liga.
    // [POR QUÉ]: El frontend necesita listar el calendario completo de una liga
    //            seleccionada (HU-02).
    @GetMapping
    public ResponseEntity<List<PartidoResponse>> listarPartidos(
            @RequestParam UUID ligaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Boolean proximos) {

        List<Partido> partidos;
        if (Boolean.TRUE.equals(proximos)) {
            partidos = partidoRepository.buscarProximosPorLiga(ligaId);
        } else if (fecha != null) {
            partidos = partidoRepository.buscarPorLigaYFecha(ligaId, fecha);
        } else {
            partidos = partidoRepository.buscarPorLiga(ligaId);
        }
        return ResponseEntity.ok(partidos.stream().map(this::toPartidoResponse).toList());
    }

    // [QUÉ]: Endpoint GET /api/v1/partidos/{partidoId}/cuotas — cuotas de un partido.
    // [POR QUÉ]: La pantalla de pronósticos del frontend necesita ver las cuotas
    //            disponibles para un partido específico (HU-03).
    @GetMapping("/{partidoId}/cuotas")
    public ResponseEntity<List<CuotaResponse>> obtenerCuotas(@PathVariable UUID partidoId) {
        Partido partido = partidoRepository.buscarPorId(partidoId)
                .orElseThrow(() -> new com.tipsterbyte.tipsterbytefxv2.domain.DomainException("Partido no encontrado: " + partidoId));
        List<CuotaResponse> cuotas = partido.cuotas().stream()
                .map(this::toCuotaResponse)
                .toList();
        return ResponseEntity.ok(cuotas);
    }

    private PartidoResponse toPartidoResponse(Partido partido) {
        String resultado = null;
        if (partido.resultado() != null) {
            resultado = partido.resultado().golesLocal() + "-" + partido.resultado().golesVisitante();
        }
        return new PartidoResponse(
                partido.id(),
                partido.ligaId(),
                partido.equipoLocal().nombre(),
                partido.equipoVisitante().nombre(),
                partido.fechaProgramada().fechaHora(),
                partido.jornada(),
                partido.estado(),
                resultado,
                partido.cuotas().stream().map(this::toCuotaResponse).toList());
    }

    private CuotaResponse toCuotaResponse(Cuota cuota) {
        return new CuotaResponse(cuota.mercado().name(), cuota.valor());
    }
}