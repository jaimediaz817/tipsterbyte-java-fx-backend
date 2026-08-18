// ─────────────────────────────────────────────
// [QUÉ]: Puerto de consulta del estado de ejecución de las tareas programadas.
// [POR QUÉ]: El scheduler mantiene en memoria qué tareas están corriendo
//            (anti-solapamiento); exponerlo vía puerto permite a la capa interfaces
//            (controller) informar "en ejecución" sin acoplar el controller a
//            infraestructura (regla de dependencias).
// [ALTERNATIVAS]: Consultar el TareaLog (se escribe al terminar); se descarta porque
//                 no refleja las ejecuciones en curso.
// [RELACIONES]: Implementado por CatalogoScheduler (infrastructure.adapter); consumido
//               por TareaProgramadaController → GET /tareas-programadas/ejecucion.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import java.util.Set;
import java.util.UUID;

public interface EstadoEjecucionTareas {

    // [QUÉ]: Devuelve los ids de las tareas programadas que están corriendo ahora mismo.
    Set<UUID> tareasEnEjecucion();
}