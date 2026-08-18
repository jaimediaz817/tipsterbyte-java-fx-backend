package com.tipsterbyte.tipsterbytefxv2.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * ─────────────────────────────────────────────
 * [QUÉ]: Registro de ejecución de una tarea programada.
 * [POR QUÉ]: Necesario para almacenar historial de ejecuciones y permitir
 *            correlación con logs estructurados mediante executionId.
 * [ALTERNATIVAS]: No persistir logs (solo en memoria) o usar un sistema
 *                 externo de logging (ELK) sin trazabilidad a la BD.
 * [RELACIONES]: CU-15 (GestionarTareasProgramasUseCase) → TareaLogRepository.
 *                También relacionado con CatalogoScheduler (infrastructure)
 *                que persiste cada ejecución.
 * ─────────────────────────────────────────────
 */
public record TareaLog(
        UUID id,
        UUID tareaProgramadaId,
        String executionId,
        Instant timestamp,
        String status, // SUCCESS, ERROR
        Long durationMs,
        String errorCode,
        String mensaje
) {
}
