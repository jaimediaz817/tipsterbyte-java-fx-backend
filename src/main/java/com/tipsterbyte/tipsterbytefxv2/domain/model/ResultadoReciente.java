// ─────────────────────────────────────────────
// [QUÉ]: Resultado de un partido reciente de un equipo, usado para la racha de los
//        últimos 5 partidos (fuente #3, resultados_ultimos_5_jugados).
// [POR QUÉ]: La fuente entrega valores numéricos (1=G, 0=E, -1=P); el dominio los
//            representa con un enum del lenguaje ubicuo para no propagar códigos
//            mágicos a través de las capas.
// [ALTERNATIVAS]: Enteros en PosicionTabla; se descartan porque un -1/0/1 no expresa
//                 el negocio y permitiría valores inválidos.
// [RELACIONES]: Usado por PosicionTabla.ultimosResultados (CU-01) y mapeado desde
//               PosicionFuente (FASE 8.5).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

public enum ResultadoReciente {

    GANADO,
    EMPATE,
    PERDIDO;

    // [QUÉ]: Convierte el código numérico de la fuente #3 (1/0/-1) al enum.
    // [POR QUÉ]: El adapter de posiciones traduce el formato externo al dominio.
    public static ResultadoReciente desdeCodigo(int codigo) {
        return switch (codigo) {
            case 1 -> GANADO;
            case 0 -> EMPATE;
            case -1 -> PERDIDO;
            default -> throw new IllegalArgumentException("Código de resultado reciente inválido: " + codigo);
        };
    }
}
