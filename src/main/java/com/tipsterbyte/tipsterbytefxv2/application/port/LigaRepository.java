// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia del aggregate Liga.
// [POR QUÉ]: Abstrae la persistencia del dominio (adapter JPA en FASE 8). Los casos
//            de uso CU-01, CU-02 y CU-04 guardan/recuperan ligas sin conocer la BD.
// [ALTERNATIVAS]: Trabajar con la entidad JPA directamente en application; se descarta
//                 porque acoplaría la capa de aplicación a infraestructura.
// [RELACIONES]: CU-01, CU-02, CU-04. Implementado por LigaRepositoryJpaAdapter (FASE 8).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LigaRepository {

    // [QUÉ]: Recupera una liga por su id, o vacío si no existe.
    Optional<Liga> buscarPorId(UUID id);

    // [QUÉ]: Recupera las ligas en estado ACTIVA (las que pueden sincronizarse).
    List<Liga> buscarActivas();

    // [QUÉ]: Persiste una liga (crea o actualiza según exista el id).
    void guardar(Liga liga);

}