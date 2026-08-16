/**
 * [QUÉ]: DTOs de respuesta de la API REST: vista HTTP de resultados de éxito y el
 *        formato estándar de error (ApiError).
 * [POR QUÉ]: Aíslan a los clientes HTTP de los aggregates de dominio. Toda respuesta
 *            de éxito usa un DTO propio de esta capa (nunca expone application.dto
 *            ni domain directamente). Los errores usan siempre ApiError con schema
 *            unificado: {timestamp, status, error, mensaje, path}.
 * [ESTÁNDAR DE ÉXITO]:
 *   - 200 → ResponseEntity.ok(dto) o ResponseEntity.ok(lista)
 *   - 201 → ResponseEntity.status(CREATED).body(dto) con Location header cuando aplica
 *   - 204 → ResponseEntity.noContent() para operaciones que no devuelven representación
 * [ESTÁNDAR DE ERROR]:
 *   - 400 → Validación de request (@Valid, query params, JSON malformado)
 *   - 401 → No autenticado (JWT ausente/inválido) → ApiError
 *   - 403 → Rol insuficiente → ApiError
 *   - 422 → Violación de regla de negocio (DomainException / BR-xx)
 *   - 500 → Error interno no esperado → ApiError
 * [RELACIONES]: Producidos por los controllers; mapeados desde application.dto o
 *               aggregates de dominio. ApiError es consumido por GlobalExceptionHandler
 *               y por los handlers de seguridad (ApiErrorAuthenticationEntryPoint,
 *               ApiErrorAccessDeniedHandler).
 */
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;