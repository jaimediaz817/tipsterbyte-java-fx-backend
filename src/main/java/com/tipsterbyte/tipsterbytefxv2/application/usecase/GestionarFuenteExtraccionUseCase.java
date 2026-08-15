// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-11: gestiona el catálogo de fuentes de extracción
//        (registrar fuente, listar fuentes, asociar una URL de fuente a una liga).
// [POR QUÉ]: El usuario administra el catálogo de fuentes (posiciones, cuotas Wplay,
//            calendario) y asocia cada una a una liga con su URL real. Sin esta
//            gestión los adapters de sincronización no tendrían URL que consultar.
// [ALTERNATIVAS]: Fuentes fijas como enum; se descarta porque el usuario pidió un
//                 catálogo gestionable que permita ampliar sin código.
// [RELACIONES]: HU-11 → CU-11 → FuenteExtraccionRepository + DetalleFuenteExtraccionRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.AsociarUrlFuenteComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarFuenteComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.FuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;

import java.util.List;

public final class GestionarFuenteExtraccionUseCase {

    private final FuenteExtraccionRepository fuenteRepository;
    private final DetalleFuenteExtraccionRepository detalleRepository;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public GestionarFuenteExtraccionUseCase(FuenteExtraccionRepository fuenteRepository,
                                            DetalleFuenteExtraccionRepository detalleRepository) {
        this.fuenteRepository = fuenteRepository;
        this.detalleRepository = detalleRepository;
    }

    // [QUÉ]: Ejecuta CU-11 alta: registra una fuente en el catálogo si no existe
    //        otra con el mismo tipo (un tipo = una fuente del catálogo).
    public List<DomainEvent> registrarFuente(RegistrarFuenteComando comando) {
        if (comando.tipo() == null) {
            throw new DomainException("Registrar fuente requiere tipo");
        }
        if (fuenteRepository.buscarPorTipo(comando.tipo()).isPresent()) {
            throw new DomainException("Ya existe una fuente registrada para el tipo: " + comando.tipo());
        }
        fuenteRepository.guardar(new FuenteExtraccion(
                comando.nombre(), comando.tipo(), comando.activa()));
        return List.of();
    }

    // [QUÉ]: Ejecuta CU-11 listado: devuelve todas las fuentes del catálogo.
    public List<FuenteExtraccion> listarFuentes() {
        return fuenteRepository.buscarTodas();
    }

    // [QUÉ]: Ejecuta CU-11 asociación: asocia una URL de fuente a una liga,
    //        actualizando la URL existente para ese (ligaId, tipo) si ya existe,
    //        o creando el detalle nuevo si no existe.
    // [POR QUÉ]: CU-04 también delega aquí la creación de los detalles, garantizando
    //            la unicidad por (ligaId, tipo) al hacer update en lugar de duplicar.
    public List<DomainEvent> asociarUrlFuente(AsociarUrlFuenteComando comando) {
        FuenteExtraccion fuente = fuenteRepository.buscarPorTipo(comando.tipo())
                .orElseThrow(() -> new DomainException("No existe fuente registrada para el tipo: " + comando.tipo()));
        detalleRepository.buscarPorLigaYTipo(comando.ligaId(), comando.tipo())
                .ifPresentOrElse(
                        detalle -> detalleRepository.guardar(
                                new DetalleFuenteExtraccion(
                                        detalle.id(), detalle.ligaId(), detalle.fuente(),
                                        comando.url(), comando.activa())),
                        () -> detalleRepository.guardar(new DetalleFuenteExtraccion(
                                comando.ligaId(), fuente, comando.url(), comando.activa())));
        return List.of();
    }

    // [QUÉ]: Ejecuta CU-11 consulta: devuelve los detalles de fuentes de una liga.
    // [POR QUÉ]: Expone a las interfaces qué URL tiene cada fuente de una liga.
    public List<DetalleFuenteExtraccion> listarDetallesDeLiga(java.util.UUID ligaId) {
        return detalleRepository.buscarPorLiga(ligaId);
    }
}
