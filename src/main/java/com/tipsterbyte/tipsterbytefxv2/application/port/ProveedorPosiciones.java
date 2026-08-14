// ─────────────────────────────────────────────
// [QUÉ]: Puerto de dominio que expone la obtención de la tabla de posiciones
//        de una liga desde fuentes externas.
// [POR QUÉ]: Abstrae a los proveedores de posiciones (football-data.org,
//            API-Football). Cambiar de proveedor = agregar/ajustar un adapter,
//            sin tocar casos de uso ni dominio.
// [ALTERNATIVAS]: Llamar directamente al cliente HTTP de un proveedor desde
//                 application; se descarta porque acopla la app a una API.
// [RELACIONES]: Implementado por infrastructure.adapter; usado por CU-01.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

public interface ProveedorPosiciones {

}