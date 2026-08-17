// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de un país disponible en la fuente #1 (sin persistir).
// [POR QUÉ]: GET /api/v1/paises/disponibles expone el catálogo completo de la fuente
//            para que el frontend elija los países de interés (CU-14) antes de
//            poblarlos. A diferencia de PaisResponse no lleva id (aún no persistido).
// [ALTERNATIVAS]: Reusar PaisResponse con id null; se descarta porque confundiría
//                 disponibles (sin id) con persistidos (con id).
// [RELACIONES]: PaisController GET /api/v1/paises/disponibles → ProveedorPaises.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

public record PaisDisponibleResponse(
        String isoAlpha2,
        String nombre,
        String continente,
        String code,
        String href,
        boolean mapeado) {
}