package kitnapper.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import kitnapper.model.LoanStatus;
import kitnapper.view.AllLoansScreen;
import kitnapper.view.LoanRow;
import kitnapper.view.MyLoansScreen;

/**
 * The loan screens' view contracts (§5.5) over one {@link LoansController}.
 * Screens declare their own nested {@code Actions}; this adapter is the single
 * controller-side implementation, so {@link AppController} stays free of
 * anonymous classes and Overdue joins the same adapter in WP4.
 */
public final class LoansActions implements MyLoansScreen.Actions,
        AllLoansScreen.Actions {

    private final LoansController loans;

    public LoansActions(LoansController loans) {
        this.loans = loans;
    }

    @Override
    public List<LoanRow> myLoans(LoanStatus tab) {
        return loans.myLoans(tab);
    }

    @Override
    public List<LoanRow> allLoans() {
        return loans.allLoans();
    }

    @Override
    public List<LocalDate> returnDates(LoanRow row) {
        return loans.returnDates(row);
    }

    @Override
    public double feeOn(LoanRow row, LocalDate date) {
        return loans.feeOn(row, date);
    }

    @Override
    public Optional<String> returnLoan(LoanRow row, LocalDate date) {
        return loans.returnLoan(row, date);
    }
}
