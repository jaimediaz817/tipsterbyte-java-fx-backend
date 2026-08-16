// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia del aggregate Liga.
// [POR QUÉ]: Abstrae la persistencia del dominio (adapter JPA en FASE 8). Los casos
//            de uso CU-01, CU-02 y CU-04 guardan/recuperan ligas sin conocer la BD.
// [ALTERNATIVAS]: Trabajar con la entidad JPA directamente en application; se descarta
//                 porque acoplaría la capa de aplicación a infraestructura.
// [RELACIONES]: CU-01, CU-02, CU-04. Implementado por LigaRepositoryJpaAdapter (FASE 8).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoLiga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LigaRepository {

    // [QUÉ]: Recupera una liga por su id, o vacío si no existe.
    Optional<Liga> buscarPorId(UUID id);

    // [QUÉ]: Recupera las ligas en estado ACTIVA (las que pueden sincronizarse).
    List<Liga> buscarActivas();

    // [QUÉ]: Recupera las ligas en un estado dado (catálogo del frontend: BORRADOR,
    //        ACTIVA, INACTIVA). Filtro de solo lectura para el panel geográfico.
    // [POR QUÉ]: El catálogo de CU-10 crea ligas BORRADOR que el frontend debe poder
    //            listar para el flujo países → ligas → activación (CU-04).
    // [RELACIONES]: LigaController GET /api/v1/ligas?estado=... (solicitud frontend).
    List<Liga> buscarPorEstado(EstadoLiga estado);

    // [QUÉ]: Recupera las ligas de un estado y un país dado (nombre exacto,
    //        case-insensitive). Filtro combinado para el panel geográfico.
    // [POR QUÉ]: El frontend filtra el catálogo por país sin cargar todo el listado.
    // [RELACIONES]: LigaController GET /api/v1/ligas?estado=...&pais=...
    List<Liga> buscarPorEstadoYPais(EstadoLiga estado, String pais);

    // [QUÉ]: Recupera una liga por su URL de Soccerway, o vacío si no existe.
    // [POR QUÉ]: La fuente #5 entrega url_soccerway como clave natural; CU-10 la usa
    //            para no duplicar ligas ya registradas en el catálogo.
    Optional<Liga> buscarPorUrlSoccerway(String urlSoccerway);

    // [QUÉ]: Persiste una liga (crea o actualiza según exista el id).
    void guardar(Liga liga);

}