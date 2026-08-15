// ─────────────────────────────────────────────
// [QUÉ]: DTO de fuente que representa un país tal como lo entrega el endpoint
//        #1 (ext-soccerway-countries).
// [POR QUÉ]: Aísla el formato externo (nombre, iso_alpha2, continente, etc.) del
//            entity Pais del dominio. El caso de uso CU-10 mapea este DTO al dominio.
// [ALTERNATIVAS]: Que el ProveedorPaises devuelva List<Pais>; se descarta porque el
//                 proveedor no conoce el id generado por el dominio.
// [RELACIONES]: Devuelto por ProveedorPaises; consumido por CU-10.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

public record PaisFuente(
        String nombre,
        String href,
        String code,
        String isoAlpha2,
        String continente,
        boolean mapeado) {
}