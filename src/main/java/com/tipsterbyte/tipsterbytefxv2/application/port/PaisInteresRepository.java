// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia de los países de interés del catálogo (entity PaisInteres).
// [POR QUÉ]: Abstrae la persistencia de la lista de preferencia (CU-14) del dominio
//            (adapter JPA). CU-10 la consulta para ordenar el poblamiento con los
//            países preferidos primero.
// [ALTERNATIVAS]: Dentro de PaisRepository; se descarta porque es una lista curada con
//                 orden, concepto distinto del catálogo de países persistidos.
// [RELACIONES]: CU-14 (GestionarPaisesInteresUseCase) y CU-10 (SincronizarCatalogoUseCase).
//               Implementado por PaisInteresRepositoryJpaAdapter.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.PaisInteres;

import java.util.List;
import java.util.Optional;

public interface PaisInteresRepository {

    // [QUÉ]: Recupera un país de interés por su iso_alpha2, o vacío si no existe.
    // [POR QUÉ]: El iso_alpha2 es la clave natural de la fuente #1 (CU-14 hace upsert).
    Optional<PaisInteres> buscarPorIsoAlpha2(String isoAlpha2);

    // [QUÉ]: Lista todos los países de interés ordenados por prioridad ascendente.
    // [POR QUÉ]: CU-10 los procesa en ese orden; CU-14 los expone al frontend.
    List<PaisInteres> listarPorPrioridad();

    // [QUÉ]: Persiste un país de interés (crea o actualiza según exista el id).
    void guardar(PaisInteres paisInteres);

    // [QUÉ]: Elimina un país de interés por su iso_alpha2.
    void eliminar(String isoAlpha2);

}