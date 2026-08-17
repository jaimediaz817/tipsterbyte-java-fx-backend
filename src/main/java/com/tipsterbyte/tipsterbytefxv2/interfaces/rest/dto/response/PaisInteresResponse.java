// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de CU-14: país de interés con su prioridad de poblamiento.
// [POR QUÉ]: Expone el entity PaisInteres por REST sin acoplar interfaces al dominio.
//            El frontend usa la prioridad (1 = primero) para mostrar el orden y
//            enviar la lista reordenada en el PUT de reemplazo.
// [ALTERNATIVAS]: Exponer el entity PaisInteres directo; se descarta porque la UI no
//                 necesita conocer el modelo de dominio.
// [RELACIONES]: PaisInteresController GET /api/v1/paises-interes.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

public record PaisInteresResponse(
        String isoAlpha2,
        String nombre,
        int prioridad) {
}