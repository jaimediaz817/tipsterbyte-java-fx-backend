// ─────────────────────────────────────────────
// [QUÉ]: Response DTO del catálogo geográfico de países (panel admin del frontend:
//        países → ligas por país → activación CU-04).
// [POR QUÉ]: Expone el entity Pais (CU-10, fuente #1) por REST sin exponer el aggregate
//            ni acoplar la capa de interfaces a persistencia. Vista de solo lectura
//            (CQS en la capa de interfaces).
// [ALTERNATIVAS]: Exponer el entity Pais directo; se descarta porque la UI no necesita
//                 conocer el modelo de dominio.
// [RELACIONES]: PaisController GET /api/v1/paises → PaisRepository.buscarTodos().
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import java.util.UUID;

public record PaisResponse(
        UUID id,
        String nombre,
        String isoAlpha2,
        String continente,
        String code,
        String href,
        boolean mapeado) {
}