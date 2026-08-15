// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-06 (HU-06): crea un pronóstico en BORRADOR para un tipster
//        sobre un partido jugable y con cuota > 1.0.
// [POR QUÉ]: Valida en el caso de uso que el partido sea jugable (BR-004) y deja que
//            los VOs validen la cuota (BR-007) y la coherencia mercado/selección.
// [ALTERNATIVAS]: Validar el partido dentro del aggregate Pronostico; se descarta porque
//                 el partido es otro aggregate (se referencia por id).
// [RELACIONES]: HU-06 → CU-06 → PronosticoRepository + PartidoRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.dto.CrearPronosticoComando;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PronosticoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Cuota;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Pronostico;
import com.tipsterbyte.tipsterbytefxv2.domain.model.SeleccionPronostico;

import java.math.BigDecimal;
import java.util.UUID;

public final class CrearPronosticoUseCase {

    private final PronosticoRepository pronosticoRepository;
    private final PartidoRepository partidoRepository;

    // [QUÉ]: Construye el caso de uso con sus puertos (inyección por constructor).
    public CrearPronosticoUseCase(PronosticoRepository pronosticoRepository, PartidoRepository partidoRepository) {
        this.pronosticoRepository = pronosticoRepository;
        this.partidoRepository = partidoRepository;
    }

    // [QUÉ]: Ejecuta CU-06: valida el partido jugable (BR-004), construye el pronóstico
    //        en BORRADOR y lo persiste. Devuelve el id del pronóstico creado.
    public UUID ejecutar(CrearPronosticoComando comando) {
        Partido partido = partidoRepository.buscarPorId(comando.partidoId())
                .orElseThrow(() -> new DomainException("Partido no encontrado: " + comando.partidoId()));
        if (partido.estado() != EstadoPartido.PROGRAMADO && partido.estado() != EstadoPartido.EN_VIVO) {
            throw new DomainException("No se puede crear un pronóstico sobre un partido no jugable (BR-004)");
        }

        SeleccionPronostico seleccion = new SeleccionPronostico(comando.mercado(), comando.resultadoEsperado());
        Cuota cuota = new Cuota(seleccion.mercado(), comando.cuotaValor()); // BR-007: valor > 1.0
        Pronostico pronostico = new Pronostico(comando.tipsterId(), comando.partidoId(), seleccion, cuota);

        pronosticoRepository.guardar(pronostico);
        return pronostico.id();
    }
}