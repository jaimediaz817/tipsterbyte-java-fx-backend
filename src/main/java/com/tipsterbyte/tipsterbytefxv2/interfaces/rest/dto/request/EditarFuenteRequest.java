// ─────────────────────────────────────────────
// [QUÉ]: Request DTO de CU-11 (editar fuente): nombre, url base opcional y estado
//        activa. Identifica la fuente por su tipo (clave natural única) en el path.
// [POR QUÉ]: El SUPERADMIN corrige url_base_fuente desde el formulario sin re-registrar
//            (el registro rechaza tipos duplicados). Bean Validation falla temprano 400.
// [RELACIONES]: PUT /api/v1/fuentes/{tipo} → CU-11 editarFuente.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EditarFuenteRequest(

        @NotBlank(message = "nombre es obligatorio")
        String nombre,

        String urlBaseFuente,

        boolean activa) {
}
