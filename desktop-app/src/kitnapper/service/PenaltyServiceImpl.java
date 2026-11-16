package kitnapper.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import kitnapper.model.Borrower;
import kitnapper.model.LoanRecord;
import kitnapper.model.Manager;
import kitnapper.model.Penalty;
import kitnapper.model.PenaltyOutcome;
import kitnapper.model.PenaltyStatus;
import kitnapper.model.User;

/**
 * Settlement and reads over the live dataset (M4-D1). The model's
 * {@code markPaid}/{@code waive} stay the only status writers; this class
 * guards who may call them, translates model rejections into user-facing
 * strings, and saves through {@link DataStore} last. Reads reuse the
 * existing query shapes — the borrower scope comes from
 * {@link LoanQueries#myLoans(String)} ids, nothing new (plan §5.2).
 */
public final class PenaltyServiceImpl implements PenaltyService {

    private static final Comparator<Penalty> ASSESSED_DESC =
            Comparator.comparing(Penalty::getAssessedDate).reversed()
                    .thenComparing(Penalty::getPenaltyId);

    private final DataStore store;
    private final Supplier<User> session;
    private final Runnable onChange;
    private final LoanQueries queries;

    PenaltyServiceImpl(DataStore store, Supplier<LocalDate> today,
                       Supplier<User> session, Runnable onChange) {
        this.store = Objects.requireNonNull(store, "store");
        this.queries = new LoanQueries(store,
                Objects.requireNonNull(today, "today"));
        this.session = Objects.requireNonNull(session, "session");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    @Override
    public List<Penalty> allPenalties() {
        return store.penalties().stream()
                .sorted(ASSESSED_DESC)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public List<Penalty> penaltiesForLoan(String loanId) {
        if (loanId == null || loanId.isBlank()) {
            return new ArrayList<>();
        }
        return store.penalties().stream()
                .filter(penalty -> penalty.getLoanId().equals(loanId))
                .sorted(ASSESSED_DESC)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public List<Penalty> myPenalties() {
        User user = session.get();
        if (!(user instanceof Borrower)) {
            return new ArrayList<>();
        }
        Set<String> loanIds = queries.myLoans(user.getUserId()).stream()
                .map(LoanRecord::getLoanId)
                .collect(Collectors.toSet());
        return store.penalties().stream()
                .filter(penalty -> loanIds.contains(penalty.getLoanId()))
                .sorted(ASSESSED_DESC)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public double unpaidTotal() {
        return store.penalties().stream()
                .filter(penalty -> penalty.getStatus() == PenaltyStatus.UNPAID)
                .mapToDouble(Penalty::getAmount)
                .sum();
    }

    @Override
    public void settle(String penaltyId, PenaltyOutcome outcome, String reason) {
        Objects.requireNonNull(outcome, "outcome");
        User user = session.get();
        if (!(user instanceof Manager)) {
            throw new IllegalStateException(PenaltyMessages.MANAGER_ONLY);
        }
        Penalty penalty = findPenalty(penaltyId);
        if (penalty.getStatus() != PenaltyStatus.UNPAID) {
            throw new IllegalStateException(PenaltyMessages.ALREADY_SETTLED);
        }
        if (outcome == PenaltyOutcome.PAID) {
            penalty.markPaid();
        } else {
            waive(penalty, reason);
        }
        store.save();
        onChange.run();
    }

    /** Null and blank mean the same thing here: no reason given (FR-11). */
    private static void waive(Penalty penalty, String reason) {
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(PenaltyMessages.REASON_REQUIRED);
        }
        penalty.waive(trimmed);
    }

    private Penalty findPenalty(String penaltyId) {
        if (penaltyId == null || penaltyId.isBlank()) {
            throw new IllegalArgumentException(PenaltyMessages.PENALTY_MISSING);
        }
        return store.penalties().stream()
                .filter(penalty -> penalty.getPenaltyId().equals(penaltyId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        PenaltyMessages.PENALTY_MISSING));
    }
}
