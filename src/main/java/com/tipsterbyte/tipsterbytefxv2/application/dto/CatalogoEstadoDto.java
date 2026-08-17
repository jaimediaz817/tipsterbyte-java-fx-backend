// ─────────────────────────────────────────────
// [QUÉ]: DTO de aplicación que resume el estado del catálogo geográfico: estado
//        derivado (VACIO/POBLADO) y conteos de países y ligas persistidos.
// [POR QUÉ]: Aísla a los use cases de los response DTOs de interfaces y expresa
//            el estado del catálogo sin acoplar application a la capa HTTP.
// [ALTERNATIVAS]: Exponer directamente los conteos; se descarta porque el estado
//                 derivado le da al frontend una señal única y legible.
// [RELACIONES]: Producido por ConsultarEstadoCatalogoUseCase (CU-10) y consumido
//               por CatalogoController.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoCatalogo;

public record CatalogoEstadoDto(
        EstadoCatalogo estado,
        int totalPaises,
        int totalLigas) {
}