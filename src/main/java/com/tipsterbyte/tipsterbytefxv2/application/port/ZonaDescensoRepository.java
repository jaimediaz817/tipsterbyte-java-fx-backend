// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia de ZonaDescenso.
// [POR QUÉ]: HU-16 AC5 — CRUD de zona de descenso por temporada.
// [RELACIONES]: CU-24 (evaluación lee configuración de descenso).
//               Implementado por ZonaDescensoRepositoryJpaAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.ZonaDescenso;

import java.util.Optional;
import java.util.UUID;

public interface ZonaDescensoRepository {

    Optional<ZonaDescenso> buscarPorTemporadaId(UUID temporadaId);

    void guardar(ZonaDescenso zonaDescenso);
}
