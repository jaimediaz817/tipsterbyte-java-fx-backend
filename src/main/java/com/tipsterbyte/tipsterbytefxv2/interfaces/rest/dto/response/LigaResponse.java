// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de consulta de ligas: vista simplificada de una liga para
//        listados y navegación del frontend (HU-01, HU-02).
// [POR QUÉ]: El frontend Angular necesita un listado ligero de ligas activas sin
//            cargar la tabla de posiciones completa. Separa la vista de lectura
//            de los comandos de escritura (CQS en la capa de interfaces).
// [ALTERNATIVAS]: Reutilizar el aggregate Liga; se descarta porque expone eventos
//                 y estructuras internas innecesarias para la UI.
// [RELACIONES]: LigaController GET /api/v1/ligas → LigaRepository.buscarActivas().
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;

import java.util.UUID;

public record LigaResponse(
        UUID id,
        String nombre,
        String pais,
        EstadoLiga estado,
        String temporada) {
}
