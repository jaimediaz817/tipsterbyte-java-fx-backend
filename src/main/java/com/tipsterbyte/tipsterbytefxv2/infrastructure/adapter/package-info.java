/**
 * [QUÉ]: Subpaquete de adapters hacia las 4 APIs externas: football-data.org,
 *        API-Football, The Odds API y SharpAPI.
 * [POR QUÉ]: Implementan ProveedorPosiciones, ProveedorCalendario y ProveedorCuotas.
 *            Cambiar de proveedor = ajustar un adapter, sin tocar dominio ni use cases.
 * [RELACIONES]: Implementa application.port.ProveedorXxx (FASE 8+).
 */
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;