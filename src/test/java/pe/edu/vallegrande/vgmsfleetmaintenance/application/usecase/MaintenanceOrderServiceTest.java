package pe.edu.vallegrande.vgmsfleetmaintenance.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.MaintenanceOrder;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.TechnicalDischargeReport;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.MaintenanceOrderRepositoryPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.ResourceClientPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.MaintenanceType;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderPriority;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceOrderServiceTest {

        @Mock
        private MaintenanceOrderRepositoryPort repositoryPort;

        @Mock
        private ResourceClientPort resourceClientPort;

        @InjectMocks
        private MaintenanceOrderService useCase;

        private MaintenanceOrder newOrder;

        @BeforeEach
        void setUp() {
                newOrder = MaintenanceOrder.create(
                                null,
                                "rec-001",
                                "POL-100",
                                "ent-policia",
                                "12345678",
                                null,
                                MaintenanceType.CORRECTIVE_ACCIDENT,
                                OrderPriority.HIGH,
                                "Choque lateral durante intervención",
                                35000,
                                null);
        }

        @Test
        @DisplayName("Should create order and trigger resource disabling in vg-ms-recursos via outbound port")
        void shouldCreateOrderSuccessfully() {
                when(repositoryPort.existsByResourceIdAndOrderStatusNotIn(eq("rec-001"), anyCollection()))
                                .thenReturn(Mono.just(false));

                when(resourceClientPort.disableResourceForMaintenance(eq("rec-001"), anyString()))
                                .thenReturn(Mono.empty());

                when(repositoryPort.save(any(MaintenanceOrder.class)))
                                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                StepVerifier.create(useCase.create(newOrder, "mock-token"))
                                .expectNextMatches(resp -> resp.getResourceId().equals("rec-001")
                                                && resp.getOrderStatus() == OrderStatus.REGISTERED
                                                && resp.getOrderCode().startsWith("ORD-"))
                                .verifyComplete();

                verify(resourceClientPort, times(1)).disableResourceForMaintenance(eq("rec-001"), eq("mock-token"));
        }

        @Test
        @DisplayName("Should issue technical discharge and restore resource to DISPONIBLE via outbound port")
        void shouldIssueTechnicalDischargeSuccessfully() {
                MaintenanceOrder existingOrder = MaintenanceOrder.create(
                                "ORD-2026-TEST",
                                "rec-001",
                                "POL-100",
                                "ent-policia",
                                "12345678",
                                null,
                                MaintenanceType.CORRECTIVE,
                                OrderPriority.HIGH,
                                "Falla de embrague",
                                35000,
                                null);
                existingOrder.setId("orden-mongo-001");
                existingOrder.setOrderStatus(OrderStatus.IN_WORKSHOP);

                when(repositoryPort.findById("orden-mongo-001"))
                                .thenReturn(Mono.just(existingOrder));

                when(resourceClientPort.enableResourceToAvailable(eq("rec-001"), anyString()))
                                .thenReturn(Mono.empty());

                when(repositoryPort.save(any(MaintenanceOrder.class)))
                                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                TechnicalDischargeReport dischargeReport = TechnicalDischargeReport.builder()
                                .technicalVerdict("Mantenimiento concluido con éxito, embrague calibrado")
                                .testsPerformed("Prueba de frenos y cambios en ruta")
                                .exitMileage(35015)
                                .dischargeDate(LocalDateTime.now())
                                .approvedByDni("87654321")
                                .remarks("Conforme")
                                .mediaUrl("https://res.cloudinary.com/vallegrande/image/upload/v1234/acta-prueba.pdf")
                                .build();

                StepVerifier.create(useCase.issueTechnicalDischarge("orden-mongo-001", dischargeReport, "mock-token"))
                                .expectNextMatches(resp -> resp.getOrderStatus() == OrderStatus.COMPLETED
                                                && resp.getTechnicalDischargeReport() != null
                                                && resp.getTechnicalDischargeReport().getExitMileage() == 35015)
                                .verifyComplete();

                verify(resourceClientPort, times(1)).enableResourceToAvailable(eq("rec-001"), eq("mock-token"));
        }
}
