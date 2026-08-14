/**
 * [QUÉ]: Capa de infraestructura. Contiene los adaptadores que implementan los
 *        puertos de application: persistencia JPA, adapters a las 4 APIs externas
 *        y configuración técnica del framework.
 * [POR QUÉ]: Aísla los detalles técnicos (Spring, JPA, HTTP clients) para que el
 *            dominio y los casos de uso no dependan de ellos. Cambiar de proveedor
 *            o de BD solo toca esta capa.
 * [ALTERNATIVAS]: Hacer que el dominio use directamente librerías HTTP/ORM;
 *                 se descarta porque acopla el negocio a infraestructura.
 * [RELACIONES]: Implementa los puertos definidos en application; depende de domain.
 */
package com.tipsterbyte.tipsterbytefxv2.infrastructure;