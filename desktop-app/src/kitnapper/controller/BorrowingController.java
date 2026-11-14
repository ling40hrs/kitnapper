package kitnapper.controller;

import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import kitnapper.model.Equipment;
import kitnapper.service.AppContext;

/**
 * Checkout and return actions through {@link AppContext#loans()}. Translates
 * service and persistence failures into user-facing messages so views never
 * catch exceptions or compute business rules themselves.
 */
public final class BorrowingController {

    private static final String SAVE_FAILED =
            "Could not save the change. Check the app folder and retry.";
    private static final String CHECKOUT_FAILED = "Could not complete checkout.";
    private static final String RETURN_FAILED = "Could not complete the return.";

    private final AppContext context;

    public BorrowingController(AppContext context) {
        this.context = context;
    }

    /** Today through today + 14 days, ascending; the UI defaults to +3. */
    public List<LocalDate> dueDateChoices() {
        LocalDate today = context.today();
        return IntStream.rangeClosed(0, 14)
                .mapToObj(today::plusDays)
                .toList();
    }

    public Optional<String> checkout(Equipment item, LocalDate dueDate) {
        if (item == null) {
            return Optional.of(CHECKOUT_FAILED);
        }
        try {
            context.loans().checkout(item.getId(), dueDate);
            return Optional.empty();
        } catch (UncheckedIOException saveFailure) {
            return Optional.of(SAVE_FAILED);
        } catch (RuntimeException failure) {
            return Optional.of(messageOr(failure, CHECKOUT_FAILED));
        }
    }

    public Optional<String> returnLoan(String loanId, LocalDate returnDate) {
        try {
            context.loans().returnLoan(loanId, returnDate);
            return Optional.empty();
        } catch (UncheckedIOException saveFailure) {
            return Optional.of(SAVE_FAILED);
        } catch (RuntimeException failure) {
            return Optional.of(messageOr(failure, RETURN_FAILED));
        }
    }

    private static String messageOr(RuntimeException failure, String fallback) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
