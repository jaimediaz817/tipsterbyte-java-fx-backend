// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia del catálogo de fuentes de extracción (entity FuenteExtraccion).
// [POR QUÉ]: Abstrae la persistencia del catálogo de fuentes del dominio (adapter JPA).
//            El caso de uso CU-11 registra/recupera fuentes sin conocer la BD.
// [ALTERNATIVAS]: Acceso directo a JpaRepository en application; se descarta porque
//                 acoplaría la capa de aplicación a infraestructura.
// [RELACIONES]: CU-11. Implementado por FuenteExtraccionRepositoryJpaAdapter (FASE 8.5).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FuenteExtraccionRepository {

    // [QUÉ]: Recupera una fuente por su id, o vacío si no existe.
    Optional<FuenteExtraccion> buscarPorId(UUID id);

    // [QUÉ]: Recupera una fuente por su tipo (STANDINGS/ODDS_WPLAY/CALENDAR), o vacío.
    // [POR QUÉ]: Los adapters de sincronización resuelven la URL por tipo de fuente.
    Optional<FuenteExtraccion> buscarPorTipo(TipoFuenteExtraccion tipo);

    // [QUÉ]: Recupera todas las fuentes del catálogo.
    List<FuenteExtraccion> buscarTodas();

    // [QUÉ]: Persiste una fuente (crea o actualiza según exista el id).
    void guardar(FuenteExtraccion fuente);

}
