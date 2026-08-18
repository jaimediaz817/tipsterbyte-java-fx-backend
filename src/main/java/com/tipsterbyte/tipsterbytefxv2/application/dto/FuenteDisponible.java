// ─────────────────────────────────────────────
// [QUÉ]: Fuente candidata para el selector "Programar" del frontend: una liga activa
//        con su tipo de fuente, o la opción "Catálogo global" (ligaId/tipo null).
// [POR QUÉ]: El modal de creación debe mostrar qué ligas/tipos pueden programarse y
//            cuáles ya tienen tarea (unicidad por liga+tipo).
// [ALTERNATIVAS]: Que el frontend arme la lista desde GET /ligas; se descarta porque
//                 pierde el flag yaProgramada y mezcla responsabilidades.
// [RELACIONES]: CU-15 → GestionarTareasProgramasUseCase.listarFuentesDisponibles() →
//               GET /api/v1/tareas-programadas/disponibles.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;

import java.util.UUID;

public record FuenteDisponible(
        UUID ligaId,
        String ligaNombre,
        TipoFuenteExtraccion tipoFuente,
        boolean yaProgramada) {
}