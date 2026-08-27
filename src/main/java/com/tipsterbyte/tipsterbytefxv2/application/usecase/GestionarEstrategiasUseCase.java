// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-23 (HU-16): CRUD de estrategias de pronóstico.
// [POR QUÉ]: El tipster/superadmin crea, lista, actualiza y elimina estrategias
//            con criterios dinámicos que el motor de evaluación procesará.
// [ALTERNATIVAS]: CRUD directo en controller; se descarta por separar lógica de negocio.
// [RELACIONES]: HU-16 AC1/12/17/18 → EstrategiaRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.EstrategiaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Criterio;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Estrategia;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Mercado;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class GestionarEstrategiasUseCase {

    private static final int MAX_ESTRATEGIAS_ACTIVAS_POR_TIPSTER = 10;

    private final EstrategiaRepository estrategiaRepository;

    public GestionarEstrategiasUseCase(EstrategiaRepository estrategiaRepository) {
        this.estrategiaRepository = estrategiaRepository;
    }

    // [QUÉ]: Crea una estrategia nueva con criterios (AC12 POST).
    public Estrategia crear(String nombre, UUID tipsterId, Mercado mercado,
                            Integer maxPartidos, BigDecimal confianzaMinima,
                            List<Criterio> criterios, List<UUID> ligaIds) {
        // AC18: máximo 10 estrategias activas por tipster.
        long activas = estrategiaRepository.contarActivasPorTipsterId(tipsterId);
        if (activas >= MAX_ESTRATEGIAS_ACTIVAS_POR_TIPSTER) {
            throw new DomainException("Un tipster no puede tener más de " + MAX_ESTRATEGIAS_ACTIVAS_POR_TIPSTER + " estrategias activas (AC18 HU-16)");
        }
        // AC17: validar combinaciones válidas de criterios.
        validarCriterios(criterios);

        Estrategia estrategia = new Estrategia(nombre, tipsterId, mercado,
                maxPartidos, confianzaMinima, criterios, ligaIds);
        estrategiaRepository.guardar(estrategia);
        return estrategia;
    }

    // [QUÉ]: Actualiza una estrategia existente (AC12 PUT).
    public Estrategia actualizar(UUID id, String nombre, Mercado mercado,
                                  Integer maxPartidos, BigDecimal confianzaMinima,
                                  List<Criterio> criterios, List<UUID> ligaIds) {
        Estrategia existente = estrategiaRepository.buscarPorId(id)
                .orElseThrow(() -> new DomainException("Estrategia no encontrada: " + id));

        validarCriterios(criterios);

        Estrategia actualizada = new Estrategia(
                id, nombre != null ? nombre : existente.nombre(),
                existente.tipsterId(),
                mercado != null ? mercado : existente.mercado(),
                maxPartidos != null ? maxPartidos : existente.maxPartidos(),
                confianzaMinima != null ? confianzaMinima : existente.confianzaMinima(),
                existente.activa(),
                criterios != null ? criterios : existente.criterios(),
                ligaIds != null ? ligaIds : existente.ligaIds(),
                existente.createdAt());
        estrategiaRepository.guardar(actualizada);
        return actualizada;
    }

    // [QUÉ]: Activa/desactiva una estrategia (AC12 DELETE/PUT toggle).
    public void cambiarEstado(UUID id, boolean activa) {
        Estrategia existente = estrategiaRepository.buscarPorId(id)
                .orElseThrow(() -> new DomainException("Estrategia no encontrada: " + id));
        if (activa) {
            long activas = estrategiaRepository.contarActivasPorTipsterId(existente.tipsterId());
            if (activas >= MAX_ESTRATEGIAS_ACTIVAS_POR_TIPSTER) {
                throw new DomainException("Límite de estrategias activas alcanzado (AC18 HU-16)");
            }
            existente.activar();
        } else {
            existente.desactivar();
        }
        estrategiaRepository.guardar(existente);
    }

    // [QUÉ]: Elimina una estrategia (AC12 DELETE).
    public void eliminar(UUID id) {
        if (estrategiaRepository.buscarPorId(id).isEmpty()) {
            throw new DomainException("Estrategia no encontrada: " + id);
        }
        estrategiaRepository.eliminar(id);
    }

    // [QUÉ]: Lista estrategias de un tipster (o todas si es superadmin).
    public List<Estrategia> listar(UUID tipsterId) {
        if (tipsterId != null) {
            return estrategiaRepository.buscarPorTipsterId(tipsterId);
        }
        return estrategiaRepository.buscarActivas();
    }

    // [QUÉ]: Obtiene una estrategia por ID (AC12 GET detalle).
    public Optional<Estrategia> obtenerPorId(UUID id) {
        return estrategiaRepository.buscarPorId(id);
    }

    // [QUÉ]: Valida que cada criterio tenga una combinación válida de (fuente, campo, operador).
    private void validarCriterios(List<Criterio> criterios) {
        if (criterios == null || criterios.isEmpty()) {
            throw new DomainException("La estrategia debe tener al menos un criterio");
        }
        for (Criterio c : criterios) {
            if (c.fuente() == Criterio.FuenteCriterio.CUOTAS && !c.campo().startsWith("cuota")) {
                throw new DomainException("Criterio CUOTAS debe usar campo de cuota (cuota_1x, cuota_local, etc.)");
            }
            if (c.fuente() == Criterio.FuenteCriterio.POSICIONES && c.campo().startsWith("cuota")) {
                throw new DomainException("Criterio POSICIONES no puede usar campo de cuota");
            }
        }
    }
}
