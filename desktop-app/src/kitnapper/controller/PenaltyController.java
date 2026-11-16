package kitnapper.controller;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import kitnapper.model.PenaltyOutcome;
import kitnapper.service.AppContext;
import kitnapper.view.PenaltiesScreen;
import kitnapper.view.PenaltyRow;

/**
 * Backs the Penalties screen (REQ-011): rows come from {@link LoanRows},
 * settlement from {@link kitnapper.service.PenaltyService}, and failures
 * are translated into user-facing messages so the screen never catches.
 */
public final class PenaltyController implements PenaltiesScreen.Actions {

    private static final String SAVE_FAILED =
            "Could not save the change. Check the app folder and retry.";
    private static final String SETTLE_FAILED = "Could not settle the penalty.";

    private final AppContext context;
    private final LoanRows rows;
    private Runnable onChanged;

    public PenaltyController(AppContext context) {
        this.context = context;
        this.rows = new LoanRows(context);
        context.addChangeListener(this::notifyChanged);
    }

    /** Every penalty as view rows, newest assessed first. */
    @Override
    public List<PenaltyRow> penalties() {
        return rows.toPenaltyRows(context.penalties().allPenalties());
    }

    /** Settles the penalty; empty means success, otherwise the message. */
    @Override
    public Optional<String> settle(PenaltyRow row, PenaltyOutcome outcome,
                                   String reason) {
        if (row == null) {
            return Optional.of(SETTLE_FAILED);
        }
        try {
            context.penalties().settle(row.penaltyId(), outcome, reason);
            return Optional.empty();
        } catch (UncheckedIOException saveFailure) {
            return Optional.of(SAVE_FAILED);
        } catch (RuntimeException failure) {
            return Optional.of(messageOr(failure, SETTLE_FAILED));
        }
    }

    /** The screen reload hook, fired after every app-level change. */
    public void setOnChanged(Runnable listener) {
        onChanged = listener;
    }

    private void notifyChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }

    private static String messageOr(RuntimeException failure, String fallback) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
