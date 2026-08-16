// ─────────────────────────────────────────────
// [QUÉ]: Controller REST de catálogo de roles del sistema.
// [POR QUÉ]: Expone GET /api/v1/roles para que el frontend Angular pueda pintar
//            el selector de roles en el formulario de registro y filtrar menús
//            según los roles existentes. Es público porque el registro es público
//            y el catálogo es un conjunto cerrado sin datos sensibles.
// [ALTERNATIVAS]: Consultar una tabla `roles` en BD; se descarta porque hoy no
//                 existe (FASE 11 modela el rol como enum Rol en el dominio) y
//                 el catálogo es estático por regla de negocio.
// [RELACIONES]: domain.model.Rol → RolResponse. No toca dominio ni BR-001..008.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.domain.model.Rol;
import com.tipsterbyte.tipsterbytefxv2.interfaces.rest.dto.response.RolResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@ConditionalOnProperty(name = "app.api.rest.enabled", havingValue = "true")
public class RolController {

    // [QUÉ]: GET /api/v1/roles — lista el catálogo completo de roles (código + nombre).
    // [POR QUÉ]: El formulario de registro (CU-12) necesita los roles disponibles
    //            para ofrecerlos en el select antes de que el usuario se autentique.
    @GetMapping
    public ResponseEntity<List<RolResponse>> listarRoles() {
        List<RolResponse> roles = Arrays.stream(Rol.values())
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(roles);
    }

    private RolResponse toResponse(Rol rol) {
        return new RolResponse(rol.name(), nombreLegible(rol));
    }

    private String nombreLegible(Rol rol) {
        return switch (rol) {
            case CLIENTE -> "Cliente";
            case TIPSTER -> "Tipster";
            case SUPERADMIN -> "Super Administrador";
        };
    }
}