package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaProgramada;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaProgramadaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GestionarTareasProgramasUseCase {

    private final TareaProgramadaRepository tareaProgramadaRepository;

    public GestionarTareasProgramasUseCase(TareaProgramadaRepository tareaProgramadaRepository) {
        this.tareaProgramadaRepository = tareaProgramadaRepository;
    }

    // [QUÉ]: Registra una nueva tarea programada.
    // [POR QUÉ]: Permite al frontend crear una tarea que se ejecutará según su cron.
    // [RELACIONES]: CU-15 (GestionarTareasProgramadaUseCase) → TareaProgramadaRepository.
    public TareaProgramada registrar(String isoAlpha2, String nombre, String prioridad) {
        String iso = isoAlpha2.trim().toUpperCase();
        if (iso.isEmpty()) {
            throw new DomainException("El código ISO del país es obligatorio");
        }
        // Validar que no exista ya una tarea con ese iso (unicidad)
        if (tareaProgramadaRepository.buscarPorIsoAlpha2(iso).isPresent()) {
            throw new DomainException("Ya existe una tarea programada para el país: " + iso);
        }
        TareaProgramada tarea = new TareaProgramada(
                java.util.UUID.randomUUID(),
                iso,
                nombre,
                prioridad,
                "0 0 * * * *", // cronExpression: daily at midnight (default)
                true,            // activa
                Instant.now().toString()
        );
        return tareaProgramadaRepository.save(tarea);
    }

    // [QUÉ]: Lista todas las tareas programadas ordenadas por prioridad ascendente.
    // [POR QUÉ]: Necesario para mostrar en la UI y para el scheduler.
    public List<TareaProgramada> listar() {
        return StreamSupport.stream(
                tareaProgramadaRepository.listarPorPrioridadAsc().spliterator(), false)
                .collect(Collectors.toList());
    }

    // [QUÉ]: Elimina una tarea programada por su ID.
    // [POR QUÉ]: Permite al frontend eliminar una tarea que ya no se necesita.
    public void eliminar(UUID id) {
        if (!tareaProgramadaRepository.existsById(id)) {
            throw new DomainException("Tarea programada no encontrada con id: " + id);
        }
        tareaProgramadaRepository.deleteById(id);
    }

    // [QUÉ]: Obtiene una tarea programada por su ID.
    // [POR QUÉ]: Utilizado por el controlador para retornar detalles.
    public TareaProgramada obtenerPorId(UUID id) {
        return tareaProgramadaRepository.findById(id)
                .orElseThrow(() -> new DomainException("Tarea programada no encontrada con id: " + id));
    }
}