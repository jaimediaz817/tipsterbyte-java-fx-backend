package com.tipsterbyte.tipsterbytefxv2.interfaces.rest.controller;

import com.tipsterbyte.tipsterbytefxv2.application.usecase.GestionarTareasProgramasUseCase;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaProgramada;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tareas-programadas")
public class TareaProgramadaController {

    private final GestionarTareasProgramasUseCase gestionarTareasProgramasUseCase;

    public TareaProgramadaController(GestionarTareasProgramasUseCase gestionarTareasProgramasUseCase) {
        this.gestionarTareasProgramasUseCase = gestionarTareasProgramasUseCase;
    }

    // DTO for request body
    public static class RegistrarTareaProgramadaRequest {
        public String isoAlpha2;
        public String nombre;
        public String prioridad;
    }

    @GetMapping
    public ResponseEntity<List<TareaProgramada>> listar() {
        List<TareaProgramada> tareas = gestionarTareasProgramasUseCase.listar();
        return ResponseEntity.ok(tareas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TareaProgramada> obtenerPorId(@PathVariable UUID id) {
        TareaProgramada tarea = gestionarTareasProgramasUseCase.obtenerPorId(id);
        return ResponseEntity.ok(tarea);
    }

    @PostMapping
    public ResponseEntity<TareaProgramada> registrar(@RequestBody RegistrarTareaProgramadaRequest request) {
        TareaProgramada tarea = gestionarTareasProgramasUseCase.registrar(
                request.isoAlpha2,
                request.nombre,
                request.prioridad
        );
        return ResponseEntity.created(URI.create("/api/v1/tareas-programadas/" + tarea.id()))
                .body(tarea);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        gestionarTareasProgramasUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}