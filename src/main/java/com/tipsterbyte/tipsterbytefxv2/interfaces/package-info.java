/**
 * [QUÉ]: Capa de interfaces. Contiene los controllers REST, DTOs de request/response
 *        y los exception handlers globales.
 * [POR QUÉ]: Es el límite con el mundo exterior (HTTP). Traduce peticiones HTTP a
 *            llamadas de application y respuestas HTTP a partir de sus resultados.
 * [ALTERNATIVAS]: Acoplar controllers a dominio; se descarta porque exponería
 *                 entidades y reglas internas a través de la API.
 * [RELACIONES]: Depende de application y domain. No conoce infrastructure.
 */
package com.tipsterbyte.tipsterbytefxv2.interfaces;