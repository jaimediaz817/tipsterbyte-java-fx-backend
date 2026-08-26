// ─────────────────────────────────────────────
// [QUÉ]: Comando de actualización de tarea programada (CU-15): permite pausar/reanudar
//        (activa), cambiar frecuencia/cron, ajustar prioridad y limpiar/actualizar
//        primerDisparo. Todos los campos son opcionales: solo se aplican los que
//        vienen presentes.
// [POR QUÉ]: Un único PUT cubre edición de periodo (frecuencia amigable o cron crudo),
//            pausa (activa=false) y reanudación (activa=true) sin borrar la tarea.
//            Enviar primerDisparo=null limpi el delay (reanuda según cron).
// [ALTERNATIVAS]: Endpoints separados (PUT activa, PUT cron); se descartan porque
//                 fragmentan la edición del formulario.
// [RELACIONES]: CU-15 → GestionarTareasProgramasUseCase.actualizar(id, comando);
//               HU-14 AC3 → primerDisparo (null = limpiar postergación).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Frecuencia;

import java.time.Instant;

public record ActualizarTareaProgramadaComando(
        String cron,
        Frecuencia frecuencia,
        Boolean activa,
        String prioridad,
        Instant primerDisparo) {
}