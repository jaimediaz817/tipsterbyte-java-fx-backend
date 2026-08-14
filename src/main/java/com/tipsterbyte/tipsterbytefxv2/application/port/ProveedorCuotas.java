// ─────────────────────────────────────────────
// [QUÉ]: Puerto de dominio que expone la obtención de cuotas de partidos próximos
//        desde fuentes externas de odds.
// [POR QUÉ]: Abstrae a los proveedores de cuotas (API-Football, The Odds API,
//            SharpAPI). Es el puerto con más adapters, demostrando que cambiar de
//            casa de apuestas no impacta el núcleo.
// [ALTERNATIVAS]: Acoplar el dominio a un modelo de odds de una API específica;
//                 se descarta porque cada proveedor modela las cuotas distinto.
// [RELACIONES]: Implementado por infrastructure.adapter; usado por CU-03 y CU-06.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

public interface ProveedorCuotas {

}