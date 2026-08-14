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

public interface ProveedorCalendario {

}