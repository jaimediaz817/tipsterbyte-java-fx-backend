// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia del detalle de fuente de extracción (entity
//        DetalleFuenteExtraccion): asocia una liga con una fuente y su URL.
// [POR QUÉ]: Abstrae la persistencia del detalle del dominio (adapter JPA). Los
//            adapters de sincronización (CU-01/02/03) resuelven la URL de una liga
//            consultando este puerto; CU-04 la guarda al activar la liga.
// [ALTERNATIVAS]: Guardar la URL en Liga; se descarta porque una liga tiene 3 URLs
//                 (una por fuente) y el catálogo de fuentes es gestionable (CU-11).
// [RELACIONES]: CU-04 (guardar) y CU-01/02/03 (resolver URL). Implementado por
//               DetalleFuenteExtraccionRepositoryJpaAdapter (FASE 8.5).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DetalleFuenteExtraccionRepository {

    // [QUÉ]: Recupera el detalle de una liga para un tipo de fuente, o vacío.
    // [POR QUÉ]: Los adapters resuelven la URL del endpoint por ligaId + tipo.
    Optional<DetalleFuenteExtraccion> buscarPorLigaYTipo(UUID ligaId, TipoFuenteExtraccion tipo);

    // [QUÉ]: Recupera todos los detalles de fuentes de una liga.
    // [POR QUÉ]: CU-04 verifica que las 3 fuentes estén asociadas antes de activar (BR-001).
    List<DetalleFuenteExtraccion> buscarPorLiga(UUID ligaId);

    // [QUÉ]: Persiste un detalle (crea o actualiza según exista el id).
    void guardar(DetalleFuenteExtraccion detalle);

}
