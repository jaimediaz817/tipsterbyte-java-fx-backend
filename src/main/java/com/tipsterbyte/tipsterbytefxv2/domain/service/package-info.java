/**
 * [QUÉ]: Subpaquete de servicios de dominio: reglas que no pertenecen naturalmente
 *        a una entity o value object (ej: CalculadoraPosiciones).
 * [POR QUÉ]: Mantiene el dominio cohesionado; solo se crean cuando una regla no
 *            encaja en un aggregate (regla DDD del skill ddd-domain-modeling).
 * [RELACIONES]: Usa domain.model; es invocado por application.usecase.
 */
package com.tipsterbyte.tipsterbytefxv2.domain.service;