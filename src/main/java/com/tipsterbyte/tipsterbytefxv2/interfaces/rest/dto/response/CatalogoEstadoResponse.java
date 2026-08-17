// ─────────────────────────────────────────────
// [QUÉ]: Response DTO del estado del catálogo geográfico: estado derivado
//        (VACIO/POBLADO) y conteos de países y ligas persistidos (CU-10).
// [POR QUÉ]: El frontend (SUPERADMIN) muestra el estado del catálogo y los conteos
//            en el panel de administración; estos campos alimentan el botón de
//            activación (POST /api/v1/catalogo/activar) y su consulta de estado.
// [ALTERNATIVAS]: Devolver solo el enum; se descarta porque el frontend también
//                 muestra los totales de países y ligas poblados.
// [RELACIONES]: CatalogoController (POST /activar y GET /estado); mapeado desde
//               CatalogoEstadoDto.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoCatalogo;

public record CatalogoEstadoResponse(
        EstadoCatalogo estado,
        int totalPaises,
        int totalLigas) {
}