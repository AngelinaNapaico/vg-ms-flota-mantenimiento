package pe.edu.vallegrande.vgmsfleetmaintenance.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.exception.DomainException;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.MaintenanceOrder;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.TechnicalDischargeReport;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.model.WorkshopDetail;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.MaintenanceType;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderPriority;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.vo.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MaintenanceOrderTest {

        @Test
        @DisplayName("Should create maintenance order with initial status REGISTERED")
        void shouldCreateOrderCorrectly() {
                MaintenanceOrder order = MaintenanceOrder.create(
                                "ORD-2026-TEST01",
                                "rec-001",
                                "EUA-123",
                                "ent-policia-01",
                                "44556677",
                                "inc-009",
                                MaintenanceType.CORRECTIVE_ACCIDENT,
                                OrderPriority.HIGH,
                                "Colisión frontal durante persecución",
                                45000,
                                "Requiere peritaje de aseguradora");

                assertNotNull(order);
                assertEquals("ORD-2026-TEST01", order.getOrderCode());
                assertEquals("rec-001", order.getResourceId());
                assertEquals("EUA-123", order.getResourcePlate());
                assertEquals(OrderStatus.REGISTERED, order.getOrderStatus());
                assertEquals(MaintenanceType.CORRECTIVE_ACCIDENT, order.getMaintenanceType());
                assertNotNull(order.getCreatedAt());
        }

        @Test
        @DisplayName("Should fail when resourceId or failureReason is empty")
        void shouldFailInvariantValidation() {
                assertThrows(DomainException.class,
                                () -> MaintenanceOrder.create("ORD-1", "", "ABC-123", "ent-1", "123", null,
                                                MaintenanceType.PREVENTIVE, OrderPriority.MEDIUM, "Falla", 1000, null));

                assertThrows(DomainException.class,
                                () -> MaintenanceOrder.create("ORD-1", "rec-1", "ABC-123", "ent-1", "123", null,
                                                MaintenanceType.PREVENTIVE, OrderPriority.MEDIUM, "", 1000, null));
        }

        @Test
        @DisplayName("Should transit lifecycle: REGISTERED -> IN_DIAGNOSIS -> IN_WORKSHOP -> IN_QUALITY_CONTROL -> COMPLETED")
        void shouldCompleteLifecycleSuccessfully() {
                MaintenanceOrder order = MaintenanceOrder.create(
                                "ORD-2026-FLOW",
                                "rec-amb-01",
                                "AMB-999",
                                "ent-hospital-01",
                                "77889900",
                                null,
                                MaintenanceType.PREVENTIVE,
                                OrderPriority.MEDIUM,
                                "Mantenimiento rutinario de 50,000 km",
                                50000,
                                null);

                // 1. Assign workshop
                WorkshopDetail workshop = WorkshopDetail.builder()
                                .workshopName("Taller Central Cañete")
                                .workshopRuc("20123456789")
                                .mechanicInCharge("Carlos Mendoza")
                                .estimatedCost(new BigDecimal("1200.00"))
                                .estimatedDeliveryDate(LocalDate.now().plusDays(3).atStartOfDay())
                                .initialDiagnosis("Cambio de pastillas de freno y aceite")
                                .build();
                order.assignWorkshop(workshop);
                assertEquals(OrderStatus.IN_DIAGNOSIS, order.getOrderStatus());

                // 2. Start repair
                order.startRepair();
                assertEquals(OrderStatus.IN_WORKSHOP, order.getOrderStatus());

                // 3. Quality control
                order.passToQualityControl();
                assertEquals(OrderStatus.IN_QUALITY_CONTROL, order.getOrderStatus());

                // 4. Issue technical discharge
                TechnicalDischargeReport report = TechnicalDischargeReport.builder()
                                .technicalVerdict("APTO - Pruebas de frenado y motor satisfactorias")
                                .testsPerformed("Frenado en seco, suspensión y escaneo computarizado")
                                .exitMileage(50010)
                                .approvedByDni("11223344")
                                .remarks("Listo para patrullaje")
                                .mediaUrl("https://res.cloudinary.com/vallegrande/image/upload/v1234/acta-prueba.pdf")
                                .build();
                order.issueTechnicalDischarge(report);

                assertEquals(OrderStatus.COMPLETED, order.getOrderStatus());
                assertNotNull(order.getTechnicalDischargeReport());
        }

        @Test
        @DisplayName("Should not start repair without workshop assigned")
        void shouldNotStartRepairWithoutWorkshop() {
                MaintenanceOrder order = MaintenanceOrder.create(
                                "ORD-2026-ERR",
                                "rec-01",
                                "XYZ-123",
                                "ent-01",
                                "12345678",
                                null,
                                MaintenanceType.PREVENTIVE,
                                OrderPriority.LOW,
                                "Avería leve",
                                100,
                                null);

                assertThrows(DomainException.class, order::startRepair);
        }

        @Test
        @DisplayName("Should update editable details successfully when order is active")
        void shouldUpdateDetailsSuccessfully() {
                MaintenanceOrder order = MaintenanceOrder.create(
                                "ORD-2026-UPD",
                                "rec-01",
                                "XYZ-123",
                                "ent-01",
                                "12345678",
                                null,
                                MaintenanceType.PREVENTIVE,
                                OrderPriority.LOW,
                                "Avería inicial",
                                100,
                                null);

                order.updateDetails("Falla corregida en frenos", 150, OrderPriority.CRITICAL, "Observación nueva",
                                "INC-001");

                assertEquals("Falla corregida en frenos", order.getFailureReason());
                assertEquals(150, order.getReportedMileage());
                assertEquals(OrderPriority.CRITICAL, order.getPriority());
                assertEquals("Observación nueva", order.getRemarks());
                assertEquals("INC-001", order.getIncidentId());
        }

        @Test
        @DisplayName("Should fail updating details if order is cancelled")
        void shouldFailUpdatingDetailsWhenCancelled() {
                MaintenanceOrder order = MaintenanceOrder.create(
                                "ORD-2026-UPD",
                                "rec-01",
                                "XYZ-123",
                                "ent-01",
                                "12345678",
                                null,
                                MaintenanceType.PREVENTIVE,
                                OrderPriority.LOW,
                                "Avería inicial",
                                100,
                                null);
                order.cancel("Error de registro");

                assertThrows(DomainException.class,
                                () -> order.updateDetails("Nuevo motivo", 200, OrderPriority.HIGH, null, null));
        }

        @Test
        @DisplayName("Should update workshop details without rewinding status")
        void shouldUpdateWorkshopDetailsWithoutRewindingStatus() {
                MaintenanceOrder order = MaintenanceOrder.create(
                                "ORD-2026-WK",
                                "rec-01",
                                "XYZ-123",
                                "ent-01",
                                "12345678",
                                null,
                                MaintenanceType.CORRECTIVE,
                                OrderPriority.MEDIUM,
                                "Falla de embrague",
                                500,
                                null);

                WorkshopDetail initialWorkshop = WorkshopDetail.builder()
                                .workshopName("Taller Original")
                                .mechanicInCharge("Mecánico Antiguo")
                                .build();
                order.assignWorkshop(initialWorkshop);
                order.startRepair(); // State is now IN_WORKSHOP

                assertEquals(OrderStatus.IN_WORKSHOP, order.getOrderStatus());

                WorkshopDetail updatedWorkshop = WorkshopDetail.builder()
                                .workshopName("Taller Original")
                                .mechanicInCharge("Mecánico Nuevo Carlos")
                                .contactPhone("999888777")
                                .build();

                order.updateDetails("Falla de embrague y caja", 500, OrderPriority.HIGH, "Ajuste", null,
                                updatedWorkshop);

                assertEquals(OrderStatus.IN_WORKSHOP, order.getOrderStatus()); // Status preserved!
                assertEquals("Mecánico Nuevo Carlos", order.getWorkshopDetail().getMechanicInCharge());
                assertEquals("999888777", order.getWorkshopDetail().getContactPhone());
                assertEquals("Falla de embrague", order.getFailureReason()); // Intake failure reason remains locked as
                                                                             // audit record!
                assertEquals(OrderPriority.MEDIUM, order.getPriority()); // Priority remains locked!
                assertEquals("Ajuste", order.getRemarks());
        }
}
