// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de consulta de ligas: vista simplificada de una liga para
//        listados y navegación del frontend (HU-01, HU-02) y del catálogo geográfico
//        (panel admin: países → ligas BORRADOR → activación CU-04).
// [POR QUÉ]: El frontend Angular necesita un listado ligero de ligas (activas o del
//            catálogo por estado) sin cargar la tabla de posiciones completa. Separa
//            la vista de lectura de los comandos de escritura (CQS en la capa de
//            interfaces). urlSoccerway/apiId son null si la liga no se creó desde la
//            fuente #5 (nunca se omiten campos: "no" claro antes que null silencioso).
// [ALTERNATIVAS]: Un segundo DTO LigaCatalogoResponse; se descarta: el endpoint único
//                 GET /api/v1/ligas?estado=... devuelve el MISMO shape para ambos casos
//                 (activas y catálogo), un solo modelo en el frontend.
// [RELACIONES]: LigaController GET /api/v1/ligas → LigaRepository.buscarActivas()/
//               buscarPorEstado()/buscarPorEstadoYPais().
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;

import java.util.UUID;

public record LigaResponse(
        UUID id,
        String nombre,
        String pais,
        EstadoLiga estado,
        String temporada,
        String urlSoccerway,
        String apiId) {
}
