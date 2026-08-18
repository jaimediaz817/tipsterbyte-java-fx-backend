// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-14: gestiona los países de interés del catálogo (registrar,
//        listar, eliminar y reemplazar la lista completa ordenada).
// [POR QUÉ]: El frontend configura, antes de poblar, qué países se sincronizan con
//            prioridad (CU-10 los procesa primero; el resto del mundo no se omite).
//            La prioridad se deriva aquí (siguiente libre al registrar, posición en
//            la lista al reemplazar) para que la UI no calcule orden.
// [ALTERNATIVAS]: Flag en Pais; se descarta porque es una lista con orden propia.
// [RELACIONES]: HU-10 → CU-14 → PaisInteresRepository + ProveedorPaises (validación
//               de que el país existe en la fuente #1); CU-10 consume PaisInteresRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.PaisFuente;
import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarPaisInteresComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisInteresRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.PaisInteres;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GestionarPaisesInteresUseCase {

    private final PaisInteresRepository paisInteresRepository;
    private final ProveedorPaises proveedorPaises;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public GestionarPaisesInteresUseCase(PaisInteresRepository paisInteresRepository,
                                         ProveedorPaises proveedorPaises) {
        this.paisInteresRepository = paisInteresRepository;
        this.proveedorPaises = proveedorPaises;
    }

    // [QUÉ]: Ejecuta CU-14 alta: registra un país de interés al final de la lista y
    //        devuelve la entidad guardada (el controller la mapea a PaisInteresResponse).
    // [POR QUÉ]: Solo se aceptan países que existan en la fuente #1 (regla de negocio:
    //            la preferencia se elige sobre los países disponibles). Si ya existe
    //            la preferencia se actualiza (nombre) manteniendo su prioridad. El
    //            retorno expone la prioridad asignada al POST (el frontend la usa para
    //            el UI sin recalcularla).
    public PaisInteres registrar(RegistrarPaisInteresComando comando) {
        String isoAlpha2 = comando.isoAlpha2().trim().toUpperCase();
        validarDisponibleEnFuente(isoAlpha2);
        PaisInteres pais = paisInteresRepository.buscarPorIsoAlpha2(isoAlpha2)
                .map(existente -> new PaisInteres(
                        existente.id(), isoAlpha2, comando.nombre(), existente.prioridad()))
                .orElseGet(() -> new PaisInteres(isoAlpha2, comando.nombre(), siguientePrioridad()));
        paisInteresRepository.guardar(pais);
        return pais;
    }

    // [QUÉ]: Ejecuta CU-14 consulta: lista los países de interés por prioridad ascendente.
    public List<PaisInteres> listar() {
        return paisInteresRepository.listarPorPrioridad();
    }

    // [QUÉ]: Ejecuta CU-14 baja: elimina un país de interés por su iso_alpha2.
    public List<DomainEvent> eliminar(String isoAlpha2) {
        String iso = isoAlpha2 == null ? null : isoAlpha2.trim().toUpperCase();
        if (iso == null || iso.isBlank()) {
            throw new DomainException("Eliminar país de interés requiere isoAlpha2");
        }
        if (paisInteresRepository.buscarPorIsoAlpha2(iso).isEmpty()) {
            throw new DomainException("No existe país de interés registrado: " + isoAlpha2);
        }
        paisInteresRepository.eliminar(iso);
        return List.of();
    }

    // [QUÉ]: Ejecuta CU-14 reemplazo: reemplaza la lista completa de países de interés
    //        con la lista enviada en orden (prioridad = posición 1..n) y elimina los
    //        que ya no están. Es la operación de "guardar preferencias" en bloque.
    public List<DomainEvent> reemplazarPreferencias(List<RegistrarPaisInteresComando> preferencias) {
        Set<String> isoDisponibles = paisesDisponibles();
        for (RegistrarPaisInteresComando comando : preferencias) {
            validarEnFuente(comando.isoAlpha2(), isoDisponibles);
        }
        int prioridad = 1;
        for (RegistrarPaisInteresComando comando : preferencias) {
            guardarConPrioridad(comando, prioridad++);
        }
        Set<String> isoNuevos = new HashSet<>();
        for (RegistrarPaisInteresComando comando : preferencias) {
            isoNuevos.add(comando.isoAlpha2().trim().toUpperCase());
        }
        for (PaisInteres existente : paisInteresRepository.listarPorPrioridad()) {
            if (!isoNuevos.contains(existente.isoAlpha2())) {
                paisInteresRepository.eliminar(existente.isoAlpha2());
            }
        }
        return List.of();
    }

    // [QUÉ]: Guarda una preferencia reutilizando el id existente si ya está registrada
    //        (upsert), para no violar la unicidad de iso_alpha2.
    private void guardarConPrioridad(RegistrarPaisInteresComando comando, int prioridad) {
        String iso = comando.isoAlpha2().trim().toUpperCase();
        paisInteresRepository.buscarPorIsoAlpha2(iso)
                .map(existente -> new PaisInteres(
                        existente.id(), iso, comando.nombre(), prioridad))
                .ifPresentOrElse(
                        paisInteresRepository::guardar,
                        () -> paisInteresRepository.guardar(new PaisInteres(
                                iso, comando.nombre(), prioridad)));
    }

    // [QUÉ]: Calcula la prioridad siguiente (máxima + 1) para registrar al final.
    private int siguientePrioridad() {
        return paisInteresRepository.listarPorPrioridad().stream()
                .mapToInt(PaisInteres::prioridad)
                .max()
                .orElse(0) + 1;
    }

    // [QUÉ]: Valida que el iso_alpha2 exista en la fuente #1 (obteniendo el listado).
    private void validarDisponibleEnFuente(String isoAlpha2) {
        validarEnFuente(isoAlpha2, paisesDisponibles());
    }

    private Set<String> paisesDisponibles() {
        Set<String> disponibles = new HashSet<>();
        for (PaisFuente pais : proveedorPaises.obtenerPaises()) {
            disponibles.add(pais.isoAlpha2().toUpperCase());
        }
        return disponibles;
    }

    private void validarEnFuente(String isoAlpha2, Set<String> isoDisponibles) {
        if (isoAlpha2 == null || isoAlpha2.isBlank()) {
            throw new DomainException("País de interés requiere isoAlpha2");
        }
        if (!isoDisponibles.contains(isoAlpha2.trim().toUpperCase())) {
            throw new DomainException("El país no está disponible en la fuente de países: " + isoAlpha2);
        }
    }
}