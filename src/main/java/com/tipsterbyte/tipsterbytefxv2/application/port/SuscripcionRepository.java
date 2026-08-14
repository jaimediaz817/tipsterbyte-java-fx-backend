// ─────────────────────────────────────────────
// [QUÉ]: Puerto de persistencia del aggregate Suscripcion.
// [POR QUÉ]: Abstrae la persistencia del dominio (adapter JPA en FASE 8). Los casos
//            de uso CU-08 y CU-09 guardan/recuperan suscripciones.
// [ALTERNATIVAS]: Trabajar con la entidad JPA directamente en application; se descarta
//                 porque acoplaría la capa de aplicación a infraestructura.
// [RELACIONES]: CU-08, CU-09. Implementado por SuscripcionRepositoryJpaAdapter (FASE 8).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.port;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Suscripcion;

import java.util.List;
import java.util.UUID;

public interface SuscripcionRepository {

    // [QUÉ]: Recupera las suscripciones ACTIVA de un cliente.
    // [POR QUÉ]: CU-08 valida BR-006 (acceso solo de suscriptores activos).
    List<Suscripcion> buscarActivasPorCliente(UUID clienteId);

    // [QUÉ]: Persiste una suscripción (crea o actualiza según exista el id).
    void guardar(Suscripcion suscripcion);

}