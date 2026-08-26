// ─────────────────────────────────────────────
// [QUÉ]: Comando de alta de tarea programada (CU-15): liga+tipo (o catálogo global),
//        prioridad, cron o frecuencia amigable, estado inicial activa y opcionalmente
//        primerDisparo para postergar la primera ejecución.
// [POR QUÉ]: Encapsula los parámetros de registro para que el caso de uso reciba un
//            objeto coherente en lugar de muchos argumentos sueltos.
// [ALTERNATIVAS]: Parámetros primitivos; se descartan por legibilidad al crecer.
// [RELACIONES]: CU-15 → GestionarTareasProgramasUseCase.registrar(comando);
//               HU-14 AC3 → primerDisparo (null = sin postergación).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Frecuencia;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;

import java.time.Instant;
import java.util.UUID;

public record RegistrarTareaProgramadaComando(
        UUID ligaId,
        TipoFuenteExtraccion tipoFuente,
        String prioridad,
        String cron,
        Frecuencia frecuencia,
        Boolean activa,
        Instant primerDisparo) {
}