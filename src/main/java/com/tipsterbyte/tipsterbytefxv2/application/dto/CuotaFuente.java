// ─────────────────────────────────────────────
// [QUÉ]: DTO de fuente que representa una cuota de un mercado tal como la entrega
//        una API externa de odds (API-Football / The Odds API / SharpAPI).
// [POR QUÉ]: Aísla el formato externo de la fuente del VO Cuota. El caso de uso
//            CU-03 mapea este DTO al dominio (validando BR-007 en el VO).
// [ALTERNATIVAS]: Que el ProveedorCuotas devuelva List<Cuota>; se descarta porque el
//                 mercado llega con el formato de cada proveedor (ej: "1X2" vs "h2h").
// [RELACIONES]: Devuelto por ProveedorCuotas; consumido por CU-03.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;

import java.math.BigDecimal;

public record CuotaFuente(
        Mercado mercado,
        BigDecimal valor) {
}