package pe.edu.vallegrande.vgmsfleetmaintenance.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.exception.DomainException;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.MaintenanceOrder;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.TechnicalDischargeReport;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.WorkshopDetail;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.AuditLogRepositoryPort;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.port.out.AuthClientPort;
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

        @Mock
        private AuthClientPort authClientPort;

        @Mock
        private AuditLogRepositoryPort auditLogRepositoryPort;

        @InjectMocks
        private MaintenanceOrderService useCase;

        private MaintenanceOrder newOrder;

        @BeforeEach
        void setUp() {
                lenient().when(authClientPort.getCurrentUsername()).thenReturn(Mono.just("test-user"));
                lenient().when(authClientPort.getCurrentEntityId()).thenReturn(Mono.just("ent-policia"));
                lenient().when(authClientPort.hasGlobalAccess()).thenReturn(Mono.just(false));
                lenient().when(resourceClientPort.validateResourceBelongsToEntity(any(), any(), any())).thenReturn(Mono.empty());
                lenient().when(resourceClientPort.validateResourceBelongsToEntity(any(), any())).thenReturn(Mono.empty());
                lenient().when(auditLogRepositoryPort.recordAudit(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(Mono.empty());

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

        @Test
        @DisplayName("Should reject order creation when non-global operator targets another entity")
        void shouldRejectCreationForForeignEntity() {
                newOrder.setEntityId("ent-bomberos");

                StepVerifier.create(useCase.create(newOrder, "mock-token"))
                                .expectError(AccessDeniedException.class)
                                .verify();

                verify(repositoryPort, never()).save(any());
        }

        @Test
        @DisplayName("Should reject order creation when non-global operator has no entity assigned")
        void shouldRejectCreationWithoutCallerEntity() {
                when(authClientPort.getCurrentEntityId()).thenReturn(Mono.empty());

                StepVerifier.create(useCase.create(newOrder, "mock-token"))
                                .expectError(AccessDeniedException.class)
                                .verify();

                verify(repositoryPort, never()).save(any());
        }

        @Test
        @DisplayName("Should allow global admin to create order with explicit entity ID")
        void shouldAllowGlobalAdminWithExplicitEntityId() {
                when(authClientPort.hasGlobalAccess()).thenReturn(Mono.just(true));
                when(authClientPort.getCurrentEntityId()).thenReturn(Mono.empty());
                when(repositoryPort.existsByResourceIdAndOrderStatusNotIn(eq("rec-001"), anyCollection()))
                                .thenReturn(Mono.just(false));
                when(resourceClientPort.disableResourceForMaintenance(eq("rec-001"), anyString()))
                                .thenReturn(Mono.empty());
                when(repositoryPort.save(any(MaintenanceOrder.class)))
                                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                StepVerifier.create(useCase.create(newOrder, "mock-token"))
                                .expectNextMatches(saved -> "ent-policia".equals(saved.getEntityId()))
                                .verifyComplete();
        }

        @Test
        @DisplayName("Should require entity ID when global admin creates order without entity")
        void shouldRequireEntityIdForGlobalAdminWithoutEntity() {
                when(authClientPort.hasGlobalAccess()).thenReturn(Mono.just(true));
                when(authClientPort.getCurrentEntityId()).thenReturn(Mono.empty());
                newOrder.setEntityId(null);

                StepVerifier.create(useCase.create(newOrder, "mock-token"))
                                .expectError(DomainException.class)
                                .verify();

                verify(repositoryPort, never()).save(any());
        }
}
