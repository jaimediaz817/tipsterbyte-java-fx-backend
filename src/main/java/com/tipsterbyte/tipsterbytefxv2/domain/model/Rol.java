// ─────────────────────────────────────────────
// [QUÉ]: Rol de un usuario en la plataforma.
// [POR QUÉ]: Conjunto cerrado: CLIENTE consume pronósticos vía suscripciones,
//            TIPSTER crea pronósticos, SUPERADMIN configura fuentes y activa ligas.
// [ALTERNATIVAS]: Cadena libre; se descarta porque roles fuera de este conjunto
//                 no tienen permisos definidos.
// [RELACIONES]: Usado por entities Tipster y Cliente (FASE 11 profundiza auth).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

public enum Rol {
    CLIENTE,
    TIPSTER,
    SUPERADMIN
}