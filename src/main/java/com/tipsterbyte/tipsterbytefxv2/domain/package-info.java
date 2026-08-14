/**
 * [QUÉ]: Capa de dominio. Contiene el núcleo del negocio: aggregates, entidades,
 *        value objects, reglas de negocio (BR-001..008) y eventos de dominio.
 * [POR QUÉ]: Es la capa protegida por la Dependency Rule. NO conoce Spring, JPA,
 *            ni las APIs externas. Todo lo demás depende de esta capa.
 * [ALTERNATIVAS]: Mezclar dominio con infraestructura (antipatrón común en CRUDs);
 *                 se descarta porque impediría cambiar de proveedor/BD sin tocar negocio.
 * [RELACIONES]: Se conecta con application (que lo usa) e infrastructure (que lo implementa).
 */
package com.tipsterbyte.tipsterbytefxv2.domain;