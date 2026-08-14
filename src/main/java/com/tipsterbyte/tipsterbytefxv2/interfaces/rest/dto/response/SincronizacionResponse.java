// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de los casos de uso de sincronización (CU-01, CU-02, CU-03):
//        resume cuántos eventos de dominio se emitieron durante la extracción.
// [POR QUÉ]: Expone al cliente el efecto de la sincronización sin filtrar los eventos
//            internos del dominio (su publicación real es FASE 13).
// [ALTERNATIVAS]: Devolver List<DomainEvent>; se descarta porque acoplaría interfaces
//                 al mecanismo interno de eventos del agregado.
// [RELACIONES]: CU-01/CU-02/CU-03 → SincronizacionResponse (interfaces.rest).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

public record SincronizacionResponse(
        int eventosEmitidos) {
}