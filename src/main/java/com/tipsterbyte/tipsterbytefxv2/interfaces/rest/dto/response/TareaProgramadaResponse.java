// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de tarea programada: vista del frontend con los datos de la
//        tarea, la próxima ejecución derivada de su cron (reloj en cuenta regresiva),
//        y primerDisparo (HU-14 AC3) para mostrar el delay programado.
// [POR QUÉ]: El frontend de "Tareas programadas" necesita conocer la próxima ejecución
//            sin calcular crons; el backend la deriva con la misma librería del
//            scheduler (CronExpression) para evitar discrepancias. primerDisparo se
//            expone para que el frontend muestre "inicia el mar 18:00".
// [ALTERNATIVAS]: Calcular en el frontend con cronstrue; se descarta porque el backend
//                 garantiza coherencia con el dispatcher real.
// [RELACIONES]: TareaProgramadaController GET /api/v1/tareas-programadas →
//               GestionarTareasProgramasUseCase + CronExpression (Spring);
//               HU-14 AC3 → primerDisparo.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;

import java.util.UUID;

public record TareaProgramadaResponse(
        UUID id,
        UUID ligaId,
        String ligaNombre,
        TipoFuenteExtraccion tipoFuente,
        String prioridad,
        String cronExpression,
        boolean activa,
        boolean fuenteActiva,
        String createdAt,
        String primerDisparo,
        String nextExecution) {
}