// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia del aggregate Estrategia.
// [POR QUÉ]: HU-16 CU-23/24/25 — CRUD de estrategias y consulta de activas para evaluación.
// [RELACIONES]: CU-23 (CRUD), CU-24 (lectura para evaluar), CU-25 (lectura sugerencias).
//               Implementado por EstrategiaRepositoryJpaAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Estrategia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstrategiaRepository {

    Optional<Estrategia> buscarPorId(UUID id);

    List<Estrategia> buscarPorTipsterId(UUID tipsterId);

    List<Estrategia> buscarActivas();

    List<Estrategia> buscarActivasPorTipsterId(UUID tipsterId);

    long contarActivasPorTipsterId(UUID tipsterId);

    void guardar(Estrategia estrategia);

    void eliminar(UUID id);
}
