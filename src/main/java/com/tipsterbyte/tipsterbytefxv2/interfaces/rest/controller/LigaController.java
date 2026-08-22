// ─────────────────────────────────────────────
// [QUÉ]: Controller REST de ligas: expone CU-04 (activar liga con URLs de fuentes)
//        y las sincronizaciones CU-01 (posiciones), CU-02 (calendario) y CU-03 (cuotas).
//        Además expone endpoints GET de consulta para el frontend Angular (listado con
//        filtros por estado/país, detalle y posiciones).
// [POR QUÉ]: Es la puerta de entrada HTTP de los casos de uso del administrador sobre
//            la ingesta de datos y de las consultas del tipster/cliente. Traduce request
//            DTOs a comandos/DTOs de application y mapea aggregates a response DTOs.
//            El bean solo se registra si app.api.rest.enabled=true (FASE 8.5 habilita
//            el wiring); hasta entonces se ejercita con MockMvc standalone.
// [RELACIONES]: CU-04 → ActivarLigaUseCase + ActivarLigaComando; CU-01/CU-02/CU-03 →
//               use cases de sincronización; consultas → LigaRepository;
//               → SincronizacionResponse, LigaResponse, LigaDetalleResponse, PosicionTablaResponse.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.ActivarLigaComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.JornadaActualDto;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ActivarLigaUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ObtenerJornadaActualUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCalendarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCuotasUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarEquiposLigaUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarPosicionesUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PosicionTabla;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request.ActivarLigaRequest;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.JornadaActualResponse;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.EquiposSincronizadosResponse;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.LigaDetalleResponse;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.LigaResponse;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.PosicionTablaResponse;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.SincronizacionResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ligas")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class LigaController {

    private final ActivarLigaUseCase activarLigaUseCase;
    private final SincronizarPosicionesUseCase sincronizarPosicionesUseCase;
    private final SincronizarCalendarioUseCase sincronizarCalendarioUseCase;
    private final SincronizarCuotasUseCase sincronizarCuotasUseCase;
    private final LigaRepository ligaRepository;
    private final ObtenerJornadaActualUseCase obtenerJornadaActualUseCase;
    private final SincronizarEquiposLigaUseCase sincronizarEquiposLigaUseCase;

    // [QUÉ]: Construye el controller con sus casos de uso y repositorio de consulta
    //        (inyección por constructor).
    public LigaController(ActivarLigaUseCase activarLigaUseCase,
                          SincronizarPosicionesUseCase sincronizarPosicionesUseCase,
                          SincronizarCalendarioUseCase sincronizarCalendarioUseCase,
                          SincronizarCuotasUseCase sincronizarCuotasUseCase,
                          LigaRepository ligaRepository,
                          ObtenerJornadaActualUseCase obtenerJornadaActualUseCase,
                          SincronizarEquiposLigaUseCase sincronizarEquiposLigaUseCase) {
        this.activarLigaUseCase = activarLigaUseCase;
        this.sincronizarPosicionesUseCase = sincronizarPosicionesUseCase;
        this.sincronizarCalendarioUseCase = sincronizarCalendarioUseCase;
        this.sincronizarCuotasUseCase = sincronizarCuotasUseCase;
        this.ligaRepository = ligaRepository;
        this.obtenerJornadaActualUseCase = obtenerJornadaActualUseCase;
        this.sincronizarEquiposLigaUseCase = sincronizarEquiposLigaUseCase;
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

    // [QUÉ]: Endpoint GET /api/v1/ligas — lista las ligas. Sin filtros devuelve las
    //        ACTIVA (selector de ligas del tipster); con ?estado=... lista el catálogo
    //        del panel geográfico (BORRADOR) y con ?estado=...&pais=... filtra además
    //        por país (nombre exacto, case-insensitive). Con ?pais=... sin estado se
    //        filtra dentro del scope por defecto (ACTIVA).
    // [POR QUÉ]: El frontend Angular necesita poblar el selector de ligas disponibles
    //            para sincronización y pronósticos (HU-01), y el panel admin necesita
    //            listar las ligas BORRADOR del catálogo por país (CU-10 → CU-04).
    // [ALTERNATIVAS]: Endpoints separados (activas vs catálogo); se descarta porque el
    //                 shape de LigaResponse es el mismo y un solo endpoint con filtros
    //                 mantiene el contrato simple (ver LigaResponse).
    @GetMapping
    public ResponseEntity<List<LigaResponse>> listarLigas(
            @RequestParam(required = false) EstadoLiga estado,
            @RequestParam(required = false) String pais) {
        List<Liga> ligas;
        if (estado != null && pais != null) {
            ligas = ligaRepository.buscarPorEstadoYPais(estado, pais);
        } else if (estado != null) {
            ligas = ligaRepository.buscarPorEstado(estado);
        } else if (pais != null) {
            ligas = ligaRepository.buscarPorEstadoYPais(EstadoLiga.ACTIVA, pais);
        } else {
            ligas = ligaRepository.buscarActivas();
        }
        List<LigaResponse> response = ligas.stream().map(this::toLigaResponse).toList();
        return ResponseEntity.ok(response);
    }

    // [QUÉ]: Endpoint GET /api/v1/ligas/{ligaId} — detalle de una liga con posiciones.
    // [POR QUÉ]: La pantalla de detalle de liga del frontend muestra nombre, país,
    //            temporada y la tabla de posiciones completa (HU-01).
    @GetMapping("/{ligaId}")
    public ResponseEntity<LigaDetalleResponse> obtenerLiga(@PathVariable UUID ligaId) {
        Liga liga = ligaRepository.buscarPorId(ligaId)
                .orElseThrow(() -> new com.tipsterbyte.tipsterbytefxv2.domain.DomainException("Liga no encontrada: " + ligaId));
        List<PosicionTablaResponse> posiciones = liga.posiciones().stream()
                .map(this::toPosicionResponse)
                .toList();
        LigaDetalleResponse response = new LigaDetalleResponse(
                liga.id(), liga.nombre(), liga.pais(), liga.estado(),
                etiquetaTemporada(liga),
                totalEquipos(liga),
                posiciones);
        return ResponseEntity.ok(response);
    }

    // [QUÉ]: Endpoint GET /api/v1/ligas/{ligaId}/posiciones — tabla de posiciones.
    // [POR QUÉ]: El frontend puede necesitar solo la clasificación sin los metadatos
    //            de la liga (ej: widget embebido). Evita cargar datos innecesarios.
    @GetMapping("/{ligaId}/posiciones")
    public ResponseEntity<List<PosicionTablaResponse>> obtenerPosiciones(@PathVariable UUID ligaId) {
        Liga liga = ligaRepository.buscarPorId(ligaId)
                .orElseThrow(() -> new com.tipsterbyte.tipsterbytefxv2.domain.DomainException("Liga no encontrada: " + ligaId));
        List<PosicionTablaResponse> posiciones = liga.posiciones().stream()
                .map(this::toPosicionResponse)
                .toList();
        return ResponseEntity.ok(posiciones);
    }

    // [QUÉ]: Endpoint POST /api/v1/ligas/{ligaId}/equipos/sincronizar — (re)puebla la
    //        plantilla de equipos de la liga desde la fuente #6 (CU-16).
    // [POR QUÉ]: Botón "Poblar equipos" de la pantalla "Países de interés → Ligas de mis
    //            países": repara plantillas vacías/incompletas (ej: scraper caído durante
    //            el poblamiento) sin re-ejecutar el catálogo mundial. Idempotente.
    // [RELACIONES]: CU-16 → ProveedorEquiposPorLiga (#6, cache Redis). Roles SUPERADMIN/TIPSTER.
    @PostMapping("/{ligaId}/equipos/sincronizar")
    public ResponseEntity<EquiposSincronizadosResponse> sincronizarEquipos(@PathVariable UUID ligaId) {
        var resultado = sincronizarEquiposLigaUseCase.ejecutar(ligaId);
        return ResponseEntity.ok(new EquiposSincronizadosResponse(
                resultado.creados(), resultado.actualizados(), resultado.totalPlantilla()));
    }

    // [QUÉ]: Endpoint GET /api/v1/ligas/{ligaId}/jornada-actual — jornada actual de la
    //        liga (jornada del próximo partido por jugarse) y la siguiente.
    // [POR QUÉ]: El frontend muestra el indicador cronológico "Jornada X de la
    //            temporada Y" por liga; el cálculo vive en el backend (única fuente de
    //            verdad, evita el drift de reloj del cliente).
    // [RELACIONES]: CU-02 → ObtenerJornadaActualUseCase → JornadaActualResponse.
    @GetMapping("/{ligaId}/jornada-actual")
    public ResponseEntity<JornadaActualResponse> obtenerJornadaActual(@PathVariable UUID ligaId) {
        JornadaActualDto dto = obtenerJornadaActualUseCase.ejecutar(ligaId);
        return ResponseEntity.ok(new JornadaActualResponse(dto.jornadaActual(), dto.proximaJornada()));
    }

    private LigaResponse toLigaResponse(Liga liga) {
        return new LigaResponse(
                liga.id(),
                liga.nombre(),
                liga.pais(),
                liga.estado(),
                etiquetaTemporada(liga),
                totalEquipos(liga),
                liga.urlSoccerway(),
                liga.apiId());
    }

    // [QUÉ]: Etiqueta "AAAA/AAAA" de la temporada vigente de la liga (activa o primera
    //        registrada); null si la liga aún no tiene temporadas.
    // [POR QUÉ]: Con múltiples temporadas por liga (Bridge Fix Torneos/Temporadas) el
    //            contrato del frontend sigue exponiendo una etiqueta simple: se prefiere
    //            la temporada activa y, en su defecto, la primera (catálogo PLANIFICADA).
    // [ALTERNATIVAS]: Exponer el arreglo completo de temporadas; se pospone a la fase
    //                 dedicada de CUs de temporadas para no romper el contrato actual.
    // [QUÉ]: Total de equipos de la temporada vigente (badge "28/30" del frontend).
    private int totalEquipos(Liga liga) {
        return liga.equipos().size();
    }

    private String etiquetaTemporada(Liga liga) {
        return liga.getTemporadaActual()
                .or(() -> liga.getTemporadas().stream().findFirst())
                .map(t -> t.anioInicio() + "/" + t.anioFin())
                .orElse(null);
    }

    private PosicionTablaResponse toPosicionResponse(PosicionTabla posicion) {
        return new PosicionTablaResponse(
                posicion.equipo().id(),
                posicion.equipo().nombre(),
                posicion.posicion(),
                posicion.jugados(),
                posicion.ganados(),
                posicion.empatados(),
                posicion.perdidos(),
                posicion.golesFavor(),
                posicion.golesContra(),
                posicion.puntos(),
                posicion.ultimosResultados());
    }
}