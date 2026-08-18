// ─────────────────────────────────────────────
// [QUÉ]: Comando de alta de tarea programada (CU-15): liga+tipo (o catálogo global),
//        prioridad, cron o frecuencia amigable, y estado inicial activa.
// [POR QUÉ]: Encapsula los parámetros de registro para que el caso de uso reciba un
//            objeto coherente en lugar de muchos argumentos sueltos.
// [ALTERNATIVAS]: Parámetros primitivos; se descartan por legibilidad al crecer.
// [RELACIONES]: CU-15 → GestionarTareasProgramasUseCase.registrar(comando).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.dto;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Frecuencia;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;

import java.util.UUID;

public record RegistrarTareaProgramadaComando(
        UUID ligaId,
        TipoFuenteExtraccion tipoFuente,
        String prioridad,
        String cron,
        Frecuencia frecuencia,
        Boolean activa) {
}