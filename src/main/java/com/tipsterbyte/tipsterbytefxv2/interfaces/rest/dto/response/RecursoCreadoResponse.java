// ─────────────────────────────────────────────
// [QUÉ]: Response DTO genérico para recursos creados que devuelven solo su id
//        (usado cuando el frontend necesita conocer la identidad del recurso
//        recién creado sin tener que parsear el header Location).
// [POR QUÉ]: Algunos endpoints POST devuelven 201 con body (AuthResponse,
//            SuscripcionResponse) y otros solo con header Location. Este DTO
//            unifica los casos en que solo el id es relevante, manteniendo
//            consistencia en el contrato de respuestas de creación.
// [ALTERNATIVAS]: DTO específico por recurso (PronosticoCreadoResponse,
//                 LigaCreadaResponse); se descarta porque generaría N clases
//                 con un solo campo, complicando el frontend innecesariamente.
// [RELACIONES]: PronosticoController POST /api/v1/pronosticos → 201 + RecursoCreadoResponse.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import java.util.UUID;

public record RecursoCreadoResponse(UUID id) {
}
