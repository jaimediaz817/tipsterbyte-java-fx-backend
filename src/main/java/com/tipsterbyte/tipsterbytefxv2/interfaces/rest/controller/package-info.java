/**
 * [QUÉ]: Controllers REST de la API: ligas, partidos, pronósticos y suscripciones.
 * [POR QUÉ]: Son la puerta de entrada HTTP de los casos de uso CU-01..09. Traducen
 *            request DTOs a comandos de application y respuestas a DTOs HTTP.
 *            (La anotación @RestController se agrega en FASE 8 con el wiring de beans.)
 * [RELACIONES]: CU-01..09; depende de application.usecase y application.dto.
 */
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;