package kitnapper.model;

import java.time.LocalDate;

/**
 * Pluggable fee rule (PRD 3.2). LoanRecord depends only on this interface,
 * never on a concrete rule, so adding a rule changes no entity (Open/Closed,
 * NFR-06).
 */
@FunctionalInterface
public interface FeeStrategy {

    /**
     * @param loan the loan to price
     * @param asOf the date the fee is evaluated at; for a returned loan the
     *             implementing rule should freeze at the return date
     * @return the fee in PHP, never negative
     */
    double calculate(LoanRecord loan, LocalDate asOf);
}
