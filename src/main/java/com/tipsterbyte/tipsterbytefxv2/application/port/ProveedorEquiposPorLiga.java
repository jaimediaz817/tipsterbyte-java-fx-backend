// ─────────────────────────────────────────────
// [QUÉ]: Puerto de la fuente de poblamiento #6 (ext-soccerway-teams-by-league):
//        obtiene la plantilla oficial de equipos de una liga (nombre + escudo).
// [POR QUÉ]: Los equipos deben existir ANTES de activar las fuentes operativas
//            (#2/#3/#4): es el paso 2 del poblamiento geográfico (HU-11). A diferencia
//            de las fuentes operativas, NO usa path_to_scrape ni URL asociada: se
//            consume con country_name + league_name, datos que ya viven en el
//            aggregate Liga.
// [ALTERNATIVAS]: Seguir dependiendo del subproducto #3/#4 para conocer equipos;
//                 se descarta porque no hay fuente canónica y el matching por nombre
//                 creaba duplicados entre fuentes.
// [RELACIONES]: HU-11 → CU-10 (encadenado). Implementado por SoccerwayEquiposAdapter,
//               decorado por ProveedorEquiposCacheable (cache-aside Redis).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.application.dto.EquipoFuente;

import java.util.List;

public interface ProveedorEquiposPorLiga {

    // [QUÉ]: Devuelve los equipos de la liga indicada (nombre + logoUrl).
    // [POR QUÉ]: La plantilla canónica alimenta la temporada vigente durante el poblamiento.
    List<EquipoFuente> obtenerEquipos(String countryName, String leagueName);
}
