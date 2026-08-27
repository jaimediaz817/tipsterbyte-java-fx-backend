// ─────────────────────────────────────────────
// [QUÉ]: Servicio de dominio que evalúa un criterio contra los datos de un partido.
// [POR QUÉ]: HU-16 AC7/AC8 — cada criterio tiene su evaluador según fuente/campo.
//            Devuelve `SenalCriterio` con pass, valor observado y peso.
// [ALTERNATIVAS]: Evaluar en el use case directamente; se descarta porque mezcla
//                 lógica de negocio con orquestación.
// [RELACIONES]: CU-24 EvaluarEstrategiaUseCase delega aquí la evaluación individual.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.service;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Criterio;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public final class EvaluadorCriterios {

    private EvaluadorCriterios() {}

    // [QUÉ]: Evalúa un criterio contra los datos proporcionados y retorna la señal.
    public static SenalCriterio evaluar(Criterio criterio, Map<String, BigDecimal> cuotas,
                                         Map<String, Integer> posiciones,
                                         Map<String, List<String>> forma,
                                         boolean enZonaDescenso) {
        return switch (criterio.fuente()) {
            case CUOTAS -> evaluarCuota(criterio, cuotas);
            case POSICIONES -> evaluarPosiciones(criterio, posiciones);
            case FORMA -> evaluarForma(criterio, forma);
            case ZONA_DESCENSO -> evaluarZonaDescenso(criterio, enZonaDescenso);
        };
    }

    // [QUÉ]: Evalúa criterio de tipo CUOTAS (Comparativa o Cruzada).
    private static SenalCriterio evaluarCuota(Criterio criterio, Map<String, BigDecimal> cuotas) {
        if (cuotas == null || cuotas.isEmpty()) {
            return new SenalCriterio(criterio, false, null, "SIN_DATOS");
        }

        String campo = criterio.campo();
        BigDecimal valorObservado = cuotas.get(campo);

        if (valorObservado == null) {
            return new SenalCriterio(criterio, false, null, "CAMPO_NO_ENCONTRADO");
        }

        BigDecimal umbral = parseBigDecimal(criterio.valor());
        boolean pass = comparar(valorObservado, criterio.operador(), umbral);
        return new SenalCriterio(criterio, pass, valorObservado, pass ? "PASS" : "FAIL");
    }

    // [QUÉ]: Evalúa criterio de tipo POSICIONES.
    private static SenalCriterio evaluarPosiciones(Criterio criterio, Map<String, Integer> posiciones) {
        if (posiciones == null || posiciones.isEmpty()) {
            return new SenalCriterio(criterio, false, null, "SIN_DATOS");
        }

        String campo = criterio.campo();
        Integer valorObservado = posiciones.get(campo);

        if (valorObservado == null) {
            return new SenalCriterio(criterio, false, null, "CAMPO_NO_ENCONTRADO");
        }

        BigDecimal umbral = parseBigDecimal(criterio.valor());
        boolean pass = comparar(BigDecimal.valueOf(valorObservado), criterio.operador(), umbral);
        return new SenalCriterio(criterio, pass, BigDecimal.valueOf(valorObservado), pass ? "PASS" : "FAIL");
    }

    // [QUÉ]: Evalúa criterio de tipo FORMA (patrón de últimos resultados).
    private static SenalCriterio evaluarForma(Criterio criterio, Map<String, List<String>> forma) {
        if (forma == null || forma.isEmpty()) {
            return new SenalCriterio(criterio, false, null, "SIN_DATOS");
        }

        List<String> resultados = forma.get(criterio.referencia().name());
        if (resultados == null || resultados.isEmpty()) {
            return new SenalCriterio(criterio, false, null, "SIN_DATOS");
        }

        // Parsear valor esperado: ej "G,E,G" o "max_1_P"
        String valorEsperado = criterio.valor();
        boolean pass = evaluarPatronForma(resultados, valorEsperado);
        return new SenalCriterio(criterio, pass, BigDecimal.valueOf(resultados.size()), pass ? "PASS" : "FAIL");
    }

    // [QUÉ]: Evalúa criterio de tipo ZONA_DESCENSO.
    private static SenalCriterio evaluarZonaDescenso(Criterio criterio, boolean enZonaDescenso) {
        boolean pass = comparar(Boolean.valueOf(enZonaDescenso), criterio.operador(), criterio.valor());
        return new SenalCriterio(criterio, pass, enZonaDescenso ? BigDecimal.ONE : BigDecimal.ZERO,
                pass ? "PASS" : "FAIL");
    }

    // [QUÉ]: Evalúa patrón de forma: "G,E,G" = exacto; "max_1_P" = máx 1 derrota en últimos N.
    private static boolean evaluarPatronForma(List<String> resultados, String patron) {
        if (patron.startsWith("max_")) {
            // Formato: max_N_LETRA (ej: max_1_P = máx 1 derrota)
            String[] partes = patron.substring(4).split("_", 2);
            int max = Integer.parseInt(partes[0]);
            String letra = partes[1];
            long count = resultados.stream().filter(r -> r.equals(letra)).count();
            return count <= max;
        }
        // Formato exacto: "G,E,G"
        String[] esperado = patron.split(",");
        if (esperado.length != resultados.size()) return false;
        for (int i = 0; i < esperado.length; i++) {
            if (!resultados.get(i).equals(esperado[i].trim())) return false;
        }
        return true;
    }

    // [QUÉ]: Comparación genérica entre valor observado y umbral.
    private static boolean comparar(BigDecimal observado, Criterio.OperadorCriterio operador, BigDecimal umbral) {
        return switch (operador) {
            case MAYOR_IGUAL -> observado.compareTo(umbral) >= 0;
            case MENOR_IGUAL -> observado.compareTo(umbral) <= 0;
            case IGUAL -> observado.compareTo(umbral) == 0;
            case MAYOR -> observado.compareTo(umbral) > 0;
            case MENOR -> observado.compareTo(umbral) < 0;
            default -> false;
        };
    }

    // [QUÉ]: Comparación para booleanos (ZONA_DESCENSO).
    private static boolean comparar(Boolean observado, Criterio.OperadorCriterio operador, String valorEsperado) {
        boolean esperado = Boolean.parseBoolean(valorEsperado);
        return switch (operador) {
            case IGUAL -> observado == esperado;
            case NO_CONTIENE -> observado != esperado;
            default -> false;
        };
    }

    private static BigDecimal parseBigDecimal(String valor) {
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException e) {
            throw new DomainException("Valor de criterio no es un número válido: " + valor);
        }
    }

    // ─────────────────────────────────────────────
    // VO resultado de la evaluación de un criterio.
    // ─────────────────────────────────────────────
    public record SenalCriterio(
            Criterio criterio,
            boolean pass,
            BigDecimal valorObservado,
            String estado) {
    }
}
