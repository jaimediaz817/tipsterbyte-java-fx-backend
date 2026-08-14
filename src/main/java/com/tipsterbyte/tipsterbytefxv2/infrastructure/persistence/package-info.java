/**
 * [QUÉ]: Subpaquete de persistencia: adapters JPA de los repository ports
 *        (LigaRepository, PartidoRepository, PronosticoRepository, SuscripcionRepository).
 * [POR QUÉ]: Aísla JPA/Hibernate/PostgreSQL del resto; el dominio no sabe que existe la BD.
 * [RELACIONES]: Implementa application.port.XxxRepository (FASE 8).
 */
package com.tipsterbyte.tipsterbytefxv2.infrastructure.persistence;