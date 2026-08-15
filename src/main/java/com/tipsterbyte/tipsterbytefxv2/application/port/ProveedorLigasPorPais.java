// ─────────────────────────────────────────────
// [QUÉ]: Puerto de dominio que expone la obtención de ligas por país desde la
//        fuente externa #5 (ext-soccerway-leagues-by-country).
// [POR QUÉ]: Abstrae el proveedor de ligas de catálogo (proyecto Python de extracción).
//            Cambiar de fuente = ajustar el adapter, sin tocar casos de uso ni dominio.
// [ALTERNATIVAS]: Llamar directamente al cliente HTTP desde application; se descarta
//                 porque acopla la app a una API concreta.
// [RELACIONES]: Implementado por infrastructure.adapter (SoccerwayLigasPorPaisAdapter);
//               usado por CU-10 junto con ProveedorPaises.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.application.dto.LigaFuente;

import java.util.List;

public interface ProveedorLigasPorPais {

    // [QUÉ]: Obtiene las ligas existentes de un país en Soccerway (fuente #5).
    // [POR QUÉ]: El caso de uso CU-10 las usa para poblar el catálogo de ligas por país.
    // [RELACIONES]: CU-10. Devuelve DTOs de fuente que el caso de uso mapea al dominio.
    List<LigaFuente> obtenerLigasPorPais(String countryName, int limit);

}