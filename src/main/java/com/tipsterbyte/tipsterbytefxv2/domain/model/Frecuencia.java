// ─────────────────────────────────────────────
// [QUÉ]: Value Object de frecuencia amigable: "cada N {unidad}" que se codifica a una
//        expresión cron de 6 segmentos (seg min hora día-mes mes día-semana).
// [POR QUÉ]: El editor del frontend elige periodo con unidades humanas (cada 8 días,
//            cada 2 horas, cada 30 minutos...) y el backend lo traduce a cron validado.
//            Centralizar la conversión aquí evita que cada consumidor (controller, UI)
//            reinvente el mapeo y garantiza límites coherentes por unidad.
// [ALTERNATIVAS]: Conversión solo en el frontend; se descarta porque el backend debe
//                 validar y persistir el cron canónico. Crons libres sin editor; se
//                 descarta por petición explícita de UX amigable.
// [RELACIONES]: UnidadFrecuencia (domain.model) + GestionarTareasProgramasUseCase
//               (resuelve frecuencia → cron en registrar/actualizar).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

public record Frecuencia(int valor, UnidadFrecuencia unidad) {

    // [QUÉ]: Factory con unidad como texto (lo que llega del JSON del frontend).
    public static Frecuencia of(int valor, String unidad) {
        UnidadFrecuencia u = parseUnidad(unidad);
        return new Frecuencia(valor, u);
    }

    // [QUÉ]: Valida rango por unidad y construye la frecuencia.
    // [POR QUÉ]: Un valor fuera de rango produciría un cron sin sentido o inválido.
    public Frecuencia {
        if (valor <= 0) {
            throw new DomainException("La frecuencia debe ser al menos 1");
        }
        int maximo = switch (unidad) {
            case SEGUNDOS, MINUTOS -> 59;
            case HORAS -> 23;
            case DIAS -> 30;
        };
        if (valor > maximo) {
            throw new DomainException(
                    "Valor " + valor + " inválido para " + unidad + " (máximo " + maximo + ")");
        }
    }

    // [QUÉ]: Codifica la frecuencia amigable a una expresión cron de 6 segmentos.
    // [POR QUÉ]: Es el contrato que entiende el scheduler (CronExpression de Spring).
    public String toCronExpression() {
        return switch (unidad) {
            case SEGUNDOS -> "0/" + valor + " * * * * *";
            case MINUTOS -> "0 0/" + valor + " * * * *";
            case HORAS -> "0 0 */" + valor + " * * *";
            case DIAS -> "0 0 0 */" + valor + " * *";
        };
    }

    private static UnidadFrecuencia parseUnidad(String unidad) {
        if (unidad == null || unidad.isBlank()) {
            throw new DomainException("La frecuencia requiere una unidad");
        }
        try {
            return UnidadFrecuencia.valueOf(unidad.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("Unidad de frecuencia inválida: " + unidad);
        }
    }
}