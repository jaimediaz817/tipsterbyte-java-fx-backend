// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de CU-11 (fuentes): la vista de una fuente de extracción o del
//        detalle asociado a una liga (id, nombre, tipo, URL y estado).
// [POR QUÉ]: Devuelve al cliente los datos del catálogo de fuentes sin exponer los
//            entities del dominio. Un solo record sirve para fuente y detalle (el
//            detalle agrega la URL); los campos ausentes van null.
// [ALTERNATIVAS]: Dos records (FuenteResponse y DetalleResponse); se descarta porque
//                 el payload es casi idéntico y un solo tipo simplifica el frontend.
// [RELACIONES]: CU-11 → FuenteExtraccion/DetalleFuenteExtraccion (interfaces.rest).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;

import java.util.UUID;

public record FuenteExtraccionResponse(
        UUID id,
        String nombre,
        TipoFuenteExtraccion tipo,
        String url,
        boolean activa) {
}
