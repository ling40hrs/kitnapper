package kitnapper.service;

import java.time.LocalDate;

/** User-facing borrowing messages, kept out of the service algorithms. */
final class LoanMessages {

    static final int WINDOW_DAYS = 14;

    static final String BORROWER_ONLY = "Only borrowers can check out gear.";
    static final String ITEM_MISSING = "That item is no longer in the catalog.";
    static final String ITEM_UNAVAILABLE = "This item is not available to borrow.";
    static final String LOAN_CAP_REACHED =
            "You already have 3 active loans. Return one first.";
    static final String MANAGER_ONLY = "Only a manager can log returns.";
    static final String LOAN_MISSING = "That loan is no longer in the records.";
    static final String ALREADY_RETURNED = "This loan has already been returned.";
    static final String RETURN_REQUIRED = "Return date is required.";
    static final String RETURN_FUTURE = "Return date cannot be in the future.";
    static final String RETURN_BEFORE_BORROW =
            "Return date cannot be before the borrow date.";
    static final String EQUIPMENT_MISSING = "The linked equipment record is missing.";
    static final String EQUIPMENT_DECOMMISSIONED = "The linked item is decommissioned.";

    private LoanMessages() {
    }

    static String window(LocalDate borrowDate) {
        return "Due date must be between " + borrowDate + " and "
                + borrowDate.plusDays(WINDOW_DAYS) + ".";
    }
}
