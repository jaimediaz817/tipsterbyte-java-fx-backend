/**
 * [QUÉ]: Subpaquete de puertos: interfaces que declaran lo que la aplicación
 *        necesita del exterior. Repository (persistencia) y ProveedorXxx (externo).
 * [POR QUÉ]: Aplican la Dependency Inversion: infrastructure implementa estas
 *            interfaces; application/domain solo las consumen.
 * [RELACIONES]: Implementados por infrastructure.adapter y infrastructure.persistence.
 */
package com.tipsterbyte.tipsterbytefxv2.application.port;