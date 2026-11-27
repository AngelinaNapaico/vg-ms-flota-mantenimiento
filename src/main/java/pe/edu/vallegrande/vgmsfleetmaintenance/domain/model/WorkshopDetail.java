package pe.edu.vallegrande.vgmsfleetmaintenance.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.vallegrande.vgmsfleetmaintenance.domain.exception.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Domain model for the assigned external mechanical workshop details.
 * Enforces exact decimal precision and monetary business bounds.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopDetail {

    public static final int MONETARY_SCALE = 2;
    public static final BigDecimal MIN_COST = BigDecimal.ZERO.setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
    public static final BigDecimal MAX_COST = new BigDecimal("99999999.99");

    private String workshopName;
    private String workshopRuc;
    private String mechanicInCharge;
    private String contactPhone;
    private String workshopAddress;
    private BigDecimal estimatedCost;
    private BigDecimal finalCost;
    private LocalDateTime estimatedDeliveryDate;
    private String initialDiagnosis;

    public void normalizeMonetaryAmounts() {
        this.estimatedCost = normalizeCost(this.estimatedCost, "Estimated cost");
        this.finalCost = normalizeCost(this.finalCost, "Final cost");
    }

    public static BigDecimal normalizeCost(BigDecimal amount, String fieldLabel) {
        if (amount == null) {
            return null;
        }
        if (amount.stripTrailingZeros().scale() > MONETARY_SCALE) {
            throw new DomainException(fieldLabel + " cannot have more than 2 decimal places");
        }
        if (amount.compareTo(MIN_COST) < 0 || amount.compareTo(MAX_COST) > 0) {
            throw new DomainException(fieldLabel + " must be between 0.00 and 99999999.99");
        }
        return amount.setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
    }
}
