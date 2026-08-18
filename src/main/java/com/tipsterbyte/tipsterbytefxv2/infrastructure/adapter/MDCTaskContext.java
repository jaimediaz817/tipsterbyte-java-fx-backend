package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * ─────────────────────────────────────────────
 * [QUÉ]: Utilidad para enriquecer el MDC (Mapped Diagnostic Context) con
 *        información de la tarea programada actualmente en ejecución.
 * [POR QUÉ]: Permite que los logs estructurados (JSON) incluyan taskName,
 *            executionId, ligaId, etc., facilitando la correlación en sistemas
 *            de logging externos (ELK/Loki/Grafana).
 * [ALTERNATIVAS]: Insertar manualmente MDC en cada punto de log; se descarta
 *                 porque es propenso a olvidos y dificulta la limpieza.
 * [RELACIONES]: Usado por CatalogoScheduler antes y después de ejecutar una
 *                tarea programada.
 * ─────────────────────────────────────────────
 */
public class MDCTaskContext {

    private static final String KEY_TASK_NAME = "taskName";
    private static final String KEY_EXECUTION_ID = "executionId";
    private static final String KEY_LIGA_ID = "ligaId";

    /**
     * Inserta en el MDC el contexto de la tarea a ejecutar.
     * Debe llamarse antes de ejecutar la tarea y limpiar con {@code clear()} en finally.
     *
     * @param tarea La tarea programada que se va a ejecutar.
     * @param executionId Un identificador único para esta ejecución (ej: UUID).
     */
    public static void putTaskContext(TareaProgramada tarea, String executionId) {
        clear(); // Limpiar por si hubiera restos de ejecuciones anteriores
        MDC.put(KEY_TASK_NAME, 
                tarea.tipoFuente() != null ? tarea.tipoFuente().name() : "GLOBAL");
        MDC.put(KEY_EXECUTION_ID, executionId);
        MDC.put(KEY_LIGA_ID, 
                tarea.ligaId() != null ? tarea.ligaId().toString() : "null");
    }

    /**
     * Elimina las claves de contexto de tarea del MDC para evitar fugas entre hilos.
     */
    public static void clear() {
        MDC.remove(KEY_TASK_NAME);
        MDC.remove(KEY_EXECUTION_ID);
        MDC.remove(KEY_LIGA_ID);
    }
}
