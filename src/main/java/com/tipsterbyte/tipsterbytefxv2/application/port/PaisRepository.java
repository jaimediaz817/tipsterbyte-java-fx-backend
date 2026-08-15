// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia del catálogo de países (entity Pais).
// [POR QUÉ]: Abstrae la persistencia del catálogo de países del dominio (adapter JPA).
//            El caso de uso CU-10 guarda/recupera países sin conocer la BD.
// [ALTERNATIVAS]: Persistir países dentro del aggregate Liga; se descarta porque el
//                 catálogo es independiente de las ligas (176 países hoy no viven en Liga).
// [RELACIONES]: CU-10. Implementado por PaisRepositoryJpaAdapter (FASE 8.5).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Pais;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaisRepository {

    // [QUÉ]: Recupera un país por su id, o vacío si no existe.
    Optional<Pais> buscarPorId(UUID id);

    // [QUÉ]: Recupera un país por su código ISO alfa-2, o vacío si no existe.
    // [POR QUÉ]: La fuente #1 entrega iso_alpha2 como clave natural para evitar duplicados.
    Optional<Pais> buscarPorIsoAlpha2(String isoAlpha2);

    // [QUÉ]: Recupera todos los países del catálogo.
    List<Pais> buscarTodos();

    // [QUÉ]: Persiste un país (crea o actualiza según exista el id).
    void guardar(Pais pais);

}