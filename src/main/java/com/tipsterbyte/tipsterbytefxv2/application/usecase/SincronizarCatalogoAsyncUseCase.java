// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso FASE T3: ejecuta CU-10 (poblamiento geográfico) en background
//        con trazabilidad TareaLog y anti-solapamiento. Devuelve inmediatamente un
//        executionId para polling del frontend.
// [POR QUÉ]: El poblamiento manual tarda 10-30 min (176 países × ligas × #6): síncrono
//            provoca timeouts, spinner infinito y doble ejecución por impaciencia
//            (H-02). Reutiliza el patrón del scheduler (hilos virtuales + TareaLog con
//            SUCCESS/ERROR/duracionMs) añadiendo estado RUNNING para el polling.
// [ALTERNATIVAS]: Nueva Entity PoblamientoEjecucion; se descarta porque duplica
//                 TareaLog. WebSocket/SSE; se descarta para v1 (polling basta).
// [RELACIONES]: H-02 → delega en SincronizarCatalogoUseCase (CU-10); persiste vía
//               TareaLogRepository; expone progreso vía ProgresoPoblamiento.
//               Consumido por CatalogoController (POST /catalogo/activar → 202).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.ProgresoPoblamiento;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaLogRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.PoblamientoEnCursoException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SincronizarCatalogoAsyncUseCase {

    public static final String ESTADO_RUNNING = "RUNNING";
    public static final String ESTADO_SUCCESS = "SUCCESS";
    public static final String ESTADO_ERROR = "ERROR";

    private static final Logger log = LoggerFactory.getLogger(SincronizarCatalogoAsyncUseCase.class);

    private final SincronizarCatalogoUseCase delegado;
    private final TareaLogRepository tareaLogRepository;
    private final ProgresoPoblamiento progreso;

    // Anti-solapamiento global de la vía manual: una sola ejecución a la vez.
    private final AtomicBoolean enCurso = new AtomicBoolean(false);

    // Hilos virtuales: coste mínimo para tareas I/O-bound (scraping), igual que el scheduler.
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public SincronizarCatalogoAsyncUseCase(SincronizarCatalogoUseCase delegado,
                                           TareaLogRepository tareaLogRepository,
                                           ProgresoPoblamiento progreso) {
        this.delegado = delegado;
        this.tareaLogRepository = tareaLogRepository;
        this.progreso = progreso;
    }

    // [QUÉ]: Lanza el poblamiento en background y devuelve el executionId para polling.
    // [POR QUÉ]: compareAndSet garantiza que solo UNA ejecución manual esté activa:
    //            si ya hay una, lanza PoblamientoEnCursoException → HTTP 409.
    public String ejecutarAsync() {
        if (!enCurso.compareAndSet(false, true)) {
            throw new PoblamientoEnCursoException(
                    "Ya hay un poblamiento geográfico en curso; espera a que termine o consulta su estado");
        }
        String executionId = UUID.randomUUID().toString();
        Instant inicio = Instant.now();
        tareaLogRepository.guardar(new TareaLog(
                UUID.randomUUID(), null, executionId, inicio,
                ESTADO_RUNNING, null, null,
                "Poblamiento geográfico manual en curso", null));
        log.info("FASE T3: poblamiento manual iniciado (executionId={})", executionId);

        executor.submit(() -> {
            try {
                delegado.ejecutar();
                Instant fin = Instant.now();
                tareaLogRepository.guardar(new TareaLog(
                        UUID.randomUUID(), null, executionId, fin,
                        ESTADO_SUCCESS, Duration.between(inicio, fin).toMillis(), null,
                        "Poblamiento geográfico manual completado", null));
                log.info("FASE T3: poblamiento manual completado (executionId={})", executionId);
            } catch (Exception e) {
                Instant fin = Instant.now();
                tareaLogRepository.guardar(new TareaLog(
                        UUID.randomUUID(), null, executionId, fin,
                        ESTADO_ERROR, Duration.between(inicio, fin).toMillis(),
                        e.getClass().getSimpleName(), e.getMessage(), null));
                log.error("FASE T3: poblamiento manual falló (executionId={})", executionId, e);
            } finally {
                enCurso.set(false);
            }
        });
        return executionId;
    }

    // [QUÉ]: Indica si hay una ejecución manual en curso (para validaciones previas).
    public boolean estaEnCurso() {
        return enCurso.get();
    }
}
