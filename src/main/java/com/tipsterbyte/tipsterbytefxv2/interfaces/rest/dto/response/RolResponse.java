// ─────────────────────────────────────────────
// [QUÉ]: Response DTO de catálogo de roles: código canónico (enum Rol) y nombre
//        legible para la UI.
// [POR QUÉ]: El frontend Angular pinta un select de roles en el formulario de
//            registro y filtra menús/rutas; necesita el código y la etiqueta.
// [ALTERNATIVAS]: Devolver solo strings; se descarta porque la UI requiere
//                 mostrar un nombre amigable además del valor persistible.
// [RELACIONES]: RolController GET /api/v1/roles → enum domain.model.Rol.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response;

public record RolResponse(
        String codigo,
        String nombre) {
}