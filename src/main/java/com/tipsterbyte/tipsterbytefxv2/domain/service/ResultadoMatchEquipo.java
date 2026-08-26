// ─────────────────────────────────────────────
// [QUÉ]: Resultado del matching de un equipo externo contra la plantilla local.
//        CASADO = un equipo encontrado; AMBIGUO = >1 candidato; SIN_MATCH = ninguno.
// [POR QUÉ]: HU-14 AC4.2 requiere un resultado tipado para que el use case decida
//            si crear el partido (CASADO) o omitirlo (AMBIGUO/SIN_MATCH).
// [RELACIONES]: ResolutorEquipoExtraccion → resultado de matching.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.service;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Equipo;

import java.util.List;

public sealed interface ResultadoMatchEquipo {

    record Casado(Equipo equipo) implements ResultadoMatchEquipo {}

    record Ambiguo(String nombreExterno, List<Equipo> candidatos) implements ResultadoMatchEquipo {}

    record SinMatch(String nombreExterno) implements ResultadoMatchEquipo {}
}
