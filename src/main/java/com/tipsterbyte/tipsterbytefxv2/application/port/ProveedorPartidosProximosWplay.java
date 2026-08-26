// ─────────────────────────────────────────────
// [QUÉ]: Puerto que expone la obtención de partidos próximos de Wplay con sus datos
//        crudos (nombres de equipos, fecha, cuotas), sin resolver contra la plantilla.
// [POR QUÉ]: HU-14 AC4.2/4.3 — el caso de uso necesita los nombres de equipo crudos
//            de Wplay para resolverlos contra la plantilla (cascada exacto→difuso→alias)
//            y crear partidos faltantes. ProveedorCuotas solo devuelve cuotas por
//            partidoId ya resuelto; este puerto entrega el nivel intermedio.
// [ALTERNATIVAS]: Modificar ProveedorCuotas para devolver más datos; se descarta porque
//                 rompe el contrato existente de CU-03 y acopla el puerto a Wplay.
// [RELACIONES]: Implementado por WplayCuotasAdapter (infrastructure); consumido por
//               SincronizarCuotasUseCase (AC4.2/4.3).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PartidoWplay;

import java.util.List;
import java.util.UUID;

public interface ProveedorPartidosProximosWplay {

    // [QUÉ]: Obtiene los partidos próximos de Wplay para una temporada, con datos crudos
    //        (nombres de equipos, fecha, cuotas) sin resolver contra la plantilla.
    List<PartidoWplay> obtenerPartidosProximos(UUID temporadaId);
}
