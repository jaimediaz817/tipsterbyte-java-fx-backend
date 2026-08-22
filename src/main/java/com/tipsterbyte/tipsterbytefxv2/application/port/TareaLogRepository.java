package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog;

import java.util.List;
import java.util.UUID;

/**
 * ─────────────────────────────────────────────
 * [QUÉ]: Puerto de persistencia para logs de ejecución de tareas programadas.
 * [POR QUÉ]: Permite al scheduler guardar logs y al controlador recuperarlos
 *            sin acoplar la capa application a JPA.
 * [ALTERNATIVAS]: Usar directamente JPA en el scheduler; se descarta porque
 *                 viola la Dependency Rule (la capa application no debe conocer
 *                 infraestructura).
 * [RELACIONES]: Implementado por TareaLogRepositoryJpaAdapter (infrastructure).
 *                Usado por CatalogoScheduler (para guardar) y
 *                TareaProgramadaController (para obtener logs).
 * ─────────────────────────────────────────────
 */
public interface TareaLogRepository {
    void guardar(TareaLog log);
    List<TareaLog> buscarPorTareaProgramadaId(UUID tareaProgramadaId);
    List<TareaLog> listarUltimas(int limite);

    // [QUÉ]: Logs de una ejecución (FASE T3), más reciente primero.
    // [POR QUÉ]: El ciclo RUNNING -> SUCCESS/ERROR registra una fila por transición con
    //            el MISMO executionId; el polling consume la más reciente (estado actual).
    List<TareaLog> buscarPorExecutionId(String executionId);
}
