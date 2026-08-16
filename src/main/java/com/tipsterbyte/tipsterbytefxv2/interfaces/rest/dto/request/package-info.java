/**
 * [QUÉ]: DTOs de request de la API REST: la forma en que los clientes (frontend Angular,
 *        herramientas externas) envían datos a los endpoints.
 * [POR QUÉ]: Validan la estructura de entrada con Bean Validation (@NotNull, @NotBlank,
 *            etc.) en la capa interfaces, desacoplada de los comandos de application
 *            y de los VOs del dominio. Si la validación falla, el GlobalExceptionHandler
 *            traduce a 400 BAD_REQUEST con detalles por campo.
 * [ESTÁNDAR]: Cada request DTO se mapea a un comando de application o a un VO de
 *             dominio dentro del controller. Nunca se usa un request DTO más allá de
 *             la capa interfaces.
 * [RELACIONES]: Consumidos por los controllers en interfaces.rest.controller; mapeados
 *               a application.dto o domain.model.
 */
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request;