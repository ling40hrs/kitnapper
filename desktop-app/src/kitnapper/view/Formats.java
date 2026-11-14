package kitnapper.view;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The single place money and date formatting are defined (design.md §8):
 * money always {@code ₱1,234.00}, dates always {@code yyyy-MM-dd}.
 */
public final class Formats {

    private static final NumberFormat MONEY = NumberFormat.getNumberInstance(Locale.US);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    static {
        MONEY.setMinimumFractionDigits(2);
        MONEY.setMaximumFractionDigits(2);
    }

    private Formats() { }

    /** {@code ₱1,234.00} — the peso sign is explicit so no locale symbols it. */
    public static String money(double amount) {
        return "₱" + MONEY.format(amount);
    }

    /** {@code yyyy-MM-dd}, the only date format in the app. */
    public static String date(LocalDate value) {
        return DATE.format(value);
    }
}
