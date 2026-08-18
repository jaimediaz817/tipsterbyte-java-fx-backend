// ─────────────────────────────────────────────
// [QUÉ]: Controller de CU-15: administración de tareas programadas (listar, obtener,
//        crear con cron o frecuencia amigable, actualizar cron/frecuencia/estado —
//        pausar/reanudar —, eliminar), estado de ejecución, historial de logs y
//        fuentes disponibles para programar.
// [POR QUÉ]: Expone el panel "Automatización → Tareas programadas" del frontend con
//            contrato estable (ApiError en errores: DomainException → 422, validación
//            → 400). La próxima ejecución se deriva del cron con la misma librería del
//            scheduler para alimentar el reloj de cuenta regresiva.
// [ALTERNATIVAS]: Exponer el dominio directamente; se descarta porque la vista de
//                 lectura (nextExecution) y los comandos se separan (CQS).
// [RELACIONES]: CU-15 → GestionarTareasProgramasUseCase + EstadoEjecucionTareas
//               (scheduler) → /api/v1/tareas-programadas.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.dto.ActualizarTareaProgramadaComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.FuenteDisponible;
import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarTareaProgramadaComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.EstadoEjecucionTareas;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.GestionarTareasProgramasUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Frecuencia;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.TareaProgramadaResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tareas-programadas")
public class TareaProgramadaController {

    private final GestionarTareasProgramasUseCase gestionarTareasProgramasUseCase;
    private final ObjectProvider<EstadoEjecucionTareas> estadoEjecucionTareas;

    public TareaProgramadaController(GestionarTareasProgramasUseCase gestionarTareasProgramasUseCase,
                                     ObjectProvider<EstadoEjecucionTareas> estadoEjecucionTareas) {
        this.gestionarTareasProgramasUseCase = gestionarTareasProgramasUseCase;
        this.estadoEjecucionTareas = estadoEjecucionTareas;
    }

    // DTOs de request (se parsean a comandos del caso de uso)
    public static class RegistrarTareaProgramadaRequest {
        public String ligaId;
        public String tipoFuente;
        public String prioridad;
        public String cron;
        public FrecuenciaRequest frecuencia;
        public Boolean activa;
    }

    public static class ActualizarTareaProgramadaRequest {
        public String cron;
        public FrecuenciaRequest frecuencia;
        public Boolean activa;
        public String prioridad;
    }

    public static class FrecuenciaRequest {
        public int valor;
        public String unidad;
    }

    // [QUÉ]: Lista de tareas con próxima ejecución derivada.
    @GetMapping
    public ResponseEntity<List<TareaProgramadaResponse>> listar() {
        List<TareaProgramadaResponse> respuesta = gestionarTareasProgramasUseCase.listar().stream()
                .map(this::aResponse)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    // [QUÉ]: Ids de tareas ejecutándose ahora mismo (fuente: dispatcher).
    @GetMapping("/ejecucion")
    public ResponseEntity<Set<UUID>> enEjecucion() {
        EstadoEjecucionTareas estado = estadoEjecucionTareas.getIfAvailable();
        return ResponseEntity.ok(estado == null ? Set.of() : estado.tareasEnEjecucion());
    }

    // [QUÉ]: Fuentes candidatas para el modal "Programar" (ligas activas + catálogo global).
    @GetMapping("/disponibles")
    public ResponseEntity<List<FuenteDisponible>> disponibles() {
        return ResponseEntity.ok(gestionarTareasProgramasUseCase.listarFuentesDisponibles());
    }

    // [QUÉ]: Historial de ejecuciones de una tarea (más reciente primero).
    @GetMapping("/{id}/logs")
    public ResponseEntity<List<TareaLog>> logs(@PathVariable UUID id) {
        return ResponseEntity.ok(gestionarTareasProgramasUseCase.obtenerLogs(id));
    }

    // [QUÉ]: Últimas (n) ejecuciones de todas las tareas, más recientes primero.
    // [POR QUÉ]: Monitoreo global: ver en una sola vista qué procesos corrieron juntos,
    //            su resultado (SUCCESS/ERROR) y el executionId para correlacionar con
    //            los logs JSON. Límite por defecto 20, acotado por el caso de uso a [1,100].
    @GetMapping("/logs")
    public ResponseEntity<List<TareaLog>> logsGlobales(@RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(gestionarTareasProgramasUseCase.obtenerUltimasEjecuciones(limite));
    }

    // [QUÉ]: Detalle de una tarea con próxima ejecución derivada.
    @GetMapping("/{id}")
    public ResponseEntity<TareaProgramadaResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(aResponse(gestionarTareasProgramasUseCase.obtenerPorId(id)));
    }

    // [QUÉ]: Crea una tarea con cron crudo o frecuencia amigable (default diario 00:00).
    @PostMapping
    public ResponseEntity<TareaProgramadaResponse> registrar(@RequestBody RegistrarTareaProgramadaRequest request) {
        RegistrarTareaProgramadaComando comando = new RegistrarTareaProgramadaComando(
                aUuid(request.ligaId),
                aTipo(request.tipoFuente),
                request.prioridad,
                request.cron,
                aFrecuencia(request.frecuencia),
                request.activa);
        TareaProgramada tarea = gestionarTareasProgramasUseCase.registrar(comando);
        return ResponseEntity.created(URI.create("/api/v1/tareas-programadas/" + tarea.id()))
                .body(aResponse(tarea));
    }

    // [QUÉ]: Actualiza cron/frecuencia, estado (pausar/reanudar) y/o prioridad. Devuelve
    //        la tarea actualizada para que el frontend refresque sin otro GET.
    @PutMapping("/{id}")
    public ResponseEntity<TareaProgramadaResponse> actualizar(@PathVariable UUID id,
                                                              @RequestBody ActualizarTareaProgramadaRequest request) {
        ActualizarTareaProgramadaComando comando = new ActualizarTareaProgramadaComando(
                request.cron,
                aFrecuencia(request.frecuencia),
                request.activa,
                request.prioridad);
        return ResponseEntity.ok(aResponse(gestionarTareasProgramasUseCase.actualizar(id, comando)));
    }

    // [QUÉ]: Elimina una tarea programada.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        gestionarTareasProgramasUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private TareaProgramadaResponse aResponse(TareaProgramada tarea) {
        return new TareaProgramadaResponse(
                tarea.id(), tarea.ligaId(),
                gestionarTareasProgramasUseCase.obtenerNombreLiga(tarea.ligaId()),
                tarea.tipoFuente(), tarea.prioridad(),
                tarea.cronExpression(), tarea.activa(), tarea.createdAt(),
                calcularProximaEjecucion(tarea.cronExpression()));
    }

    // [QUÉ]: Deriva la próxima ejecución del cron (misma librería del scheduler).
    // [POR QUÉ]: null si la tarea está pausada, el cron es inválido o no volverá a disparar.
    private String calcularProximaEjecucion(String cron) {
        if (cron == null || cron.isBlank()) {
            return null;
        }
        try {
            ZonedDateTime proxima = CronExpression.parse(cron).next(ZonedDateTime.now());
            return proxima == null ? null : proxima.toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static UUID aUuid(String valor) {
        return valor == null || valor.isBlank() ? null : UUID.fromString(valor);
    }

    private static TipoFuenteExtraccion aTipo(String valor) {
        return valor == null || valor.isBlank() ? null : TipoFuenteExtraccion.valueOf(valor);
    }

    private static Frecuencia aFrecuencia(FrecuenciaRequest frecuencia) {
        return frecuencia == null ? null : Frecuencia.of(frecuencia.valor, frecuencia.unidad);
    }
}