// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia del aggregate Pronostico.
// [POR QUÉ]: Abstrae la persistencia del dominio (adapter JPA en FASE 8). Los casos
//            de uso CU-06, CU-07 y CU-08 guardan/recuperan pronósticos.
// [ALTERNATIVAS]: Trabajar con la entidad JPA directamente en application; se descarta
//                 porque acoplaría la capa de aplicación a infraestructura.
// [RELACIONES]: CU-06, CU-07, CU-08. Implementado por PronosticoRepositoryJpaAdapter (FASE 8).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Pronostico;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PronosticoRepository {

    // [QUÉ]: Recupera un pronóstico por su id, o vacío si no existe.
    Optional<Pronostico> buscarPorId(UUID id);

    // [QUÉ]: Recupera los pronósticos PUBLICADO de una colección de partidos.
    // [POR QUÉ]: CU-08 consulta los pronósticos visibles de los partidos de la liga/fecha.
    List<Pronostico> buscarPublicadosPorPartidos(Collection<UUID> partidoIds);

    // [QUÉ]: Persiste un pronóstico (crea o actualiza según exista el id).
    void guardar(Pronostico pronostico);

}