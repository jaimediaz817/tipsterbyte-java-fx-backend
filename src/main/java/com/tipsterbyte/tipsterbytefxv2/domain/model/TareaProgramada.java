package com.tipsterbyte.tipsterbytefxv2.domain.model;

import java.util.UUID;

public record TareaProgramada(
        UUID id,
        UUID ligaId,
        TipoFuenteExtraccion tipoFuente,
        String prioridad,
        String cronExpression,
        boolean activa,
        String createdAt) {
}