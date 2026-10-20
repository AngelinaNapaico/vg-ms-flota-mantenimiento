package pe.edu.vallegrande.flota.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.vallegrande.flota.domain.enums.PrioridadOrden;
import pe.edu.vallegrande.flota.domain.enums.TipoMantenimiento;
import pe.edu.vallegrande.flota.domain.exception.DomainException;
import pe.edu.vallegrande.flota.domain.models.OrdenMantenimiento;

import static org.junit.jupiter.api.Assertions.*;

class OrdenMantenimientoParametrizedTest {

    @ParameterizedTest
    @EnumSource(TipoMantenimiento.class)
    @DisplayName("Debe crear órdenes con todos los tipos de mantenimiento soportados")
    void debeCrearOrdenConTodosLosTiposDeMantenimiento(TipoMantenimiento tipo) {
        OrdenMantenimiento orden = OrdenMantenimiento.crear(
                "ORD-2026-PARAM",
                "rec-01",
                "EUA-123",
                "ent-01",
                "12345678",
                null,
                tipo,
                PrioridadOrden.MEDIA,
                "Falla general de prueba",
                10000,
                null
        );

        assertNotNull(orden);
        assertEquals(tipo, orden.getTipoMantenimiento());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Debe fallar si el motivo de la avería está en blanco o con espacios")
    void debeFallarConMotivoInvalido(String motivoInvalido) {
        assertThrows(DomainException.class, () ->
                OrdenMantenimiento.crear(
                        "ORD-2026-FAIL",
                        "rec-01",
                        "EUA-123",
                        "ent-01",
                        "12345678",
                        null,
                        TipoMantenimiento.PREVENTIVO,
                        PrioridadOrden.BAJA,
                        motivoInvalido,
                        500,
                        null
                )
        );
    }
}
