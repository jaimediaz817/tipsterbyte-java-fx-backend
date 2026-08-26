// ─────────────────────────────────────────────
// [QUÉ]: DTO que representa un partido próximo de Wplay con sus datos crudos
//        (nombres de equipos, fecha/hora, cuotas 1X2 y doble oportunidad).
// [POR QUÉ]: HU-14 AC4.2/4.3 — el caso de uso necesita los nombres de equipo crudos
//            para resolverlos contra la plantilla. Este DTO encapsula la información
//            antes del matching.
// [RELACIONES]: Devuelto por ProveedorPartidosProximosWplay; consumido por
//               SincronizarCuotasUseCase (AC4.2/4.3).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PartidoWplay(
        String teamLocal,
        String teamVisitante,
        Instant fechaPartido,
        BigDecimal cuotaLocal,
        BigDecimal cuotaEmpate,
        BigDecimal cuotaVisitante,
        List<CuotaDobleOportunidad> dobleOportunidad) {

    public record CuotaDobleOportunidad(String nombre, BigDecimal valor) {
    }
}
