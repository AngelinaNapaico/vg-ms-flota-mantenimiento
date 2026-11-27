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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
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
                lenient().when(resourceClientPort.validateResourceBelongsToEntity(any(), any(), any()))
                                .thenReturn(Mono.empty());
                lenient().when(resourceClientPort.validateResourceBelongsToEntity(any(), any()))
                                .thenReturn(Mono.empty());
                lenient().when(auditLogRepositoryPort.recordAudit(any(), any(), any(), any(), any(), any(), any(),
                                any())).thenReturn(Mono.empty());

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
                when(repositoryPort.existsByResourceIdAndEntityIdAndOrderStatusNotIn(
                                eq("rec-001"), eq("ent-policia"), anyCollection()))
                                .thenReturn(Mono.just(false));

                when(resourceClientPort.disableResourceForMaintenance(eq("rec-001"), anyString()))
                                .thenReturn(Mono.empty());

                when(repositoryPort.save(any(MaintenanceOrder.class)))
                                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                StepVerifier.create(useCase.create(newOrder, "mock-token"))
                                .expectNextMatches(resp -> resp.getResourceId().equals("rec-001")
                                                && Boolean.TRUE.equals(resp.getActive())
                                                && resp.getOrderStatus() == OrderStatus.REGISTERED
                                                && resp.getOrderCode().startsWith("ORD-"))
                                .verifyComplete();

                verify(resourceClientPort, times(1)).disableResourceForMaintenance(eq("rec-001"), eq("mock-token"));
                verify(auditLogRepositoryPort, times(1)).recordAudit(
                                eq("CREATE"),
                                argThat(code -> code != null && code.startsWith("ORD-")),
                                eq("rec-001"),
                                eq("test-user"),
                                eq("ent-policia"),
                                eq("SUCCESS"),
                                argThat(id -> id != null && !id.isBlank()),
                                eq("NONE"));
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

                when(repositoryPort.findByIdAndEntityId("orden-mongo-001", "ent-policia"))
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
                verify(auditLogRepositoryPort, times(1)).recordAudit(
                                eq("TECHNICAL_DISCHARGE"),
                                eq("ORD-2026-TEST"),
                                eq("rec-001"),
                                eq("test-user"),
                                eq("ent-policia"),
                                eq("SUCCESS"),
                                argThat(id -> id != null && !id.isBlank()),
                                eq("NONE"));
        }

        @Test
        @DisplayName("Should reject order creation and persist failure audit when non-global operator targets another entity")
        void shouldRejectCreationForForeignEntity() {
                newOrder.setEntityId("ent-bomberos");

                StepVerifier.create(useCase.create(newOrder, "mock-token"))
                                .expectError(AccessDeniedException.class)
                                .verify();

                verify(repositoryPort, never()).save(any());
                verify(auditLogRepositoryPort, times(1)).recordAudit(
                                eq("CREATE"),
                                anyString(),
                                eq("rec-001"),
                                eq("test-user"),
                                eq("ent-policia"),
                                eq("FAILED"),
                                argThat(id -> id != null && !id.isBlank()),
                                eq("AccessDeniedException"));
        }

        @Test
        @DisplayName("Should reject order creation when non-global operator has no entity assigned")
        void shouldRejectCreationWithoutCallerEntity() {
                when(authClientPort.getCurrentEntityId()).thenReturn(Mono.empty());

                StepVerifier.create(useCase.create(newOrder, "mock-token"))
                                .expectError(AccessDeniedException.class)
                                .verify();

                verify(repositoryPort, never()).save(any());
                verify(auditLogRepositoryPort, times(1)).recordAudit(
                                eq("CREATE"),
                                anyString(),
                                eq("rec-001"),
                                eq("test-user"),
                                eq("ent-policia"),
                                eq("FAILED"),
                                argThat(id -> id != null && !id.isBlank()),
                                eq("AccessDeniedException"));
        }

        @Test
        @DisplayName("Should reject mutation when non-global operator lacks tenant claim without using fixed tenant fallback")
        void shouldRejectMutationWhenCallerLacksTenantClaim() {
                when(authClientPort.getCurrentEntityId()).thenReturn(Mono.empty());

                MaintenanceOrder updateDetails = MaintenanceOrder.builder()
                                .failureReason("Attempt without tenant claim")
                                .build();

                StepVerifier.create(useCase.update("order-001", updateDetails))
                                .expectError(AccessDeniedException.class)
                                .verify();

                verify(repositoryPort, never()).findById(anyString());
                verify(repositoryPort, never()).findByIdAndEntityId(anyString(), anyString());
                verify(auditLogRepositoryPort, times(1)).recordAudit(
                                eq("UPDATE"),
                                eq("order-001"),
                                eq("RESOURCE_UNAVAILABLE"),
                                eq("test-user"),
                                eq("N/A"),
                                eq("FAILED"),
                                argThat(id -> id != null && !id.isBlank()),
                                eq("AccessDeniedException"));
        }

        @Test
        @DisplayName("Should propagate error instead of suppressing when failure audit persistence fails")
        void shouldPropagateErrorWhenFailureAuditPersistenceFails() {
                when(repositoryPort.findByIdAndEntityId("missing-id", "ent-policia"))
                                .thenReturn(Mono.empty());
                when(auditLogRepositoryPort.recordAudit(
                                eq("START_REPAIR"), any(), any(), any(), any(), eq("FAILED"), any(), any()))
                                .thenReturn(Mono.error(
                                                new RuntimeException("Audit store down during failure logging")));

                StepVerifier.create(useCase.startRepair("missing-id"))
                                .expectErrorMatches(err -> err instanceof DomainException
                                                && "Failed to persist durable audit log for operation"
                                                                .equals(err.getMessage()))
                                .verify();
        }

        @Test
        @DisplayName("Should allow global admin to create order with explicit entity ID")
        void shouldAllowGlobalAdminWithExplicitEntityId() {
                when(authClientPort.hasGlobalAccess()).thenReturn(Mono.just(true));
                when(authClientPort.getCurrentEntityId()).thenReturn(Mono.empty());
                when(repositoryPort.existsByResourceIdAndEntityIdAndOrderStatusNotIn(
                                eq("rec-001"), eq("ent-policia"), anyCollection()))
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

        @Test
        @DisplayName("Should preserve resourceId and entityId when mutation fails after loading order")
        void shouldPreserveResourceAndTenantInFailureAudit() {
                MaintenanceOrder completedOrder = MaintenanceOrder.create(
                                "ORD-2026-DONE",
                                "rec-009",
                                "POL-900",
                                "ent-policia",
                                "12345678",
                                null,
                                MaintenanceType.PREVENTIVE,
                                OrderPriority.LOW,
                                "Routine check",
                                20000,
                                null);
                completedOrder.setId("order-done-1");
                completedOrder.setOrderStatus(OrderStatus.COMPLETED);

                when(repositoryPort.findByIdAndEntityId("order-done-1", "ent-policia"))
                                .thenReturn(Mono.just(completedOrder));

                StepVerifier.create(useCase.cancel("order-done-1", "Duplicate", ""))
                                .expectError(DomainException.class)
                                .verify();

                verify(auditLogRepositoryPort, times(1)).recordAudit(
                                eq("CANCEL"),
                                eq("ORD-2026-DONE"),
                                eq("rec-009"),
                                eq("test-user"),
                                eq("ent-policia"),
                                eq("FAILED"),
                                anyString(),
                                eq("DomainException"));
        }

        @Test
        @DisplayName("Should preserve caller tenant and order reference when target order is not found")
        void shouldPreserveCallerTenantWhenOrderNotFound() {
                when(repositoryPort.findByIdAndEntityId("missing-id", "ent-policia"))
                                .thenReturn(Mono.empty());

                StepVerifier.create(useCase.startRepair("missing-id"))
                                .expectError(DomainException.class)
                                .verify();

                verify(auditLogRepositoryPort, times(1)).recordAudit(
                                eq("START_REPAIR"),
                                eq("missing-id"),
                                eq("RESOURCE_UNAVAILABLE"),
                                eq("test-user"),
                                eq("ent-policia"),
                                eq("FAILED"),
                                anyString(),
                                eq("DomainException"));
        }

        @Test
        @DisplayName("Should fail operation instead of reporting success when durable audit persistence fails")
        void shouldFailOperationWhenMandatorySuccessAuditFails() {
                when(repositoryPort.existsByResourceIdAndEntityIdAndOrderStatusNotIn(
                                eq("rec-001"), eq("ent-policia"), anyCollection()))
                                .thenReturn(Mono.just(false));
                when(resourceClientPort.disableResourceForMaintenance(eq("rec-001"), anyString()))
                                .thenReturn(Mono.empty());
                when(repositoryPort.save(any(MaintenanceOrder.class)))
                                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
                when(auditLogRepositoryPort.recordAudit(
                                eq("CREATE"), any(), any(), any(), any(), eq("SUCCESS"), any(), any()))
                                .thenReturn(Mono.error(new RuntimeException("Audit store unavailable")));

                StepVerifier.create(useCase.create(newOrder, "mock-token"))
                                .expectError(DomainException.class)
                                .verify();
        }

        @Test
        @DisplayName("Should scope all read operations to the authenticated tenant")
        void shouldScopeAllReadsToAuthenticatedTenant() {
                newOrder.setId("order-own-1");
                newOrder.setOrderCode("ORD-2026-OWN001");

                when(repositoryPort.findByEntityId("ent-policia")).thenReturn(Flux.just(newOrder));
                when(repositoryPort.findByIdAndEntityId("order-own-1", "ent-policia")).thenReturn(Mono.just(newOrder));
                when(repositoryPort.findByIdAndEntityId("order-foreign-2", "ent-policia")).thenReturn(Mono.empty());
                when(repositoryPort.findByOrderCodeAndEntityId("ORD-2026-OWN001", "ent-policia"))
                                .thenReturn(Mono.just(newOrder));
                when(repositoryPort.findByResourceIdAndEntityId("rec-001", "ent-policia"))
                                .thenReturn(Flux.just(newOrder));
                when(repositoryPort.findByEntityIdAndOrderStatus("ent-policia", "REGISTERED"))
                                .thenReturn(Flux.just(newOrder));

                StepVerifier.create(useCase.findAll())
                                .expectNextMatches(order -> "order-own-1".equals(order.getId()))
                                .verifyComplete();

                StepVerifier.create(useCase.findById("order-own-1"))
                                .expectNextMatches(order -> "ent-policia".equals(order.getEntityId()))
                                .verifyComplete();

                StepVerifier.create(useCase.findById("order-foreign-2"))
                                .expectErrorMatches(err -> err instanceof DomainException de && de.isNotFound())
                                .verify();

                StepVerifier.create(useCase.findByCode("ORD-2026-OWN001"))
                                .expectNextMatches(order -> "ORD-2026-OWN001".equals(order.getOrderCode()))
                                .verifyComplete();

                StepVerifier.create(useCase.findByResource("rec-001"))
                                .expectNextCount(1)
                                .verifyComplete();

                StepVerifier.create(useCase.findByEntity("ent-bomberos"))
                                .verifyComplete();

                StepVerifier.create(useCase.findByStatus(OrderStatus.REGISTERED))
                                .expectNextCount(1)
                                .verifyComplete();

                verify(repositoryPort, never()).findAll();
                verify(repositoryPort, never()).findById(anyString());
                verify(repositoryPort, never()).findByOrderCode(anyString());
                verify(repositoryPort, never()).findByResourceId(anyString());
                verify(repositoryPort, never()).findByOrderStatus(anyString());
                verify(repositoryPort, never()).findByEntityId("ent-bomberos");
        }

        @Test
        @DisplayName("Should persist durable audit logs with correlationId across all lifecycle mutations")
        void shouldPersistDurableAuditAcrossAllLifecycleMutations() {
                MaintenanceOrder order = MaintenanceOrder.create(
                                "ORD-2026-LIFE",
                                "rec-055",
                                "POL-550",
                                "ent-policia",
                                "12345678",
                                null,
                                MaintenanceType.CORRECTIVE,
                                OrderPriority.HIGH,
                                "Initial brake defect",
                                41000,
                                null);
                order.setId("order-life-1");

                when(repositoryPort.findByIdAndEntityId("order-life-1", "ent-policia"))
                                .thenReturn(Mono.just(order));
                when(repositoryPort.save(any(MaintenanceOrder.class)))
                                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
                when(resourceClientPort.enableResourceToAvailable(eq("rec-055")))
                                .thenReturn(Mono.empty());

                MaintenanceOrder updateDetails = MaintenanceOrder.builder()
                                .failureReason("Updated brake defect details")
                                .reportedMileage(41050)
                                .priority(OrderPriority.CRITICAL)
                                .build();
                StepVerifier.create(useCase.update("order-life-1", updateDetails))
                                .expectNextCount(1)
                                .verifyComplete();

                WorkshopDetail workshop = WorkshopDetail.builder()
                                .workshopName("Central Workshop")
                                .workshopRuc("20123456789")
                                .estimatedCost(new BigDecimal("450.00"))
                                .build();
                StepVerifier.create(useCase.assignWorkshop("order-life-1", workshop))
                                .expectNextMatches(o -> o.getOrderStatus() == OrderStatus.IN_DIAGNOSIS)
                                .verifyComplete();

                StepVerifier.create(useCase.startRepair("order-life-1"))
                                .expectNextMatches(o -> o.getOrderStatus() == OrderStatus.IN_WORKSHOP)
                                .verifyComplete();

                StepVerifier.create(useCase.passToQualityControl("order-life-1"))
                                .expectNextMatches(o -> o.getOrderStatus() == OrderStatus.IN_QUALITY_CONTROL)
                                .verifyComplete();

                StepVerifier.create(useCase.cancel("order-life-1", "Unit decommissioned", "Audit test"))
                                .expectNextMatches(o -> o.getOrderStatus() == OrderStatus.CANCELLED)
                                .verifyComplete();

                for (String action : new String[] { "UPDATE", "ASSIGN_WORKSHOP", "START_REPAIR", "QUALITY_CONTROL",
                                "CANCEL" }) {
                        verify(auditLogRepositoryPort, times(1)).recordAudit(
                                        eq(action),
                                        eq("ORD-2026-LIFE"),
                                        eq("rec-055"),
                                        eq("test-user"),
                                        eq("ent-policia"),
                                        eq("SUCCESS"),
                                        argThat(id -> id != null && !id.isBlank()),
                                        eq("NONE"));
                }
        }
}
