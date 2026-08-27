// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia de PronosticoSugerido.
// [POR QUÉ]: HU-16 AC10/13 — guarda y consulta pronósticos sugeridos tras evaluación.
// [RELACIONES]: CU-24 (escritura), CU-25 (lectura).
//               Implementado por PronosticoSugeridoRepositoryJpaAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.PronosticoSugerido;

import java.util.List;
import java.util.UUID;

public interface PronosticoSugeridoRepository {

    List<PronosticoSugerido> buscarPorEstrategiaId(UUID estrategiaId);

    void guardar(PronosticoSugerido sugerido);

    void guardarLote(List<PronosticoSugerido> sugeridos);

    void eliminarPorEstrategiaId(UUID estrategiaId);
}
