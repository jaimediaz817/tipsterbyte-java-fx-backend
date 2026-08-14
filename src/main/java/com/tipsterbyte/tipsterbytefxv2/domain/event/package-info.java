/**
 * [QUÉ]: Subpaquete de eventos de dominio (LigaActivada, PartidoProgramado,
 *        CuotaActualizada, PronosticoPublicado, SuscripcionCreada).
 * [POR QUÉ]: Comunican hechos relevantes del negocio a otros componentes
 *            (notificaciones, cache, ingesta) sin acoplarlos.
 * [RELACIONES]: Se emiten desde domain.model; se consumirán en FASE 13 (RabbitMQ).
 */
package com.tipsterbyte.tipsterbytefxv2.domain.event;