// ─────────────────────────────────────────────
// [QUÉ]: Configuración de infraestructura que registra los casos de uso de application
//        como beans de Spring (wiring REST de FASE 8.5).
// [POR QUÉ]: Los casos de uso son POJOs de la capa application (sin anotaciones de
//            Spring) para mantener la Dependency Rule. Al activar app.api.rest.enabled
//            los controllers exigen estos beans; esta configuración los declara
//            explícitamente con sus dependencias (puertos adaptados por infrastructure).
// [ALTERNATIVAS]: Anotar cada use case con @Component; se descarta porque acoplaría la
//                 capa application a Spring. Un @Configuration central concentra el
//                 wiring en infraestructura.
// [RELACIONES]: Crea los beans que inyectan LigaController, PronosticoController,
//               SuscripcionController, PartidoController y FuenteExtraccionController.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.infrastructure.config;

import com.tipsterbyte.tipsterbytefxv2.application.port.CacheLecturas;
import com.tipsterbyte.tipsterbytefxv2.application.port.CuotaHistorialRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.DetalleFuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.EquiposAliasRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.FuenteExtraccionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.LigaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisInteresRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PaisRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PartidoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.PasswordHasher;
import com.tipsterbyte.tipsterbytefxv2.application.port.PronosticoRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorCalendario;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorCuotas;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorEquiposPorLiga;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPartidosProximosWplay;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProgresoPoblamiento;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaLogRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.DetectarDiscrepanciasEquiposUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCatalogoAsyncUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorLigasPorPais;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPaises;
import com.tipsterbyte.tipsterbytefxv2.application.port.ProveedorPosiciones;
import com.tipsterbyte.tipsterbytefxv2.application.port.SuscripcionRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaLogRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaProgramadaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.TemporadaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.port.TokenEmisor;
import com.tipsterbyte.tipsterbytefxv2.application.port.UsuarioRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ActivarLigaUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.AutenticarUsuarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarEstadoCatalogoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarPronosticosUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.ObtenerJornadaActualUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.CrearPronosticoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.CrearSuscripcionUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.GestionarFuenteExtraccionUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.GestionarPaisesInteresUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.PublicarPronosticoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.RegistrarResultadoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.RegistrarUsuarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCalendarioUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCatalogoUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarEquiposLigaUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarCuotasUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarPosicionesUseCase;
import com.tipsterbyte.tipsterbytefxv2.application.port.TareaProgramadaRepository;
import com.tipsterbyte.tipsterbytefxv2.application.usecase.GestionarTareasProgramasUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    // [QUÉ]: Bean de CU-04 (activar liga con URLs de fuentes, asociadas a su temporada).
    @Bean
    public ActivarLigaUseCase activarLigaUseCase(LigaRepository ligaRepository,
                                                 FuenteExtraccionRepository fuenteRepository,
                                                 DetalleFuenteExtraccionRepository detalleRepository,
                                                 TemporadaRepository temporadaRepository) {
        return new ActivarLigaUseCase(ligaRepository, fuenteRepository, detalleRepository, temporadaRepository);
    }

    // [QUÉ]: Bean de CU-01 (sincronizar posiciones, con invalidación de cache FASE 12).
    @Bean
    public SincronizarPosicionesUseCase sincronizarPosicionesUseCase(LigaRepository ligaRepository,
                                                                     ProveedorPosiciones proveedorPosiciones,
                                                                     CacheLecturas cacheLecturas) {
        return new SincronizarPosicionesUseCase(ligaRepository, proveedorPosiciones, cacheLecturas);
    }

    // [QUÉ]: Bean de CU-02 (sincronizar calendario, con invalidación de cache FASE 12).
    @Bean
    public SincronizarCalendarioUseCase sincronizarCalendarioUseCase(LigaRepository ligaRepository,
                                                                     PartidoRepository partidoRepository,
                                                                     ProveedorCalendario proveedorCalendario,
                                                                     CacheLecturas cacheLecturas) {
        return new SincronizarCalendarioUseCase(ligaRepository, partidoRepository, proveedorCalendario, cacheLecturas);
    }

    // [QUÉ]: Bean de CU-03 (sincronizar cuotas, con invalidación de cache FASE 12,
    //        resolución multi-fuente HU-14 AC4.2/4.3, historial AC4.5).
    @Bean
    public SincronizarCuotasUseCase sincronizarCuotasUseCase(PartidoRepository partidoRepository,
                                                             ProveedorCuotas proveedorCuotas,
                                                             CacheLecturas cacheLecturas,
                                                             ProveedorPartidosProximosWplay proveedorPartidosWplay,
                                                             TemporadaRepository temporadaRepository,
                                                             EquiposAliasRepository equiposAliasRepository,
                                                             CuotaHistorialRepository cuotaHistorialRepository) {
        return new SincronizarCuotasUseCase(partidoRepository, proveedorCuotas, cacheLecturas,
                proveedorPartidosWplay, temporadaRepository, equiposAliasRepository, cuotaHistorialRepository);
    }

    // [QUÉ]: Bean de CU-05 (registrar resultado de partido).
    @Bean
    public RegistrarResultadoUseCase registrarResultadoUseCase(PartidoRepository partidoRepository) {
        return new RegistrarResultadoUseCase(partidoRepository);
    }

    // [QUÉ]: Bean de CU-06 (crear pronóstico).
    @Bean
    public CrearPronosticoUseCase crearPronosticoUseCase(PronosticoRepository pronosticoRepository,
                                                         PartidoRepository partidoRepository) {
        return new CrearPronosticoUseCase(pronosticoRepository, partidoRepository);
    }

    // [QUÉ]: Bean de CU-07 (publicar pronóstico).
    @Bean
    public PublicarPronosticoUseCase publicarPronosticoUseCase(PronosticoRepository pronosticoRepository,
                                                               PartidoRepository partidoRepository) {
        return new PublicarPronosticoUseCase(pronosticoRepository, partidoRepository);
    }

    // [QUÉ]: Bean de CU-08 (consultar pronósticos públicos).
    @Bean
    public ConsultarPronosticosUseCase consultarPronosticosUseCase(SuscripcionRepository suscripcionRepository,
                                                                   PartidoRepository partidoRepository,
                                                                   PronosticoRepository pronosticoRepository) {
        return new ConsultarPronosticosUseCase(suscripcionRepository, partidoRepository, pronosticoRepository);
    }

    // [QUÉ]: Bean de CU-09 (crear suscripción).
    @Bean
    public CrearSuscripcionUseCase crearSuscripcionUseCase(SuscripcionRepository suscripcionRepository) {
        return new CrearSuscripcionUseCase(suscripcionRepository);
    }

    // [QUÉ]: Bean de CU-10 (sincronizar catálogo de países y ligas, con prioridad de
    //        poblamiento por países de interés, límite maxLigasPorPais, invalidación del
    //        cache de países y plantilla de equipos desde la fuente #6 para países de
    //        interés — HU-11).
    // [QUÉ]: Bean de CU-16 (poblar plantilla de equipos de una liga desde la fuente #6).
    @Bean
    public SincronizarEquiposLigaUseCase sincronizarEquiposLigaUseCase(
            ProveedorEquiposPorLiga proveedorEquiposPorLiga,
            CacheLecturas cacheLecturas,
            LigaRepository ligaRepository) {
        return new SincronizarEquiposLigaUseCase(proveedorEquiposPorLiga, cacheLecturas, ligaRepository);
    }

    // [QUÉ]: Bean de CU-10 con reporte de progreso (FASE T3): alimenta el snapshot que
    //        lee el endpoint GET /catalogo/activar/{executionId} durante el polling.
    @Bean
    public SincronizarCatalogoUseCase sincronizarCatalogoUseCase(ProveedorPaises proveedorPaises,
                                                                 ProveedorLigasPorPais proveedorLigasPorPais,
                                                                 SincronizarEquiposLigaUseCase sincronizarEquiposLigaUseCase,
                                                                 PaisRepository paisRepository,
                                                                 LigaRepository ligaRepository,
                                                                 PaisInteresRepository paisInteresRepository,
                                                                 CacheLecturas cacheLecturas,
                                                                 ProgresoPoblamiento progresoPoblamiento) {
        return new SincronizarCatalogoUseCase(proveedorPaises, proveedorLigasPorPais,
                sincronizarEquiposLigaUseCase, paisRepository, ligaRepository,
                paisInteresRepository, cacheLecturas, progresoPoblamiento);
    }

    // [QUÉ]: Bean H-04 (diagnóstico de duplicados de equipos por temporada).
    @Bean
    public DetectarDiscrepanciasEquiposUseCase detectarDiscrepanciasEquiposUseCase(
            LigaRepository ligaRepository) {
        return new DetectarDiscrepanciasEquiposUseCase(ligaRepository);
    }

    // [QUÉ]: Bean del wrapper asíncrono de CU-10 (FASE T3 / H-02): 202 + polling.
    @Bean
    public SincronizarCatalogoAsyncUseCase sincronizarCatalogoAsyncUseCase(
            SincronizarCatalogoUseCase sincronizarCatalogoUseCase,
            TareaLogRepository tareaLogRepository,
            ProgresoPoblamiento progresoPoblamiento) {
        return new SincronizarCatalogoAsyncUseCase(sincronizarCatalogoUseCase,
                tareaLogRepository, progresoPoblamiento);
    }

    // [QUÉ]: Bean de consulta del estado del catálogo (CU-10): deriva VACIO/POBLADO
    //        de los conteos reales de países y ligas para el panel del SUPERADMIN.
    @Bean
    public ConsultarEstadoCatalogoUseCase consultarEstadoCatalogoUseCase(PaisRepository paisRepository,
                                                                          LigaRepository ligaRepository) {
        return new ConsultarEstadoCatalogoUseCase(paisRepository, ligaRepository);
    }

    // [QUÉ]: Bean de la jornada actual de una liga (CU-02): calcula la jornada del
    //        próximo partido por jugarse desde el calendario persistido.
    @Bean
    public ObtenerJornadaActualUseCase obtenerJornadaActualUseCase(PartidoRepository partidoRepository) {
        return new ObtenerJornadaActualUseCase(partidoRepository);
    }

    // [QUÉ]: Bean de CU-11 (gestionar catálogo de fuentes de extracción, asociación
    //        de URLs a la temporada vigente de la liga).
    @Bean
    public GestionarFuenteExtraccionUseCase gestionarFuenteExtraccionUseCase(
            FuenteExtraccionRepository fuenteRepository,
            DetalleFuenteExtraccionRepository detalleRepository,
            TemporadaRepository temporadaRepository) {
        return new GestionarFuenteExtraccionUseCase(fuenteRepository, detalleRepository, temporadaRepository);
    }

    // [QUÉ]: Bean de CU-14 (gestionar países de interés, prioridad de poblamiento).
    @Bean
    public GestionarPaisesInteresUseCase gestionarPaisesInteresUseCase(
            PaisInteresRepository paisInteresRepository,
            ProveedorPaises proveedorPaises) {
        return new GestionarPaisesInteresUseCase(paisInteresRepository, proveedorPaises);
    }

    // [QUÉ]: Bean de CU-15 (gestionar tareas programadas).
    @Bean
    public GestionarTareasProgramasUseCase gestionarTareasProgramasUseCase(TareaProgramadaRepository tareaProgramadaRepository,
                                                                           TareaLogRepository tareaLogRepository,
                                                                           LigaRepository ligaRepository,
                                                                           DetalleFuenteExtraccionRepository detalleRepository) {
        return new GestionarTareasProgramasUseCase(tareaProgramadaRepository, tareaLogRepository,
                ligaRepository, detalleRepository);
    }

    // [QUÉ]: Bean de CU-12 (registro de usuario autenticable con BCrypt).
    @Bean
    public RegistrarUsuarioUseCase registrarUsuarioUseCase(UsuarioRepository usuarioRepository,
                                                           PasswordHasher passwordHasher) {
        return new RegistrarUsuarioUseCase(usuarioRepository, passwordHasher);
    }

    // [QUÉ]: Bean de CU-13 (login y emisión de JWT).
    @Bean
    public AutenticarUsuarioUseCase autenticarUsuarioUseCase(UsuarioRepository usuarioRepository,
                                                             PasswordHasher passwordHasher,
                                                             TokenEmisor tokenEmisor) {
        return new AutenticarUsuarioUseCase(usuarioRepository, passwordHasher, tokenEmisor);
    }

    // [QUÉ]: Bean de CU-17 (HU-12 paso 1): poblar solo países, síncrono.
    @Bean
    public com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarPaisesUseCase sincronizarPaisesUseCase(
            ProveedorPaises proveedorPaises,
            PaisRepository paisRepository,
            CacheLecturas cacheLecturas) {
        return new com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarPaisesUseCase(
                proveedorPaises, paisRepository, cacheLecturas);
    }

    // [QUÉ]: Bean de CU-18 sync (HU-12 paso 2): poblar ligas por país.
    @Bean
    public com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarLigasPorPaisUseCase sincronizarLigasPorPaisUseCase(
            ProveedorLigasPorPais proveedorLigasPorPais,
            com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarEquiposLigaUseCase sincronizarEquiposLigaUseCase,
            PaisRepository paisRepository,
            LigaRepository ligaRepository,
            PaisInteresRepository paisInteresRepository) {
        return new com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarLigasPorPaisUseCase(
                proveedorLigasPorPais, sincronizarEquiposLigaUseCase,
                paisRepository, ligaRepository, paisInteresRepository);
    }

    // [QUÉ]: Bean de CU-18 async (HU-12 paso 2 async): wrapper 202 + polling.
    @Bean
    public com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarLigasPorPaisAsyncUseCase sincronizarLigasPorPaisAsyncUseCase(
            com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarLigasPorPaisUseCase delegado,
            TareaLogRepository tareaLogRepository) {
        return new com.tipsterbyte.tipsterbytefxv2.application.usecase.SincronizarLigasPorPaisAsyncUseCase(
                delegado, tareaLogRepository);
    }

    // [QUÉ]: Bean de CU-21 (HU-15): snapshot de cuotas próximas con volatilidad.
    @Bean
    public com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarCuotasProximasUseCase consultarCuotasProximasUseCase(
            PartidoRepository partidoRepository,
            com.tipsterbyte.tipsterbytefxv2.application.port.CuotaHistorialRepository cuotaHistorialRepository) {
        return new com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarCuotasProximasUseCase(
                partidoRepository, cuotaHistorialRepository);
    }

    // [QUÉ]: Bean de CU-22 (HU-15): historial cronológico de cuotas por partido.
    @Bean
    public com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarHistorialCuotasUseCase consultarHistorialCuotasUseCase(
            com.tipsterbyte.tipsterbytefxv2.application.port.CuotaHistorialRepository cuotaHistorialRepository) {
        return new com.tipsterbyte.tipsterbytefxv2.application.usecase.ConsultarHistorialCuotasUseCase(
                cuotaHistorialRepository);
    }

    // [QUÉ]: Bean de CU-23 (HU-16): CRUD de estrategias de pronóstico.
    @Bean
    public com.tipsterbyte.tipsterbytefxv2.application.usecase.GestionarEstrategiasUseCase gestionarEstrategiasUseCase(
            com.tipsterbyte.tipsterbytefxv2.application.port.EstrategiaRepository estrategiaRepository) {
        return new com.tipsterbyte.tipsterbytefxv2.application.usecase.GestionarEstrategiasUseCase(
                estrategiaRepository);
    }
}
