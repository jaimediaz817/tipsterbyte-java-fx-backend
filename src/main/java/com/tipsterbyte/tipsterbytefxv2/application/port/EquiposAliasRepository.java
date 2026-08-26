// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia para el diccionario de alias de equipos (HU-14 AC4.2).
// [POR QUÉ]: El resolutor multi-fuente necesita consultar y crear aliases sin acoplar
//            a JPA. El auto-aprendizaje crea aliases tras cada match difuso exitoso;
//            el SUPERADMIN puede listar/crear/editar para override manual.
// [RELACIONES]: Implementado por EquiposAliasRepositoryJpaAdapter (infrastructure).
//               Consumido por ResolutorEquipoExtraccion + SincronizarCuotasUseCase.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EquiposAlias;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EquiposAliasRepository {

    // [QUÉ]: Busca aliases por nombre externo normalizado y temporada.
    List<EquiposAlias> buscarPorNombreExternoYTemporada(String nombreExterno, UUID temporadaId);

    // [QUÉ]: Busca todos los aliases de una temporada para una fuente.
    List<EquiposAlias> buscarPorTemporadaYFuente(UUID temporadaId, TipoFuenteExtraccion fuenteTipo);

    // [QUÉ]: Persiste un alias nuevo (auto-aprendido o manual).
    EquiposAlias guardar(EquiposAlias alias);

    // [QUÉ]: Elimina un alias por ID.
    void eliminarPorId(UUID id);
}
