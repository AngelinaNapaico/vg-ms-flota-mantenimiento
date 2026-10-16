package pe.edu.vallegrande.flota.domain.models;

import lombok.Getter;
import lombok.Setter;
import pe.edu.vallegrande.flota.domain.enums.EstadoOrden;
import pe.edu.vallegrande.flota.domain.enums.PrioridadOrden;
import pe.edu.vallegrande.flota.domain.enums.TipoMantenimiento;
import pe.edu.vallegrande.flota.domain.exception.DomainException;

import java.time.LocalDateTime;

/**
 * Entidad principal y Aggregate Root del dominio de Flota y Mantenimiento.
 * Encapsula las reglas de negocio e invariantes del ciclo de vida de reparación vehicular.
 * Estándar PR241 - Dominio puro desacoplado de la tecnología de persistencia.
 */
@Getter
@Setter
public class OrdenMantenimiento {

    private String id;
    private String codigoOrden;
    private String recursoId;
    private String recursoPlaca;
    private String entidadId;
    private String reportadoPorDni;
    private String incidenteId;
    private TipoMantenimiento tipoMantenimiento;
    private EstadoOrden estadoOrden;
    private PrioridadOrden prioridad;
    private String motivoAveria;
    private Integer kilometrajeReportado;
    private DetalleTaller detalleTaller;
    private InformeAltaTecnica informeAltaTecnica;
    private String observaciones;
    private String status;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public OrdenMantenimiento() {}

    /**
     * Factory method para crear una nueva orden de mantenimiento válida
     */
    public static OrdenMantenimiento crear(
            String codigoOrden,
            String recursoId,
            String recursoPlaca,
            String entidadId,
            String reportadoPorDni,
            String incidenteId,
            TipoMantenimiento tipoMantenimiento,
            PrioridadOrden prioridad,
            String motivoAveria,
            Integer kilometrajeReportado,
            String observaciones) {

        if (recursoId == null || recursoId.isBlank()) {
            throw new DomainException("El ID del recurso operativo es obligatorio");
        }
        if (entidadId == null || entidadId.isBlank()) {
            throw new DomainException("La entidad responsable es obligatoria");
        }
        if (motivoAveria == null || motivoAveria.isBlank()) {
            throw new DomainException("El motivo o descripción de la avería/siniestro es obligatorio");
        }

        OrdenMantenimiento orden = new OrdenMantenimiento();
        orden.codigoOrden = codigoOrden;
        orden.recursoId = recursoId;
        orden.recursoPlaca = recursoPlaca != null ? recursoPlaca.toUpperCase().trim() : "SIN-PLACA";
        orden.entidadId = entidadId;
        orden.reportadoPorDni = reportadoPorDni;
        orden.incidenteId = incidenteId;
        orden.tipoMantenimiento = tipoMantenimiento != null ? tipoMantenimiento : TipoMantenimiento.PREVENTIVO;
        orden.prioridad = prioridad != null ? prioridad : PrioridadOrden.MEDIA;
        orden.motivoAveria = motivoAveria;
        orden.kilometrajeReportado = kilometrajeReportado != null ? kilometrajeReportado : 0;
        orden.observaciones = observaciones;
        orden.estadoOrden = EstadoOrden.REGISTRADA;
        orden.status = "A";
        orden.creadoEn = LocalDateTime.now();
        orden.actualizadoEn = LocalDateTime.now();
        return orden;
    }

    /**
     * Reconstruir modelo de dominio desde la persistencia
     */
    public static OrdenMantenimiento reconstruir(
            String id,
            String codigoOrden,
            String recursoId,
            String recursoPlaca,
            String entidadId,
            String reportadoPorDni,
            String incidenteId,
            TipoMantenimiento tipoMantenimiento,
            EstadoOrden estadoOrden,
            PrioridadOrden prioridad,
            String motivoAveria,
            Integer kilometrajeReportado,
            DetalleTaller detalleTaller,
            InformeAltaTecnica informeAltaTecnica,
            String observaciones,
            String status,
            LocalDateTime creadoEn,
            LocalDateTime actualizadoEn) {

        OrdenMantenimiento orden = new OrdenMantenimiento();
        orden.id = id;
        orden.codigoOrden = codigoOrden;
        orden.recursoId = recursoId;
        orden.recursoPlaca = recursoPlaca;
        orden.entidadId = entidadId;
        orden.reportadoPorDni = reportadoPorDni;
        orden.incidenteId = incidenteId;
        orden.tipoMantenimiento = tipoMantenimiento;
        orden.estadoOrden = estadoOrden;
        orden.prioridad = prioridad;
        orden.motivoAveria = motivoAveria;
        orden.kilometrajeReportado = kilometrajeReportado;
        orden.detalleTaller = detalleTaller;
        orden.informeAltaTecnica = informeAltaTecnica;
        orden.observaciones = observaciones;
        orden.status = status != null ? status : "A";
        orden.creadoEn = creadoEn;
        orden.actualizadoEn = actualizadoEn;
        return orden;
    }

    public void asignarTaller(DetalleTaller taller) {
        validarModificable();
        if (taller == null || taller.getNombreTaller() == null || taller.getNombreTaller().isBlank()) {
            throw new DomainException("El nombre del taller mecánico es obligatorio");
        }
        this.detalleTaller = taller;
        this.estadoOrden = EstadoOrden.EN_DIAGNOSTICO;
        this.actualizadoEn = LocalDateTime.now();
    }

    public void iniciarReparacion() {
        validarModificable();
        if (this.detalleTaller == null) {
            throw new DomainException("No se puede iniciar la reparación sin asignar previamente un taller mecánico");
        }
        this.estadoOrden = EstadoOrden.EN_TALLER;
        this.actualizadoEn = LocalDateTime.now();
    }

    public void pasarAControlCalidad() {
        validarModificable();
        if (this.estadoOrden != EstadoOrden.EN_TALLER && this.estadoOrden != EstadoOrden.EN_DIAGNOSTICO) {
            throw new DomainException("La orden debe estar EN_TALLER o EN_DIAGNOSTICO para pasar a control de calidad");
        }
        this.estadoOrden = EstadoOrden.CONTROL_CALIDAD;
        this.actualizadoEn = LocalDateTime.now();
    }

    public void emitirAltaOperativa(InformeAltaTecnica informe) {
        validarModificable();
        if (informe == null || informe.getDictamenTecnico() == null || informe.getDictamenTecnico().isBlank()) {
            throw new DomainException("El dictamen técnico del alta operativa es obligatorio");
        }
        if (this.estadoOrden != EstadoOrden.CONTROL_CALIDAD && this.estadoOrden != EstadoOrden.EN_TALLER) {
            throw new DomainException("Para emitir alta técnica, el vehículo debe estar en taller o control de calidad");
        }
        this.informeAltaTecnica = informe;
        this.estadoOrden = EstadoOrden.FINALIZADA;
        this.actualizadoEn = LocalDateTime.now();
    }

    public void cancelar(String motivo) {
        if (this.estadoOrden == EstadoOrden.FINALIZADA) {
            throw new DomainException("No se puede cancelar una orden de mantenimiento que ya fue FINALIZADA");
        }
        if (this.estadoOrden == EstadoOrden.CANCELADA) {
            throw new DomainException("La orden de mantenimiento ya se encuentra cancelada");
        }
        this.observaciones = (this.observaciones != null ? this.observaciones + " | " : "") + "Cancelada: " + motivo;
        this.estadoOrden = EstadoOrden.CANCELADA;
        this.actualizadoEn = LocalDateTime.now();
    }

    private void validarModificable() {
        if (this.estadoOrden == EstadoOrden.FINALIZADA) {
            throw new DomainException("La orden de mantenimiento ya está FINALIZADA y no puede modificarse");
        }
        if (this.estadoOrden == EstadoOrden.CANCELADA) {
            throw new DomainException("La orden de mantenimiento está CANCELADA y no puede modificarse");
        }
    }
}
