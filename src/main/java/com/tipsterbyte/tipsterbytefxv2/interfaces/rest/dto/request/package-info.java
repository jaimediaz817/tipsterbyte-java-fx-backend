/**
 * [QUÉ]: DTOs de request de la API: la forma en que los clientes envían datos a los
 *        endpoints de CU-04, CU-05, CU-06 y CU-09.
 * [POR QUÉ]: Validan la estructura de entrada (Bean Validation) en la capa interfaces,
 *            desacoplada de los comandos de application y de los VOs del dominio.
 * [RELACIONES]: Consumidos por los controllers; mapeados a application.dto / domain.
 */
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request;