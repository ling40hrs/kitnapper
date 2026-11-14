package kitnapper.service.persistence;

import java.util.LinkedHashMap;
import java.util.Map;

import kitnapper.model.Penalty;
import kitnapper.model.PenaltyType;

/**
 * Maps penalties to JSON and back. {@code waiveReason} is null unless the
 * penalty was waived, a WAIVED penalty requires a non-blank reason, and
 * an unknown status or type is corruption (D18).
 */
public final class PenaltyMapper {

    public Map<String, Object> toDocument(Penalty penalty) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("penaltyId", penalty.getPenaltyId());
        doc.put("loanId", penalty.getLoanId());
        doc.put("type", penalty.getType().name());
        doc.put("amount", penalty.getAmount());
        doc.put("status", penalty.getStatus().name());
        doc.put("assessedDate", penalty.getAssessedDate().toString());
        doc.put("waiveReason", penalty.getWaiveReason().orElse(null));
        return doc;
    }

    public Penalty fromDocument(Object value) {
        Map<String, Object> doc = Json.asObject(value, "penalty");
        Penalty penalty = new Penalty(
                Json.requireString(doc, "penaltyId"),
                Json.requireString(doc, "loanId"),
                parseType(Json.requireString(doc, "type")),
                Json.requireDouble(doc, "amount"),
                Json.requireDate(doc, "assessedDate"));
        restoreStatus(penalty, doc);
        return penalty;
    }

    /** Replays the settlement transition; a WAIVED entry must carry its reason. */
    private static void restoreStatus(Penalty penalty, Map<String, Object> doc) {
        String status = Json.requireString(doc, "status");
        switch (status) {
            case "UNPAID" -> requireNoReason(doc);
            case "PAID" -> {
                requireNoReason(doc);
                penalty.markPaid();
            }
            case "WAIVED" -> penalty.waive(Json.requireString(doc, "waiveReason"));
            default -> throw new IllegalArgumentException("Unknown penalty status: " + status);
        }
    }

    private static void requireNoReason(Map<String, Object> doc) {
        if (Json.optString(doc, "waiveReason") != null) {
            throw new IllegalArgumentException("Unsettled penalty carries a waiveReason");
        }
    }

    private static PenaltyType parseType(String raw) {
        try {
            return PenaltyType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown penalty type: " + raw);
        }
    }
}
