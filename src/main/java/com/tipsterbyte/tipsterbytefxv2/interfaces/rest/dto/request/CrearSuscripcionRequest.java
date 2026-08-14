// ─────────────────────────────────────────────
// [QUÉ]: Request DTO de CU-09 (crear suscripción): cliente, tipster, el plan elegido
//        (nombre, precio, duración en días) y la fecha de inicio de la vigencia.
// [POR QUÉ]: Traduce la request HTTP al aggregate Suscripcion. El VO Plan valida
//            precio no negativo y duración positiva; aquí solo se valida la presencia
//            de los campos de la request.
// [ALTERNATIVAS]: Recibir un id de plan persistido; se descarta porque los planes son
//                 un catálogo cerrado sin tabla propia aún (FASE 8).
// [RELACIONES]: CU-09 → Suscripcion (domain.model) + Plan (domain.model).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CrearSuscripcionRequest(

        @NotNull(message = "clienteId es obligatorio")
        UUID clienteId,

        @NotNull(message = "tipsterId es obligatorio")
        UUID tipsterId,

        @NotBlank(message = "plan.nombre es obligatorio")
        String planNombre,

        @NotNull(message = "plan.precio es obligatorio")
        @DecimalMin(value = "0.0", message = "plan.precio debe ser mayor o igual a 0")
        BigDecimal planPrecio,

        @NotNull(message = "plan.duracionDias es obligatorio")
        Integer planDuracionDias,

        @NotNull(message = "fechaInicio es obligatorio")
        LocalDateTime fechaInicio) {
}