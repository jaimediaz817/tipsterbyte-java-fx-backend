// ─────────────────────────────────────────────
// [QUÉ]: Caso de uso CU-05 (HU-05): registra el resultado final de un partido,
//        marcándolo como FINALIZADO.
// [POR QUÉ]: Aplica BR-003: el resultado solo se asigna cuando el partido finalizó.
//            El caso de uso finaliza el partido (si aplica) y delega la validación
//            de estado al aggregate Partido.
// [ALTERNATIVAS]: Asignar el resultado sin finalizar; se descarta porque el aggregate
//                 rechaza la asignación en estados no finalizados (BR-003).
// [RELACIONES]: HU-05 → CU-05 → PartidoRepository.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.application.usecase;

import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;
import com.tipsterbyte.tipsterbytefxv2.domain.model.EstadoPartido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Partido;
import com.tipsterbyte.tipsterbytefxv2.domain.model.Resultado;

import java.util.UUID;

public final class RegistrarResultadoUseCase {

    private final PartidoRepository partidoRepository;

    // [QUÉ]: Construye el caso de uso con su puerto (inyección por constructor).
    public RegistrarResultadoUseCase(PartidoRepository partidoRepository) {
        this.partidoRepository = partidoRepository;
    }

    // [QUÉ]: Ejecuta CU-05: finaliza el partido si aplica y asigna el resultado (BR-003).
    public void ejecutar(UUID partidoId, Resultado resultado) {
        Partido partido = partidoRepository.buscarPorId(partidoId)
                .orElseThrow(() -> new DomainException("Partido no encontrado: " + partidoId));

        if (partido.resultado() != null) {
            throw new DomainException("El resultado ya fue registrado y no se modifica (BR-003)");
        }
        if (partido.estado() != EstadoPartido.FINALIZADO) {
            partido.finalizar();
        }
        partido.asignarResultado(resultado); // exige estado FINALIZADO (BR-003)
        partidoRepository.guardar(partido);
    }
}