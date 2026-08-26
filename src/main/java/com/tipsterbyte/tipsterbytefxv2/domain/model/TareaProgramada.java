// ─────────────────────────────────────────────
// [QUÉ]: Registro de dominio que representa una tarea programada: una sincronización
//        automática de una fuente (por tipo) para una liga concreta, ejecutada según
//        una frecuencia (cronExpression).
// [POR QUÉ]: Es la unidad de trabajo del scheduler (CatalogoScheduler): cada tarea
//            define QUÉ fuente se ejecuta, CADA CUÁNTO (cron) y QUÉ liga afecta.
//            El primerDisparo (HU-14 AC3) permite postergar la primera ejecución.
// [ALTERNATIVAS]: Entidad JPA en dominio; se descarta porque el dominio es un POJO puro.
// [RELACIONES]: CU-15 (GestionarTareasProgramasUseCase) + CatalogoScheduler;
//               respaldado por tareas_programadas (JPA entity).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TareaProgramada(
        UUID id,
        UUID ligaId,
        TipoFuenteExtraccion tipoFuente,
        String prioridad,
        String cronExpression,
        boolean activa,
        String createdAt,
        Instant primerDisparo) {
}