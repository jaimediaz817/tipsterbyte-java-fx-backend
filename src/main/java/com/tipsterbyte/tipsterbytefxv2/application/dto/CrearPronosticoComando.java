// ─────────────────────────────────────────────
// [QUÉ]: Comando de entrada para CU-06 (crear pronóstico): el tipster, el partido,
//        el mercado, la selección esperada y la cuota de referencia.
// [POR QUÉ]: Encapsula los datos de entrada del caso de uso en un solo objeto,
//            desacoplado de la forma de la request HTTP (FASE 7 lo construirá).
// [ALTERNATIVAS]: Método con 5 parámetros; se descarta porque un comando nombra cada
//                 dato y simplifica la firma y los tests.
// [RELACIONES]: CU-06 → Pronostico (constructor) y sus VOs SeleccionPronostico/Cuota.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;

import java.math.BigDecimal;
import java.util.UUID;

public record CrearPronosticoComando(
        UUID tipsterId,
        UUID partidoId,
        Mercado mercado,
        String resultadoEsperado,
        BigDecimal cuotaValor) {
}