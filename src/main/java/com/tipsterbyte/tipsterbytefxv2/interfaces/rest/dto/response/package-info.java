/**
 * [QUÉ]: DTOs de respuesta de la API: la vista HTTP de los resultados de los casos de
 *        uso (sincronizaciones, suscripción) y el formato estándar de error.
 * [POR QUÉ]: Aíslan a los clientes HTTP de los aggregates de dominio y normalizan el
 *            contrato de error (ApiError) en toda la API.
 * [RELACIONES]: Producidos por los controllers; algunos reutilizan application.dto.
 */
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;