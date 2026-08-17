package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.TareaProgramadaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCalendarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCatalogoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCuotasUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarPosicionesUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableScheduling
public class CatalogoScheduler {

    private final TareaProgramadaRepository tareaProgramadaRepository;
    private final SincronizarPosicionesUseCase sincronizarPosicionesUseCase;
    private final SincronizarCalendarioUseCase sincronizarCalendarioUseCase;
    private final SincronizarCuotasUseCase sincronizarCuotasUseCase;
    private final SincronizarCatalogoUseCase sincronizarCatalogoUseCase;

    // Map to avoid overlapping execution of the same task
    private final Map<UUID, Boolean> runningTasks = new ConcurrentHashMap<>();

    @Autowired
    public CatalogoScheduler(TareaProgramadaRepository tareaProgramadaRepository,
                             SincronizarPosicionesUseCase sincronizarPosicionesUseCase,
                             SincronizarCalendarioUseCase sincronizarCalendarioUseCase,
                             SincronizarCuotasUseCase sincronizarCuotasUseCase,
                             SincronizarCatalogoUseCase sincronizarCatalogoUseCase) {
        this.tareaProgramadaRepository = tareaProgramadaRepository;
        this.sincronizarPosicionesUseCase = sincronizarPosicionesUseCase;
        this.sincronizarCalendarioUseCase = sincronizarCalendarioUseCase;
        this.sincronizarCuotasUseCase = sincronizarCuotasUseCase;
        this.sincronizarCatalogoUseCase = sincronizarCatalogoUseCase;
    }

    // Runs every minute (adjustable via property)
    @Scheduled(fixedDelayString = "${scheduler.interval:60000}")
    public void ejecutarTareasProgramadas() {
        List<com.tipsterbyte.tipsterbytefxv2.domain.model.TareaProgramada> tareas = tareaProgramadaRepository.listarPorPrioridadAsc();
        ZonedDateTime now = ZonedDateTime.now();

        for (com.tipsterbyte.tipsterbytefxv2.domain.model.TareaProgramada tarea : tareas) {
            if (!tarea.activa()) {
                continue;
            }
            UUID id = tarea.id();
            // Skip if already running
            if (runningTasks.computeIfAbsent(id, k -> Boolean.FALSE)) {
                continue;
            }
            try {
                String cron = tarea.cronExpression();
                if (cron == null || cron.isBlank()) {
                    continue;
                }
                CronExpression cronExpression = new CronExpression(cron);
                if (cronExpression.isSatisfiedBy(now)) {
                    // Mark as running
                    runningTasks.put(id, true);
                    // Determine which use case to invoke based on task name (nombre)
                    String nombre = tarea.nombre().toLowerCase();
                    if (nombre.contains("posicion")) {
                        sincronizarPosicionesUseCase.ejecutar();
                    } else if (nombre.contains("calendario")) {
                        sincronizarCalendarioUseCase.ejecutar();
                    } else if (nombre.contains("cuota")) {
                        sincronizarCuotasUseCase.ejecutar();
                    } else if (nombre.contains("catalogo")) {
                        sincronizarCatalogoUseCase.ejecutar();
                    } else {
                        // Default: do nothing
                    }
                }
            } catch (Exception e) {
                // Log error (in real app use logger)
                System.err.println("Error processing task " + id + ": " + e.getMessage());
            } finally {
                runningTasks.put(id, false);
            }
        }
    }
}