package pe.edu.vallegrande.flota.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.vallegrande.flota.domain.enums.EstadoOrden;
import pe.edu.vallegrande.flota.domain.enums.PrioridadOrden;
import pe.edu.vallegrande.flota.domain.enums.TipoMantenimiento;
import pe.edu.vallegrande.flota.domain.exception.DomainException;
import pe.edu.vallegrande.flota.domain.models.DetalleTaller;
import pe.edu.vallegrande.flota.domain.models.InformeAltaTecnica;
import pe.edu.vallegrande.flota.domain.models.OrdenMantenimiento;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class OrdenMantenimientoTest {

    @Test
    @DisplayName("Debe crear una orden de mantenimiento con estado inicial REGISTRADA")
    void debeCrearOrdenCorrectamente() {
        OrdenMantenimiento orden = OrdenMantenimiento.crear(
                "ORD-2026-TEST01",
                "rec-001",
                "EUA-123",
                "ent-policia-01",
                "44556677",
                "inc-009",
                TipoMantenimiento.CORRECTIVO_SINIESTRO,
                PrioridadOrden.ALTA,
                "Colisión frontal durante persecución",
                45000,
                "Requiere peritaje de aseguradora"
        );

        assertNotNull(orden);
        assertEquals("ORD-2026-TEST01", orden.getCodigoOrden());
        assertEquals("rec-001", orden.getRecursoId());
        assertEquals("EUA-123", orden.getRecursoPlaca());
        assertEquals(EstadoOrden.REGISTRADA, orden.getEstadoOrden());
        assertEquals(TipoMantenimiento.CORRECTIVO_SINIESTRO, orden.getTipoMantenimiento());
        assertNotNull(orden.getCreadoEn());
    }

    @Test
    @DisplayName("Debe fallar si el ID de recurso o el motivo están vacíos")
    void debeFallarValidacionInvariantes() {
        assertThrows(DomainException.class, () ->
                OrdenMantenimiento.crear("ORD-1", "", "ABC-123", "ent-1", "123", null,
                        TipoMantenimiento.PREVENTIVO, PrioridadOrden.MEDIA, "Falla", 1000, null)
        );

        assertThrows(DomainException.class, () ->
                OrdenMantenimiento.crear("ORD-1", "rec-1", "ABC-123", "ent-1", "123", null,
                        TipoMantenimiento.PREVENTIVO, PrioridadOrden.MEDIA, "", 1000, null)
        );
    }

    @Test
    @DisplayName("Debe transicionar correctamente: REGISTRADA -> EN_DIAGNOSTICO -> EN_TALLER -> CONTROL_CALIDAD -> FINALIZADA")
    void debeCompletarCicloDeVidaExitosamente() {
        OrdenMantenimiento orden = OrdenMantenimiento.crear(
                "ORD-2026-FLOW",
                "rec-amb-01",
                "AMB-999",
                "ent-hospital-01",
                "77889900",
                null,
                TipoMantenimiento.PREVENTIVO,
                PrioridadOrden.MEDIA,
                "Mantenimiento rutinario de 50,000 km",
                50000,
                null
        );

        // 1. Asignar taller
        DetalleTaller taller = DetalleTaller.builder()
                .nombreTaller("Taller Central Cañete")
                .rucTaller("20123456789")
                .mecanicoResponsable("Carlos Mendoza")
                .costoEstimado(new BigDecimal("1200.00"))
                .fechaEstimadaEntrega(LocalDate.now().plusDays(3))
                .diagnosticoInicial("Cambio de pastillas de freno y aceite")
                .build();
        orden.asignarTaller(taller);
        assertEquals(EstadoOrden.EN_DIAGNOSTICO, orden.getEstadoOrden());

        // 2. Iniciar reparación
        orden.iniciarReparacion();
        assertEquals(EstadoOrden.EN_TALLER, orden.getEstadoOrden());

        // 3. Control de calidad
        orden.pasarAControlCalidad();
        assertEquals(EstadoOrden.CONTROL_CALIDAD, orden.getEstadoOrden());

        // 4. Emitir alta operativa
        InformeAltaTecnica informe = InformeAltaTecnica.builder()
                .dictamenTecnico("APTO - Pruebas de frenado y motor satisfactorias")
                .pruebasRealizadas("Frenado en seco, suspensión y escaneo computarizado")
                .kilometrajeSalida(50010)
                .aprobadoPorDni("11223344")
                .observaciones("Listo para patrullaje")
                .build();
        orden.emitirAltaOperativa(informe);

        assertEquals(EstadoOrden.FINALIZADA, orden.getEstadoOrden());
        assertNotNull(orden.getInformeAltaTecnica());
    }

    @Test
    @DisplayName("No debe permitir iniciar reparación si no se ha asignado taller previamente")
    void noDebeIniciarReparacionSinTaller() {
        OrdenMantenimiento orden = OrdenMantenimiento.crear(
                "ORD-2026-ERR",
                "rec-01",
                "XYZ-123",
                "ent-01",
                "12345678",
                null,
                TipoMantenimiento.PREVENTIVO,
                PrioridadOrden.BAJA,
                "Avería leve",
                100,
                null
        );

        assertThrows(DomainException.class, orden::iniciarReparacion);
    }
}
