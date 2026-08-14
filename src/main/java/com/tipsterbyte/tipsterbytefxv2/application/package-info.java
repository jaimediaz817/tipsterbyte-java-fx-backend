/**
 * [QUÉ]: Capa de aplicación. Contiene los casos de uso (CU-01..09), los puertos
 *        (interfaces) que infrastructure implementa, y los DTOs de aplicación.
 * [POR QUÉ]: Orquesta el flujo Controller → Use Case → Dominio → Puerto.
 *            No contiene lógica de negocio; la delega al dominio.
 * [ALTERNATIVAS]: Poner la lógica de negocio en esta capa (service anémico);
 *                 se descarta porque rompe el encapsulamiento del dominio.
 * [RELACIONES]: Depende de domain; define los puertos que implementa infrastructure.
 */
package com.tipsterbyte.tipsterbytefxv2.application;