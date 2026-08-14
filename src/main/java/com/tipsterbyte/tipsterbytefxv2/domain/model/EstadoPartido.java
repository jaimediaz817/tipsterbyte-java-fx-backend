// ─────────────────────────────────────────────
// [QUÉ]: Estado del ciclo de vida del aggregate Partido.
// [POR QUÉ]: Conjunto cerrado del negocio. El resultado solo se asigna cuando el
//            partido finaliza (BR-003) y un pronóstico solo se crea sobre partidos
//            PROGRAMADO o EN_VIVO (BR-004).
// [ALTERNATIVAS]: String libre; se descarta por permitir transiciones inválidas.
// [RELACIONES]: Usado por el aggregate Partido (CU-02, CU-05).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

public enum EstadoPartido {
    PROGRAMADO,
    EN_VIVO,
    FINALIZADO,
    SUSPENDIDO
}