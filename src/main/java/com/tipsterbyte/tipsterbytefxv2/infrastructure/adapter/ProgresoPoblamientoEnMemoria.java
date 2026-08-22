// ─────────────────────────────────────────────
// [QUÉ]: Implementación en memoria del puerto ProgresoPoblamiento (FASE T3).
// [POR QUÉ]: El progreso es estado volátil de la ejecución en curso (no persistente):
//            vive mientras corre el poblamiento y se reinicia al iniciar uno nuevo.
//            Volátil para lectura segura desde el hilo HTTP (polling) mientras el
//            hilo virtual del poblamiento escribe.
// [ALTERNATIVAS]: Guardar progreso en Redis; se descarta porque es estado efímero de
//                 una sola instancia y no debe sobrevivir a un reinicio de la app.
// [RELACIONES]: Implementa application.port.ProgresoPoblamiento; escrito por CU-10;
//               leído por SincronizarCatalogoAsyncUseCase/CatalogoController (FASE T3).
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.adapter;

import com.tipsterbyte.tipsterbytefxv2.application.port.ProgresoPoblamiento;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ProgresoPoblamientoEnMemoria implements ProgresoPoblamiento {

    private final AtomicReference<Progreso> snapshot = new AtomicReference<>();

    @Override
    public void actualizar(String paisEnCurso, int paisesProcesados) {
        snapshot.set(new Progreso(paisEnCurso, paisesProcesados));
    }

    @Override
    public Optional<Progreso> snapshot() {
        return Optional.ofNullable(snapshot.get());
    }

    @Override
    public void reiniciar() {
        snapshot.set(null);
    }
}
