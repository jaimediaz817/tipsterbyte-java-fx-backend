// ─────────────────────────────────────────────
// [QUÉ]: Puerto de dominio que expone la obtención del calendario de partidos
//        (jugados y pendientes) de una liga desde fuentes externas.
// [POR QUÉ]: Abstrae a los proveedores de calendario (API-Football,
//            football-data.org). Aísla a application del formato concreto de cada API.
// [ALTERNATIVAS]: RestTemplate/WebClient directo en application; se descarta por
//                 acoplamiento a infraestructura.
// [RELACIONES]: Implementado por infrastructure.adapter; usado por CU-02.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PartidoFuente;

import java.util.List;
import java.util.UUID;

public interface ProveedorCalendario {

    // [QUÉ]: Obtiene el calendario de partidos (jugados y pendientes) de una liga.
    // [POR QUÉ]: El caso de uso CU-02 la usa para sincronizar partidos sin conocer
    //            el formato concreto de cada API.
    // [RELACIONES]: CU-02. Devuelve DTOs de fuente que el caso de uso mapea al dominio.
    List<PartidoFuente> obtenerCalendario(UUID ligaId);

}