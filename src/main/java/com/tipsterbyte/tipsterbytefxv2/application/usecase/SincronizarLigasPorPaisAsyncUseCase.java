// ─────────────────────────────────────────────
// [QUÉ]: Wrapper async de CU-18 (HU-12): poblar ligas por país en background con
//        TareaLog + anti-solapamiento por isoAlpha2.
// [POR QUÉ]: Aunque sea un solo país, #5 + temporadas + #6 por liga puede tardar
//            30s-3min (ENG/ESP con 20 ligas). Síncrono provocaría TimeoutError en el
//            frontend (geografia-api.service timeout 60s). Reutiliza el patrón FASE T3:
//            hilos virtuales + TareaLog RUNNING/SUCCESS/ERROR y polling vía
//            GET /catalogo/activar/{executionId}. Anti-solapamiento por país permite
//            poblar COL y ARG en paralelo, pero bloquea dos corridas de COL a la vez (409).
// [ALTERNATIVAS]: Síncrono con timeout largo; se descarta por spinner bloqueado.
// [RELACIONES]: HU-12 → CU-18 async → SincronizarLigasPorPaisUseCase + TareaLogRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.TareaLogRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.PoblamientoEnCursoException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SincronizarLigasPorPaisAsyncUseCase {

    public static final String ESTADO_RUNNING = SincronizarCatalogoAsyncUseCase.ESTADO_RUNNING;
    public static final String ESTADO_SUCCESS = SincronizarCatalogoAsyncUseCase.ESTADO_SUCCESS;
    public static final String ESTADO_ERROR = SincronizarCatalogoAsyncUseCase.ESTADO_ERROR;

    private static final Logger log = LoggerFactory.getLogger(SincronizarLigasPorPaisAsyncUseCase.class);

    private final SincronizarLigasPorPaisUseCase delegado;
    private final TareaLogRepository tareaLogRepository;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ConcurrentHashMap<String, AtomicBoolean> enCursoPorPais = new ConcurrentHashMap<>();

    public SincronizarLigasPorPaisAsyncUseCase(SincronizarLigasPorPaisUseCase delegado,
                                               TareaLogRepository tareaLogRepository) {
        this.delegado = delegado;
        this.tareaLogRepository = tareaLogRepository;
    }

    // [QUÉ]: Lanza el poblamiento del país en background y devuelve executionId.
    public String ejecutarAsync(String isoAlpha2Raw) {
        String iso = isoAlpha2Raw.trim().toUpperCase();
        AtomicBoolean flag = enCursoPorPais.computeIfAbsent(iso, k -> new AtomicBoolean(false));
        if (!flag.compareAndSet(false, true)) {
            throw new PoblamientoEnCursoException(
                    "Ya hay un poblamiento en curso para el país " + iso + "; espera a que termine");
        }
        String executionId = UUID.randomUUID().toString();
        Instant inicio = Instant.now();
        tareaLogRepository.guardar(new TareaLog(
                UUID.randomUUID(), null, executionId, inicio,
                ESTADO_RUNNING, null, null,
                "Poblamiento de ligas en curso para " + iso));

        log.info("HU-12 async: poblamiento de ligas iniciado iso={} executionId={}", iso, executionId);

        executor.submit(() -> {
            try {
                var resultado = delegado.ejecutar(iso);
                Instant fin = Instant.now();
                tareaLogRepository.guardar(new TareaLog(
                        UUID.randomUUID(), null, executionId, fin,
                        ESTADO_SUCCESS, Duration.between(inicio, fin).toMillis(), null,
                        "Ligas pobladas para " + iso + ": " + resultado.ligasCreadas() + " nuevas, total " + resultado.totalLigasPais()));
                log.info("HU-12 async: completado iso={} executionId={}", iso, executionId);
            } catch (Exception e) {
                Instant fin = Instant.now();
                tareaLogRepository.guardar(new TareaLog(
                        UUID.randomUUID(), null, executionId, fin,
                        ESTADO_ERROR, Duration.between(inicio, fin).toMillis(),
                        e.getClass().getSimpleName(), e.getMessage()));
                log.error("HU-12 async: fallo iso={} executionId={}", iso, executionId, e);
            } finally {
                flag.set(false);
            }
        });
        return executionId;
    }
}
