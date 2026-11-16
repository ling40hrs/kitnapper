package kitnapper.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import kitnapper.model.LoanStatus;
import kitnapper.view.AllLoansScreen;
import kitnapper.view.LoanRow;
import kitnapper.view.MyLoansScreen;
import kitnapper.view.OverdueScreen;

/**
 * The loan screens' view contracts (§5.5) over one {@link LoansController}.
 * Screens declare their own nested {@code Actions}; this adapter is the single
 * controller-side implementation, so {@link AppController} stays free of
 * anonymous classes. All Loans and Overdue share one shape because they share
 * one table, preview and return flow (M4-D30).
 */
public final class LoansActions implements MyLoansScreen.Actions,
        AllLoansScreen.Actions, OverdueScreen.Actions {

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
    public List<LoanRow> overdueLoans() {
        return loans.overdueLoans();
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
