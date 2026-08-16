// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de una fila de la tabla de posiciones: estadísticas del
//        equipo y racha de últimos 5 partidos (HU-01).
// [POR QUÉ]: La fuente #3 entrega posiciones + últimos resultados; el frontend
//            necesita ambos para mostrar la clasificación con indicadores de forma.
// [ALTERNATIVAS]: Omitir ultimosResultados; se descarta porque es un diferenciador
//                 clave del producto (decisión FASE 8.5).
// [RELACIONES]: LigaController GET /api/v1/ligas/{ligaId}/posiciones; mapeado desde
//               Liga.posiciones() (CU-01).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

import com.tipsterbyte.tipsterbytefxv2.domain.model.ResultadoReciente;

import java.util.List;
import java.util.UUID;

public record PosicionTablaResponse(
        UUID equipoId,
        String equipoNombre,
        int posicion,
        int jugados,
        int ganados,
        int empatados,
        int perdidos,
        int golesFavor,
        int golesContra,
        int puntos,
        List<ResultadoReciente> ultimosResultados) {
}
