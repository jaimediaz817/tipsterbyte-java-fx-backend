// ─────────────────────────────────────────────
// [QUÉ]: Tipo de fuente externa de la que se extraen datos de una liga.
//        Conjunto cerrado que refleja los 3 endpoints reales de extracción.
// [POR QUÉ]: Cada liga se sincroniza contra 3 fuentes distintas (posiciones,
//            calendario, cuotas), y cada una tiene su propia URL. El enum limita
//            los tipos válidos y coincide con el enum del proyecto Python.
// [ALTERNATIVAS]: Strings sueltos; se descartan porque un tipo inválido rompería
//                 la resolución de URL de los adapters sin error en compilación.
// [RELACIONES]: Usado por FuenteExtraccion y DetalleFuenteExtraccion (FASE 8.5);
//               por los adapters WplayCuotasAdapter, FlashscorePosicionesAdapter,
//               SoccerwayCalendarioAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

public enum TipoFuenteExtraccion {

    // [QUÉ]: Tabla de posiciones (endpoint #3, Flashscore).
    STANDINGS,
    // [QUÉ]: Cuotas de apuestas Wplay (endpoint #2).
    ODDS_WPLAY,
    // [QUÉ]: Calendario de partidos (endpoint #4, Soccerway).
    CALENDAR,
    // [QUÉ]: Plantilla de equipos con escudos (endpoint #6, H-07). A diferencia de las
    //        operativas NO usa path_to_scrape ni DetalleFuenteExtraccion: se consume con
    //        country_name + league_name del aggregate. Solo aplica a tareas por liga.
    EQUIPOS;
}
