package kitnapper.service.persistence;

import java.time.LocalDate;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import kitnapper.model.LoanRecord;

/**
 * Maps loan records to JSON and back. {@code returnDate} is null while
 * the loan is outstanding; {@code loanStatus} persists only ACTIVE and
 * RETURNED — a persisted OVERDUE normalizes to ACTIVE because the
 * overdue state is derived at read time (D1, D18).
 */
public final class LoanMapper {

    public Map<String, Object> toDocument(LoanRecord loan) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("loanId", loan.getLoanId());
        doc.put("borrowerId", loan.getBorrowerId());
        doc.put("equipmentId", loan.getEquipmentId());
        doc.put("borrowDate", loan.getBorrowDate().toString());
        doc.put("dueDate", loan.getDueDate().toString());
        doc.put("returnDate", loan.getReturnDate().map(LocalDate::toString).orElse(null));
        doc.put("loanStatus", loan.getLoanStatus().name());
        return doc;
    }

    public LoanRecord fromDocument(Object value) {
        Map<String, Object> doc = Json.asObject(value, "loan");
        String status = Json.requireString(doc, "loanStatus");
        if ("OVERDUE".equals(status)) {
            status = "ACTIVE";
        }
        if (!"ACTIVE".equals(status) && !"RETURNED".equals(status)) {
            throw new IllegalArgumentException("Unknown loanStatus: " + status);
        }
        LocalDate returnDate = Json.optDate(doc, "returnDate");
        if (!"RETURNED".equals(status) && returnDate != null) {
            throw new IllegalArgumentException("Outstanding loan carries a returnDate");
        }
        LoanRecord loan = LoanRecord.checkout(
                Json.requireString(doc, "loanId"),
                Json.requireString(doc, "borrowerId"),
                Json.requireString(doc, "equipmentId"),
                Json.requireDate(doc, "borrowDate"),
                Json.requireDate(doc, "dueDate"));
        if ("RETURNED".equals(status)) {
            loan.completeReturn(returnDate);
        }
        return loan;
    }
}
