package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TareaProgramadaRepository {
    // Custom methods needed for the use case and scheduler
    Optional<TareaProgramada> buscarPorLigaIdYTipoFuente(UUID ligaId, TipoFuenteExtraccion tipoFuente);
    Optional<TareaProgramada> buscarGlobal();
    List<TareaProgramada> listarPorPrioridadAsc();
    TareaProgramada guardar(TareaProgramada tarea);
    void eliminarPorId(UUID id);
    Optional<TareaProgramada> encontrarPorId(UUID id);

    // [QUÉ]: HU-14 AC8 — busca todas las tareas de una liga (para pausa/reanudación masiva).
    List<TareaProgramada> buscarPorLigaId(UUID ligaId);
}