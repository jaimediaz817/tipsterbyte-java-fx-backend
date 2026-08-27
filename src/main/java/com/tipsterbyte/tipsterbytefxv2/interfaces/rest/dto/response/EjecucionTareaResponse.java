// ─────────────────────────────────────────────
// [QUÉ]: DTO de respuesta del endpoint POST /tareas-programadas/{id}/ejecutar.
//        Indica que la tarea fue aceptada y se está ejecutando en background,
//        junto al executionId para correlacionar con los logs.
// [POR QUÉ]: El caso de uso es asíncrono (hilo virtual): el controller debe
//            retornar 202 Accepted inmediatamente, sin esperar a que termine.
//            El executionId permite al frontend pollear los logs y saber
//            cuándo terminó.
// [RELACIONES]: CatalogoScheduler.ejecutarTarea() genera el executionId y lo
//               persiste en tarea_log.execution_id.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import java.util.UUID;

public record EjecucionTareaResponse(
        UUID executionId,
        String mensaje
) {
}
