package kitnapper.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The single fee rule shipped in v3: PHP 50.00 per overdue day with no
 * grace period (REQ-007). The rate is defined once, here, and is never
 * duplicated anywhere else (NFR-06).
 */
public final class LateFeeStrategy implements FeeStrategy {

    /** PHP per overdue day; the one place the rate exists (REQ-007). */
    public static final double RATE_PER_DAY = 50.00;

    @Override
    public double calculate(LoanRecord loan, LocalDate asOf) {
        Objects.requireNonNull(loan, "loan");
        Objects.requireNonNull(asOf, "asOf");
        return RATE_PER_DAY * loan.daysOverdue(asOf);
    }
}
