package vg_ms.flota.orden.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import vg_ms.flota.orden.application.port.RecursoOperativoPort;
import vg_ms.flota.orden.application.OrdenMantenimientoServiceImpl;
import vg_ms.flota.orden.domain.aggregate.OrdenMantenimiento;
import vg_ms.flota.orden.domain.repository.OrdenMantenimientoRepository;
import vg_ms.flota.orden.domain.vo.*;
import vg_ms.flota.orden.interfaces.dto.AltaOperativaRequest;
import vg_ms.flota.orden.interfaces.dto.CrearOrdenRequest;
import vg_ms.flota.shared.exception.DomainException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenMantenimientoServiceImplTest {

    @Mock
    private OrdenMantenimientoRepository ordenRepository;

    @Mock
    private RecursoOperativoPort recursoOperativoPort;

    @InjectMocks
    private OrdenMantenimientoServiceImpl appService;

    private CrearOrdenRequest crearRequest;

    @BeforeEach
    void setUp() {
        crearRequest = CrearOrdenRequest.builder()
                .recursoId("rec-001")
                .recursoPlaca("POL-100")
                .entidadId("ent-policia")
                .tipoMantenimiento(TipoMantenimiento.CORRECTIVO_SINIESTRO)
                .prioridad(PrioridadOrden.ALTA)
                .motivoAveria("Choque lateral durante intervención")
                .kilometrajeReportado(35000)
                .build();
    }

    @Test
    @DisplayName("Debe crear orden y disparar inhabilitación del recurso en vg-ms-recursos")
    void debeCrearOrdenExitosamente() {
        when(ordenRepository.existsByRecursoIdAndEstadoOrdenNotIn(eq("rec-001"), anyList()))
                .thenReturn(Mono.just(false));

        when(recursoOperativoPort.inhabilitarRecursoPorMantenimiento(eq("rec-001"), anyString()))
                .thenReturn(Mono.empty());

        when(ordenRepository.save(any(OrdenMantenimiento.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(appService.crearOrden(crearRequest, "mock-token", "12345678"))
                .expectNextMatches(resp -> {
                    return resp.getRecursoId().equals("rec-001")
                            && resp.getEstadoOrden() == EstadoOrden.REGISTRADA
                            && resp.getCodigoOrden().startsWith("ORD-");
                })
                .verifyComplete();

        verify(recursoOperativoPort, times(1)).inhabilitarRecursoPorMantenimiento(eq("rec-001"), anyString());
        verify(ordenRepository, times(1)).save(any(OrdenMantenimiento.class));
    }

    @Test
    @DisplayName("Debe rechazar la creación si ya existe una orden activa para ese recurso")
    void debeRechazarSiYaExisteOrdenActiva() {
        when(ordenRepository.existsByRecursoIdAndEstadoOrdenNotIn(eq("rec-001"), anyList()))
                .thenReturn(Mono.just(true));

        StepVerifier.create(appService.crearOrden(crearRequest, "mock-token", "12345678"))
                .expectError(DomainException.class)
                .verify();

        verify(recursoOperativoPort, never()).inhabilitarRecursoPorMantenimiento(anyString(), anyString());
        verify(ordenRepository, never()).save(any(OrdenMantenimiento.class));
    }

    @Test
    @DisplayName("Debe emitir alta técnica y disparar retorno a DISPONIBLE en vg-ms-recursos")
    void debeEmitirAltaTecnicaYRetornarDisponible() {
        OrdenMantenimiento ordenExistente = OrdenMantenimiento.crear(
                "ORD-2026-ALT",
                "rec-001",
                "POL-100",
                "ent-policia",
                "12345678",
                null,
                TipoMantenimiento.PREVENTIVO,
                PrioridadOrden.MEDIA,
                "Falla",
                100,
                null
        );
        ordenExistente.asignarTaller(DetalleTaller.builder().nombreTaller("Taller San Vicente").build());
        ordenExistente.iniciarReparacion();
        ordenExistente.pasarAControlCalidad();

        when(ordenRepository.findById("ord-id-1")).thenReturn(Mono.just(ordenExistente));
        when(recursoOperativoPort.habilitarRecursoPorAltaOperativa(eq("rec-001"), anyString()))
                .thenReturn(Mono.empty());
        when(ordenRepository.save(any(OrdenMantenimiento.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        AltaOperativaRequest request = AltaOperativaRequest.builder()
                .dictamenTecnico("Mantenimiento concluido con éxito")
                .pruebasRealizadas("Prueba de frenos y alineación")
                .kilometrajeSalida(105)
                .build();

        StepVerifier.create(appService.emitirAltaOperativa("ord-id-1", request, "mock-token", "99887766"))
                .expectNextMatches(resp -> resp.getEstadoOrden() == EstadoOrden.FINALIZADA)
                .verifyComplete();

        verify(recursoOperativoPort, times(1)).habilitarRecursoPorAltaOperativa(eq("rec-001"), anyString());
    }
}
