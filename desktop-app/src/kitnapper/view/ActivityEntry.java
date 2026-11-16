package kitnapper.view;

import java.time.LocalDate;

/**
 * One derived dashboard event (D28): a loan's checkout or its return.
 * Built by a controller from loan records — never persisted, never
 * computed by the view. {@code fee} is the frozen late fee on a return
 * and zero on a checkout.
 */
public record ActivityEntry(LocalDate date, String loanId, String itemName,
                            String borrowerName, boolean checkout,
                            double fee) {
}
