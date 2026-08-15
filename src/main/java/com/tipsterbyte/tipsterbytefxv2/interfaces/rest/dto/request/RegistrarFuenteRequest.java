// ─────────────────────────────────────────────
// [QUÉ]: Request DTO de CU-11 (registrar fuente): nombre, tipo y activa del catálogo.
// [POR QUÉ]: Traduce la request HTTP en el comando RegistrarFuenteComando que CU-11
//            necesita. La validación estructural (Bean Validation) ocurre en esta capa.
// [ALTERNATIVAS]: Recibir el entity FuenteExtraccion en el body; se descarta porque la
//                 capa de interfaces no expone el dominio directamente.
// [RELACIONES]: CU-11 → RegistrarFuenteComando (application.dto) → FuenteExtraccion.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegistrarFuenteRequest(

        @NotBlank(message = "nombre es obligatorio")
        String nombre,

        @NotNull(message = "tipo es obligatorio")
        TipoFuenteExtraccion tipo,

        boolean activa) {
}
