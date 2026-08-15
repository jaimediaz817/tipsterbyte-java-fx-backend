// ─────────────────────────────────────────────
// [QUÉ]: Puerto de dominio que expone la obtención del catálogo de países desde
//        la fuente externa #1 (ext-soccerway-countries).
// [POR QUÉ]: Abstrae el proveedor de países (proyecto Python de extracción).
//            Cambiar de fuente = ajustar el adapter, sin tocar casos de uso ni dominio.
// [ALTERNATIVAS]: Llamar directamente al cliente HTTP desde application; se descarta
//                 porque acopla la app a una API concreta.
// [RELACIONES]: Implementado por infrastructure.adapter (SoccerwayPaisesAdapter); usado por CU-10.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;

import java.util.List;

public interface ProveedorPaises {

    // [QUÉ]: Obtiene todos los países con ligas registradas (fuente #1).
    // [POR QUÉ]: El caso de uso CU-10 los usa para poblar el catálogo de países.
    // [RELACIONES]: CU-10. Devuelve DTOs de fuente que el caso de uso mapea al dominio.
    List<PaisFuente> obtenerPaises();

}