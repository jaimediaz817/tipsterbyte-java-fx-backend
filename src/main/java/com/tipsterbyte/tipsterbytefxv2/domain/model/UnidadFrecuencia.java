// ─────────────────────────────────────────────
// [QUÉ]: Unidad de tiempo para expresar una frecuencia de forma amigable
//        (cada N segundos/minutos/horas/días) en el editor del frontend.
// [POR QUÉ]: La UI de "Tareas programadas" permite elegir el periodo en unidades
//            humanas; internamente se codifica a una expresión cron de 6 segmentos.
// [ALTERNATIVAS]: Solo crons crudos; se descarta porque el usuario pidió un selector
//                 amigable (cada cuantos días/horas/minutos/segundos).
// [RELACIONES]: Frecuencia (domain.model) → CronExpression (6 segmentos Spring).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

public enum UnidadFrecuencia {
    SEGUNDOS,
    MINUTOS,
    HORAS,
    DIAS
}