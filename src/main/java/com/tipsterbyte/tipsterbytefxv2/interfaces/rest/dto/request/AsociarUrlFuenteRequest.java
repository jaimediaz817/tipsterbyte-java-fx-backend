// ─────────────────────────────────────────────
// [QUÉ]: Request DTO de CU-11 (asociar URL de fuente a liga): tipo y URL real
//        (path_to_scrape) de la fuente para una liga.
// [POR QUÉ]: Traduce la request HTTP en el comando AsociarUrlFuenteComando que CU-11
//            necesita. La URL la suministra el usuario (por ejemplo, desde el formulario
//            Angular); sin ella los adapters no tienen endpoint que consultar.
// [ALTERNATIVAS]: Guardar la URL en el body de activación; se descarta porque la gestión
//                 del catálogo (CU-11) es independiente de la activación (CU-04).
// [RELACIONES]: CU-11 → AsociarUrlFuenteComando (application.dto) → DetalleFuenteExtraccion.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AsociarUrlFuenteRequest(

        @NotNull(message = "tipo es obligatorio")
        TipoFuenteExtraccion tipo,

        @NotBlank(message = "url es obligatoria")
        String url,

        boolean activa) {
}
