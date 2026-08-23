// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-15: gestiona tareas programadas (crear, listar, obtener,
//        actualizar cron/frecuencia/estado, eliminar), sus logs de ejecución y las
//        fuentes candidatas para programar.
// [POR QUÉ]: Centraliza la orquestación: unicidad por liga+tipo (o global), validación
//            del cron (crudo o derivado de una frecuencia amigable), pausa/reanudación
//            vía activa, e historial de ejecuciones para la UI de monitoreo.
// [ALTERNATIVAS]: Lógica dispersa entre controller y repositorios; se descarta porque
//                 rompería la capa application.
// [RELACIONES]: CU-15 → TareaProgramadaRepository + TareaLogRepository +
//               LigaRepository + DetalleFuenteExtraccionRepository; consumido por
//               TareaProgramadaController (/api/v1/tareas-programadas).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.ActualizarTareaProgramadaComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.FuenteDisponible;
import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarTareaProgramadaComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaLogRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaProgramadaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Frecuencia;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Liga;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaLog;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TareaProgramada;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;
import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GestionarTareasProgramasUseCase {

    private static final String CRON_DEFAULT = "0 0 * * * *";
    private static final int MAX_LOG_LIMITE = 100;

    private final TareaProgramadaRepository tareaProgramadaRepository;
    private final TareaLogRepository tareaLogRepository;
    private final LigaRepository ligaRepository;
    private final DetalleFuenteExtraccionRepository detalleRepository;

    public GestionarTareasProgramasUseCase(TareaProgramadaRepository tareaProgramadaRepository,
                                           TareaLogRepository tareaLogRepository,
                                           LigaRepository ligaRepository,
                                           DetalleFuenteExtraccionRepository detalleRepository) {
        this.tareaProgramadaRepository = tareaProgramadaRepository;
        this.tareaLogRepository = tareaLogRepository;
        this.ligaRepository = ligaRepository;
        this.detalleRepository = detalleRepository;
    }

    // [QUÉ]: Registra una nueva tarea programada con unicidad por liga+tipo (o global).
    // [POR QUÉ]: El scheduler exige un solo job por fuente (regla FASE 12.5).
    public TareaProgramada registrar(RegistrarTareaProgramadaComando comando) {
        validarUnicidad(comando.ligaId(), comando.tipoFuente());
        String cron = resolverCron(comando.cron(), comando.frecuencia());
        boolean activa = comando.activa() == null || comando.activa();
        TareaProgramada tarea = new TareaProgramada(
                UUID.randomUUID(),
                comando.ligaId(),
                comando.tipoFuente(),
                comando.prioridad(),
                cron,
                activa,
                Instant.now().toString()
        );
        return tareaProgramadaRepository.guardar(tarea);
    }

    // [QUÉ]: Actualiza una tarea existente: cron/frecuencia, activa (pausar/reanudar)
    //        y/o prioridad. Solo aplica los campos presentes.
    public TareaProgramada actualizar(UUID id, ActualizarTareaProgramadaComando comando) {
        TareaProgramada existente = obtenerPorId(id);
        String cron = (comando.cron() != null || comando.frecuencia() != null)
                ? resolverCron(comando.cron(), comando.frecuencia())
                : existente.cronExpression();
        boolean activa = comando.activa() != null ? comando.activa() : existente.activa();
        String prioridad = comando.prioridad() != null ? comando.prioridad() : existente.prioridad();
        TareaProgramada actualizada = new TareaProgramada(
                existente.id(), existente.ligaId(), existente.tipoFuente(),
                prioridad, cron, activa, existente.createdAt());
        return tareaProgramadaRepository.guardar(actualizada);
    }

    // [QUÉ]: Lista todas las tareas programadas ordenadas por prioridad ascendente.
    public List<TareaProgramada> listar() {
        return tareaProgramadaRepository.listarPorPrioridadAsc();
    }

    // [QUÉ]: Elimina una tarea programada por su ID (rechaza si no existe).
    public void eliminar(UUID id) {
        if (tareaProgramadaRepository.encontrarPorId(id).isEmpty()) {
            throw new DomainException("Tarea programada no encontrada con id: " + id);
        }
        tareaProgramadaRepository.eliminarPorId(id);
    }

    // [QUÉ]: Obtiene una tarea programada por su ID (rechaza si no existe).
    public TareaProgramada obtenerPorId(UUID id) {
        return tareaProgramadaRepository.encontrarPorId(id)
                .orElseThrow(() -> new DomainException("Tarea programada no encontrada con id: " + id));
    }

    // [QUÉ]: Devuelve el historial de ejecuciones de una tarea (más reciente primero).
    public List<TareaLog> obtenerLogs(UUID id) {
        obtenerPorId(id); // valida que la tarea exista
        return tareaLogRepository.buscarPorTareaProgramadaId(id);
    }

    // [QUÉ]: Devuelve las últimas (n) ejecuciones de TODAS las tareas, más recientes
    //        primero. Alimenta la vista global de monitoreo (ver los procesos que
    //        corrieron juntos y su resultado). El límite se acota a [1, 100].
    public List<TareaLog> obtenerUltimasEjecuciones(int limite) {
        if (limite < 1 || limite > MAX_LOG_LIMITE) {
            throw new DomainException("El límite de ejecuciones debe estar entre 1 y " + MAX_LOG_LIMITE);
        }
        return tareaLogRepository.listarUltimas(limite);
    }

    // [QUÉ]: Lista las fuentes candidatas para programar: "Catálogo global" + ligas
    //        activas con sus detalles de fuente, marcando las que ya tienen tarea.
    // [POR QUÉ]: El selector del modal de creación debe ocultar/avisar los duplicados
    //            (unicidad por liga+tipo).
    public List<FuenteDisponible> listarFuentesDisponibles() {
        List<FuenteDisponible> resultado = new ArrayList<>();
        resultado.add(new FuenteDisponible(null, "Catálogo global", null,
                tareaProgramadaRepository.buscarGlobal().isPresent()));

        for (Liga liga : ligaRepository.buscarActivas()) {
            // [POR QUÉ]: EQUIPOS no requiere DetalleFuenteExtraccion (la #6 no usa
            //            path_to_scrape): se ofrece siempre para ligas activas (H-07).
            boolean equiposYaProgramada = tareaProgramadaRepository
                    .buscarPorLigaIdYTipoFuente(liga.id(), TipoFuenteExtraccion.EQUIPOS).isPresent();
            resultado.add(new FuenteDisponible(liga.id(), liga.nombre(),
                    TipoFuenteExtraccion.EQUIPOS, equiposYaProgramada));

            for (DetalleFuenteExtraccion detalle : detalleRepository.buscarPorLiga(liga.id())) {
                if (!detalle.activa()) {
                    continue;
                }
                boolean yaProgramada = tareaProgramadaRepository
                        .buscarPorLigaIdYTipoFuente(liga.id(), detalle.tipo()).isPresent();
                resultado.add(new FuenteDisponible(liga.id(), liga.nombre(), detalle.tipo(), yaProgramada));
            }
        }
        return resultado;
    }

    // [QUÉ]: Resuelve el nombre de una liga para la vista (null si ligaId es null o la
    //        liga ya no existe).
    // [POR QUÉ]: La columna "Fuente" del listado necesita el nombre sin forzar al
    //            frontend a otro GET /ligas por cada tarea.
    public String obtenerNombreLiga(UUID ligaId) {
        if (ligaId == null) {
            return null;
        }
        return ligaRepository.buscarPorId(ligaId).map(Liga::nombre).orElse(null);
    }

    // [QUÉ]: Valida la unicidad por (liga, tipo) o de la tarea global.
    private void validarUnicidad(UUID ligaId, TipoFuenteExtraccion tipoFuente) {
        boolean existe;
        if (ligaId == null && tipoFuente == null) {
            existe = tareaProgramadaRepository.buscarGlobal().isPresent();
        } else {
            existe = tareaProgramadaRepository.buscarPorLigaIdYTipoFuente(ligaId, tipoFuente).isPresent();
        }
        if (existe) {
            throw new DomainException("Ya existe una tarea programada para la liga y tipo dados");
        }
    }

    // [QUÉ]: Resuelve el cron final: prioriza la frecuencia amigable, luego el cron
    //        crudo (validado), y usa un default si no se aporta ninguno.
    private String resolverCron(String cron, Frecuencia frecuencia) {
        if (frecuencia != null) {
            return frecuencia.toCronExpression();
        }
        if (cron == null || cron.isBlank()) {
            return CRON_DEFAULT;
        }
        if (!CronExpression.isValidExpression(cron)) {
            throw new DomainException("Expresión cron inválida: " + cron);
        }
        return cron;
    }
}