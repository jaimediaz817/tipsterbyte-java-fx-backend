// ─────────────────────────────────────────────
// [QUÉ]: Comando de entrada para CU-04 (activar liga) y CU-11 (gestionar fuentes):
//        asocia una liga con una fuente de extracción y su URL (path_to_scrape).
// [POR QUÉ]: Encapsula el dato de entrada en un solo objeto. La URL de cada fuente
//            la suministra el usuario al activar una liga; sin ella los adapters de
//            sincronización no pueden llamar a los endpoints.
// [ALTERNATIVAS]: Guardar la URL en Liga; se descarta porque una liga tiene 3 URLs
//                 (una por fuente) y el catálogo de fuentes es gestionable (CU-11).
// [RELACIONES]: CU-04/CU-11 → DetalleFuenteExtraccion (constructor).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;

import java.util.UUID;

public record AsociarUrlFuenteComando(
        UUID ligaId,
        TipoFuenteExtraccion tipo,
        String url,
        boolean activa) {
}
