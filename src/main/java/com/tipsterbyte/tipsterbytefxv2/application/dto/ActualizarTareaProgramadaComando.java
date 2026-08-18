// ─────────────────────────────────────────────
// [QUÉ]: Comando de actualización de tarea programada (CU-15): permite pausar/reanudar
//        (activa), cambiar frecuencia/cron y ajustar prioridad. Todos los campos son
//        opcionales: solo se aplican los que vienen presentes.
// [POR QUÉ]: Un único PUT cubre edición de periodo (frecuencia amigable o cron crudo),
//            pausa (activa=false) y reanudación (activa=true) sin borrar la tarea.
// [ALTERNATIVAS]: Endpoints separados (PUT activa, PUT cron); se descartan porque
//                 fragmentan la edición del formulario.
// [RELACIONES]: CU-15 → GestionarTareasProgramasUseCase.actualizar(id, comando).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Frecuencia;

public record ActualizarTareaProgramadaComando(
        String cron,
        Frecuencia frecuencia,
        Boolean activa,
        String prioridad) {
}