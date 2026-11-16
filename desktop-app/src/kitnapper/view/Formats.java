package kitnapper.view;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import kitnapper.model.EquipmentCategory;

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

    /** Title-case category word (§4: no all-caps styling anywhere). */
    public static String category(EquipmentCategory category) {
        return switch (category) {
            case CAMERA -> "Camera";
            case LENS -> "Lens";
            case LAPTOP -> "Laptop";
            case AUDIO -> "Audio";
            case TOOL -> "Tool";
            case OTHER -> "Other";
        };
    }

    /**
     * A counted noun with the right number: {@code 1 penalty}, {@code 0 penalties}.
     * Status lines use this so no screen prints "1 penalties" (design §8).
     */
    public static String count(long amount, String singular, String plural) {
        return (amount == 1 ? "1 " : amount + " ") + (amount == 1 ? singular : plural);
    }
}
