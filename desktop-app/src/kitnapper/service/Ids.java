package kitnapper.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import kitnapper.model.Equipment;
import kitnapper.model.LoanRecord;
import kitnapper.model.Penalty;

/**
 * Sequential, system-generated identifiers (D22): the next numeric suffix
 * after the entity's prefix, zero-padded to three digits. Malformed
 * suffixes are ignored so a hand-edited save file cannot break generation.
 */
public final class Ids {

    private Ids() {
    }

    /** Next loan ID after the highest existing {@code L-NNN}. */
    public static String nextLoanId(List<LoanRecord> loans) {
        Objects.requireNonNull(loans, "loans");
        return next(loans.stream().map(LoanRecord::getLoanId).toList(), "L-");
    }

    /** Next penalty ID after the highest existing {@code p-NNN}. */
    public static String nextPenaltyId(List<Penalty> penalties) {
        Objects.requireNonNull(penalties, "penalties");
        return next(penalties.stream().map(Penalty::getPenaltyId).toList(), "p-");
    }

    /** Next equipment ID after the highest existing {@code e-NNN} (M5). */
    public static String nextEquipmentId(List<Equipment> equipment) {
        Objects.requireNonNull(equipment, "equipment");
        return next(equipment.stream().map(Equipment::getId).toList(), "e-");
    }

    private static String next(List<String> ids, String prefix) {
        int highest = 0;
        for (String id : ids) {
            if (id == null || !id.startsWith(prefix)) {
                continue;
            }
            try {
                highest = Math.max(highest, Integer.parseInt(id.substring(prefix.length())));
            } catch (NumberFormatException ignored) {
                // Keep scanning; a malformed suffix never wins the max.
            }
        }
        return prefix + String.format(Locale.ROOT, "%03d", highest + 1);
    }
}
