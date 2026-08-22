// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia de temporadas (entity Temporada del dominio).
// [POR QUÉ]: Define el contrato para persistir y consultar temporadas sin acoplar
//            el dominio a JPA. Implementado por TemporadaRepositoryJpaAdapter.
// [ALTERNATIVAS]: Consultas directas en el caso de uso; se descartan porque la
//                 persistencia siempre pasa por un adapter del puerto.
// [RELACIONES]: Implementado por TemporadaRepositoryJpaAdapter; consumido por
//               CU-10, CU-04 y adapters de fuentes.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemporadaRepository {

    // [QUÉ]: Busca una temporada por su identidad.
    Optional<Temporada> buscarPorId(UUID id);

    // [QUÉ]: Lista todas las temporadas de una liga.
    List<Temporada> buscarPorLigaId(UUID ligaId);

    // [QUÉ]: Busca una temporada por liga y nombre (ej: "Apertura").
    Optional<Temporada> buscarPorLigaIdYNombre(UUID ligaId, String nombre);

    // [QUÉ]: Busca la temporada activa de una liga.
    Optional<Temporada> buscarActivaPorLigaId(UUID ligaId);

    // [QUÉ]: Persiste una temporada (insert o update).
    void guardar(Temporada temporada);

    // [QUÉ]: Elimina una temporada por su identidad.
    void eliminar(UUID id);
}
