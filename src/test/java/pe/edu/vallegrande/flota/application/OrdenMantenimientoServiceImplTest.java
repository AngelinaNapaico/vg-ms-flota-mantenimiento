package pe.edu.vallegrande.flota.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.flota.application.services.impl.OrdenMantenimientoServiceImpl;
import pe.edu.vallegrande.flota.domain.enums.EstadoOrden;
import pe.edu.vallegrande.flota.domain.enums.PrioridadOrden;
import pe.edu.vallegrande.flota.domain.enums.TipoMantenimiento;
import pe.edu.vallegrande.flota.domain.models.OrdenMantenimiento;
import pe.edu.vallegrande.flota.infrastructure.client.external.RecursoOperativoClient;
import pe.edu.vallegrande.flota.infrastructure.document.OrdenMantenimientoDocument;
import pe.edu.vallegrande.flota.infrastructure.dto.request.AltaOperativaRequest;
import pe.edu.vallegrande.flota.infrastructure.dto.request.CreateOrdenMantenimientoRequest;
import pe.edu.vallegrande.flota.infrastructure.mapper.OrdenMantenimientoMapper;
import pe.edu.vallegrande.flota.infrastructure.repository.OrdenMantenimientoRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenMantenimientoServiceImplTest {

    @Mock
    private OrdenMantenimientoRepository repository;

    @Mock
    private RecursoOperativoClient recursoClient;

    @InjectMocks
    private OrdenMantenimientoServiceImpl service;

    private CreateOrdenMantenimientoRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = CreateOrdenMantenimientoRequest.builder()
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
        when(repository.existsByRecursoIdAndEstadoOrdenNotIn(eq("rec-001"), anyCollection()))
                .thenReturn(Mono.just(false));

        when(recursoClient.inhabilitarRecursoPorMantenimiento(eq("rec-001"), anyString()))
                .thenReturn(Mono.empty());

        when(repository.save(any(OrdenMantenimientoDocument.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.create(createRequest, "mock-token", "12345678"))
                .expectNextMatches(resp -> resp.getRecursoId().equals("rec-001")
                        && resp.getEstadoOrden() == EstadoOrden.REGISTRADA
                        && resp.getCodigoOrden().startsWith("ORD-"))
                .verifyComplete();

        verify(recursoClient, times(1)).inhabilitarRecursoPorMantenimiento(eq("rec-001"), eq("mock-token"));
    }

    @Test
    @DisplayName("Debe emitir alta técnica y disparar rehabilitación del recurso a DISPONIBLE")
    void debeEmitirAltaOperativaExitosamente() {
        OrdenMantenimiento ordenExistente = OrdenMantenimiento.crear(
                "ORD-2026-TEST",
                "rec-001",
                "POL-100",
                "ent-policia",
                "12345678",
                null,
                TipoMantenimiento.CORRECTIVO,
                PrioridadOrden.ALTA,
                "Falla de embrague",
                35000,
                null
        );
        ordenExistente.setId("orden-mongo-001");
        ordenExistente.setEstadoOrden(EstadoOrden.EN_TALLER);

        when(repository.findById("orden-mongo-001"))
                .thenReturn(Mono.just(OrdenMantenimientoMapper.toDocument(ordenExistente)));

        when(recursoClient.habilitarRecursoPorAltaOperativa(eq("rec-001"), anyString()))
                .thenReturn(Mono.empty());

        when(repository.save(any(OrdenMantenimientoDocument.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        AltaOperativaRequest altaRequest = AltaOperativaRequest.builder()
                .dictamenTecnico("Mantenimiento concluido con éxito, embrague calibrado")
                .pruebasRealizadas("Prueba de frenos y cambios en ruta")
                .kilometrajeSalida(35015)
                .costoFinal(new BigDecimal("950.00"))
                .build();

        StepVerifier.create(service.emitirAltaOperativa("orden-mongo-001", altaRequest, "mock-token", "87654321"))
                .expectNextMatches(resp -> resp.getEstadoOrden() == EstadoOrden.FINALIZADA
                        && resp.getInformeAltaTecnica() != null
                        && resp.getInformeAltaTecnica().getKilometrajeSalida() == 35015)
                .verifyComplete();

        verify(recursoClient, times(1)).habilitarRecursoPorAltaOperativa(eq("rec-001"), eq("mock-token"));
    }
}
