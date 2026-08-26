// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-11: gestiona el catálogo de fuentes de extracción
//        (registrar fuente, listar fuentes, asociar una URL de fuente a una liga).
// [POR QUÉ]: El usuario administra el catálogo de fuentes (posiciones, cuotas Wplay,
//            calendario) y asocia cada una a una liga con su URL real. Sin esta
//            gestión los adapters de sincronización no tendrían URL que consultar.
//            La asociación se aplica a la temporada vigente de la liga (activa o
//            primera registrada, Bridge Fix Torneos/Temporadas).
// [ALTERNATIVAS]: Fuentes fijas como enum; se descarta porque el usuario pidió un
//                 catálogo gestionable que permita ampliar sin código.
// [RELACIONES]: HU-11 → CU-11 → FuenteExtraccionRepository + DetalleFuenteExtraccionRepository
//               + TemporadaRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.AsociarUrlFuenteComando;
import com.tipsterbyte.tipsterbytefxv2.application.dto.RegistrarFuenteComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.FuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.TemporadaRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.event.DomainEvent;
import com.tipsterbyte.tipsterbytefxv2.domain.model.DetalleFuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.FuenteExtraccion;
import com.tipsterbyte.tipsterbytefxv2.domain.model.TipoFuenteExtraccion;

import java.util.List;

public final class GestionarFuenteExtraccionUseCase {

    private final FuenteExtraccionRepository fuenteRepository;
    private final DetalleFuenteExtraccionRepository detalleRepository;
    private final TemporadaRepository temporadaRepository;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public GestionarFuenteExtraccionUseCase(FuenteExtraccionRepository fuenteRepository,
                                            DetalleFuenteExtraccionRepository detalleRepository,
                                            TemporadaRepository temporadaRepository) {
        this.fuenteRepository = fuenteRepository;
        this.detalleRepository = detalleRepository;
        this.temporadaRepository = temporadaRepository;
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
                comando.nombre(), comando.tipo(), comando.activa(), comando.urlBase()));
        return List.of();
    }

    // [QUÉ]: Ejecuta CU-11 edición: actualiza nombre/url base/estado de una fuente
    //        existente identificada por su tipo (clave natural única del catálogo).
    // [POR QUÉ]: El SUPERADMIN corrige la url_base_fuente desde el formulario sin
    //            re-registrar (registrarFuente rechaza tipos duplicados). La edición
    //            reconstruye el aggregate (inmutable) conservando su id.
    public FuenteExtraccion editarFuente(TipoFuenteExtraccion tipo, String nombre,
                                         String urlBase, boolean activa) {
        if (tipo == null) {
            throw new DomainException("Editar fuente requiere tipo");
        }
        FuenteExtraccion existente = fuenteRepository.buscarPorTipo(tipo)
                .orElseThrow(() -> new DomainException("No existe fuente registrada para el tipo: " + tipo));
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException("Editar fuente requiere nombre");
        }
        FuenteExtraccion editada = new FuenteExtraccion(
                existente.id(), nombre.trim(), tipo, activa, urlBase);
        fuenteRepository.guardar(editada);
        return editada;
    }

    // [QUÉ]: Ejecuta CU-11 listado: devuelve todas las fuentes del catálogo.
    public List<FuenteExtraccion> listarFuentes() {
        return fuenteRepository.buscarTodas();
    }

    // [QUÉ]: Ejecuta CU-11 asociación: asocia una URL de fuente a la temporada vigente
    //        de la liga, actualizando la URL existente para ese (temporadaId, tipo) si
    //        ya existe, o creando el detalle nuevo si no existe.
    // [POR QUÉ]: CU-04 también delega aquí la creación de los detalles, garantizando
    //            la unicidad por (temporadaId, tipo) al hacer update en lugar de duplicar.
    public List<DomainEvent> asociarUrlFuente(AsociarUrlFuenteComando comando) {
        FuenteExtraccion fuente = fuenteRepository.buscarPorTipo(comando.tipo())
                .orElseThrow(() -> new DomainException("No existe fuente registrada para el tipo: " + comando.tipo()));
        java.util.UUID temporadaId = resolverTemporadaVigente(comando.ligaId()).id();
        detalleRepository.buscarPorTemporadaYTipo(temporadaId, comando.tipo())
                .ifPresentOrElse(
                        detalle -> detalleRepository.guardar(
                                new DetalleFuenteExtraccion(
                                        detalle.id(), temporadaId, detalle.fuente(),
                                        comando.url(), comando.activa())),
                        () -> detalleRepository.guardar(new DetalleFuenteExtraccion(
                                temporadaId, fuente, comando.url(), comando.activa())));
        return List.of();
    }

    // [QUÉ]: Ejecuta CU-11 consulta: devuelve los detalles de fuentes de una liga.
    // [POR QUÉ]: Expone a las interfaces qué URL tiene cada fuente de una liga.
    public List<DetalleFuenteExtraccion> listarDetallesDeLiga(java.util.UUID ligaId) {
        return detalleRepository.buscarPorLiga(ligaId);
    }

    // [QUÉ]: Resuelve la temporada vigente de la liga: la ACTIVA o, en su defecto, la
    //        primera registrada (liga recién poblada por CU-10 está PLANIFICADA).
    private com.tipsterbyte.tipsterbytefxv2.domain.model.Temporada resolverTemporadaVigente(java.util.UUID ligaId) {
        return temporadaRepository.buscarActivaPorLigaId(ligaId)
                .or(() -> temporadaRepository.buscarPorLigaId(ligaId).stream().findFirst())
                .orElseThrow(() -> new DomainException(
                        "La liga no tiene temporadas registradas: " + ligaId));
    }
}
