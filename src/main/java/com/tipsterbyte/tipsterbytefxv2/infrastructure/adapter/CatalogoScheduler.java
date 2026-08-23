// ─────────────────────────────────────────────
// [QUÉ]: Dispatcher de tareas programadas: cada intervalo (scheduler.interval) evalúa
//        las tareas activas, valida su expresión cron y ejecuta el caso de uso
//        correspondiente en un hilo virtual, con anti-solapamiento por tarea.
// [POR QUÉ]: FASE 12.6 requiere ejecución automática de sincronizaciones por fuente
//            sin bloquear el hilo del scheduler ni solapar la misma tarea. El MDC y la
//            tabla tarea_log dan trazabilidad (executionId) para correlacionar con los
//            logs JSON (observabilidad).
// [ALTERNATIVAS]: Ejecutar de forma síncrona en el hilo @Scheduled; se descarta porque
//                 una tarea larga retrasaría el resto. Thread pool clásico fijo; se
//                 descarta en favor de hilos virtuales (Java 21) por su coste mínimo
//                 ante tareas I/O-bound (scraping).
// [RELACIONES]: CU-15 (GestionarTareasProgramasUseCase) → TareaProgramadaRepository;
//               TareaLogRepository (persistencia de ejecuciones); MDCTaskContext
//               (enriquecimiento MDC); CU-01/02/03/10 vía los 4 use cases de
//               sincronización.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.EstadoEjecucionTareas;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaLogRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaProgramadaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCalendarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCatalogoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCuotasUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarEquiposLigaUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarPosicionesUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaProgramada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true")
@Profile("!test")
public class CatalogoScheduler implements EstadoEjecucionTareas {

    private static final Logger log = LoggerFactory.getLogger(CatalogoScheduler.class);

    private final TareaProgramadaRepository tareaProgramadaRepository;
    private final TareaLogRepository tareaLogRepository;
    private final SincronizarPosicionesUseCase sincronizarPosicionesUseCase;
    private final SincronizarCalendarioUseCase sincronizarCalendarioUseCase;
    private final SincronizarCuotasUseCase sincronizarCuotasUseCase;
    private final SincronizarEquiposLigaUseCase sincronizarEquiposLigaUseCase;
    private final SincronizarCatalogoUseCase sincronizarCatalogoUseCase;

    // Anti-solapamiento por tarea: evita que una misma tarea se ejecute dos veces en paralelo.
    private final Map<UUID, Boolean> runningTasks = new ConcurrentHashMap<>();

    // Hilos virtuales: coste mínimo para tareas I/O-bound (llamadas a fuentes externas).
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Autowired
    public CatalogoScheduler(TareaProgramadaRepository tareaProgramadaRepository,
                             TareaLogRepository tareaLogRepository,
                             SincronizarPosicionesUseCase sincronizarPosicionesUseCase,
                             SincronizarCalendarioUseCase sincronizarCalendarioUseCase,
                             SincronizarCuotasUseCase sincronizarCuotasUseCase,
                             SincronizarEquiposLigaUseCase sincronizarEquiposLigaUseCase,
                             SincronizarCatalogoUseCase sincronizarCatalogoUseCase) {
        this.tareaProgramadaRepository = tareaProgramadaRepository;
        this.tareaLogRepository = tareaLogRepository;
        this.sincronizarPosicionesUseCase = sincronizarPosicionesUseCase;
        this.sincronizarCalendarioUseCase = sincronizarCalendarioUseCase;
        this.sincronizarCuotasUseCase = sincronizarCuotasUseCase;
        this.sincronizarEquiposLigaUseCase = sincronizarEquiposLigaUseCase;
        this.sincronizarCatalogoUseCase = sincronizarCatalogoUseCase;
    }

    // [QUÉ]: Tick del scheduler: evalúa todas las tareas activas y dispara las que su
    //        cron indique que corresponden al minuto actual.
    // [POR QUÉ]: fixedDelay asegura que el siguiente tick espera a que termine el actual,
    //            evitando evaluaciones solapadas del propio dispatcher.
    @Scheduled(fixedDelayString = "${scheduler.interval:60000}")
    public void ejecutarTareasProgramadas() {
        ZonedDateTime ahora = ZonedDateTime.now();
        List<TareaProgramada> tareasActivas = tareaProgramadaRepository.listarPorPrioridadAsc().stream()
                .filter(TareaProgramada::activa)
                .toList();

        for (TareaProgramada tarea : tareasActivas) {
            // Anti-solapamiento: si la tarea ya está en ejecución, se omite en este tick.
            if (Boolean.TRUE.equals(runningTasks.get(tarea.id()))) {
                continue;
            }
            if (!cronCoincide(tarea.cronExpression(), ahora)) {
                continue;
            }

            runningTasks.put(tarea.id(), true);
            CompletableFuture.runAsync(() -> ejecutarTarea(tarea), executor);
        }
    }

    // [QUÉ]: Valida y compara la expresión cron de la tarea contra el instante actual.
    // [POR QUÉ]: Una cron mal formada no debe romper el dispatcher completo: se ignora
    //            la tarea y se registra para diagnóstico. Spring 7 expone next(Temporal)
    //            (ya no matches): la cron dispara en el minuto actual si su próxima
    //            ejecución tras el minuto anterior cae dentro de este minuto.
    private boolean cronCoincide(String cronExpression, ZonedDateTime ahora) {
        if (cronExpression == null || cronExpression.isBlank()) {
            return false;
        }
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            ZonedDateTime minutoActual = ahora.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
            ZonedDateTime proxima = cron.next(minutoActual.minusSeconds(1));
            return proxima != null
                    && proxima.truncatedTo(java.time.temporal.ChronoUnit.MINUTES).equals(minutoActual);
        } catch (IllegalArgumentException e) {
            log.warn("Cron inválido en tarea programada: {}", cronExpression);
            return false;
        }
    }

    // [QUÉ]: Devuelve los ids de las tareas que están en ejecución en este momento.
    // [POR QUÉ]: Expone el mapa de anti-solapamiento al puerto EstadoEjecucionTareas para
    //            que el controller pueda marcar "en ejecución" en la UI.
    @Override
    public Set<UUID> tareasEnEjecucion() {
        return runningTasks.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    // [QUÉ]: Ejecuta la tarea en su propio hilo: enriquece el MDC, despacha al caso de
    //        uso según tipoFuente, persiste el TareaLog (éxito o error) y limpia estado.
    private void ejecutarTarea(TareaProgramada tarea) {
        String executionId = UUID.randomUUID().toString();
        Instant inicio = Instant.now();
        MDCTaskContext.putTaskContext(tarea, executionId);
        try {
            despachar(tarea);
            Instant fin = Instant.now();
            long duracionMs = Duration.between(inicio, fin).toMillis();
            tareaLogRepository.guardar(new TareaLog(
                    UUID.randomUUID(),
                    tarea.id(),
                    executionId,
                    inicio,
                    "SUCCESS",
                    duracionMs,
                    null,
                    "Ejecución exitosa de tarea programada"
            ));
        } catch (Exception e) {
            Instant fin = Instant.now();
            long duracionMs = Duration.between(inicio, fin).toMillis();
            tareaLogRepository.guardar(new TareaLog(
                    UUID.randomUUID(),
                    tarea.id(),
                    executionId,
                    inicio,
                    "ERROR",
                    duracionMs,
                    e.getClass().getSimpleName(),
                    e.getMessage()
            ));
            // El MDC ya está enriquecido: este log aparece en los logs JSON correlacionado.
            log.error("Error ejecutando tarea programada {}", tarea.id(), e);
        } finally {
            MDCTaskContext.clear();
            runningTasks.put(tarea.id(), false);
        }
    }

    // [QUÉ]: Despacha la tarea al caso de uso correspondiente según su tipo de fuente.
    //        tipoFuente null = tarea global de catálogo (CU-10, sin liga).
    private void despachar(TareaProgramada tarea) {
        if (tarea.tipoFuente() == null) {
            sincronizarCatalogoUseCase.ejecutar();
            return;
        }
        if (tarea.ligaId() == null) {
            throw new IllegalArgumentException("Tarea de tipo " + tarea.tipoFuente()
                    + " requiere ligaId para la liga " + tarea.id());
        }
        switch (tarea.tipoFuente()) {
            case STANDINGS -> sincronizarPosicionesUseCase.ejecutar(tarea.ligaId());
            case CALENDAR -> sincronizarCalendarioUseCase.ejecutar(tarea.ligaId());
            case ODDS_WPLAY -> sincronizarCuotasUseCase.ejecutar(tarea.ligaId());
            // [POR QUÉ]: EQUIPOS no usa path_to_scrape ni DetalleFuenteExtraccion (H-07):
            //            la #6 se consume con country_name+league_name del aggregate.
            case EQUIPOS -> sincronizarEquiposLigaUseCase.ejecutar(tarea.ligaId());
            default -> throw new IllegalArgumentException(
                    "Tipo de fuente no soportado: " + tarea.tipoFuente());
        }
    }
}