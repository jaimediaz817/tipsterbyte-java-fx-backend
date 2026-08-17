package com.tipsterbyte.tipsterbytefxv2.domain.model;

import java.util.UUID;

public record TareaProgramada(
        UUID id,
        String isoAlpha2,
        String nombre,
        String prioridad,
        String cronExpression,
        boolean activa,
        String createdAt) {
}