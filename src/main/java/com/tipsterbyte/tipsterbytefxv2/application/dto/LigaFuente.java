// ─────────────────────────────────────────────
// [QUÉ]: DTO de fuente que representa una liga de catálogo tal como la entrega el
//        endpoint #5 (ext-soccerway-leagues-by-country).
// [POR QUÉ]: Aísla el formato externo (name, url_soccerway, anio, api_id, etc.) del
//            aggregate Liga del dominio. El caso de uso CU-10 mapea este DTO al dominio,
//            resolviendo la temporada desde el campo `anio` (AAAA/AAAA); `semestre` se
//            descarta por ser inconsistente (a veces temporada, a veces categoría).
// [ALTERNATIVAS]: Que el ProveedorLigasPorPais devuelva List<Liga>; se descarta porque
//                 el proveedor no conoce los ids del dominio ni resuelve la Temporada.
// [RELACIONES]: Devuelto por ProveedorLigasPorPais; consumido por CU-10.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

public record LigaFuente(
        String nombre,
        String type,
        String logoUrl,
        String apiId,
        String urlSoccerway,
        String anio) {
}