package kitnapper.service.persistence;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import kitnapper.model.Equipment;
import kitnapper.model.LoanRecord;
import kitnapper.model.Penalty;
import kitnapper.model.User;
import kitnapper.service.AppData;

/**
 * Maps the top-level document to the {@link AppData} aggregate and back
 * (D17): schemaVersion + savedAt + the four arrays. Referential breakage
 * (a loan whose borrower or equipment is missing, a penalty whose loan is
 * missing, two penalties on one loan) counts as corruption (D18), and the
 * aggregate is only returned once the whole document validated.
 */
public final class RootMapper {

    public static final int SCHEMA_VERSION = 1;

    private final UserMapper userMapper = new UserMapper();
    private final EquipmentMapper equipmentMapper = new EquipmentMapper();
    private final LoanMapper loanMapper = new LoanMapper();
    private final PenaltyMapper penaltyMapper = new PenaltyMapper();

    /** Serializes the whole aggregate; savedAt is the date of the save. */
    public Map<String, Object> toDocument(AppData data) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("schemaVersion", SCHEMA_VERSION);
        doc.put("savedAt", LocalDate.now().toString());
        doc.put("users", mapEach(data.users(), userMapper::toDocument));
        doc.put("equipment", mapEach(data.equipment(), equipmentMapper::toDocument));
        doc.put("loans", mapEach(data.loans(), loanMapper::toDocument));
        doc.put("penalties", mapEach(data.penalties(), penaltyMapper::toDocument));
        return doc;
    }

    /** Deserializes the document into a fresh aggregate, validating everything first. */
    public AppData fromDocument(Object document) {
        Map<String, Object> doc = Json.asObject(document, "root document");
        double version = Json.requireDouble(doc, "schemaVersion");
        if (version != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unknown schemaVersion: " + version);
        }
        Json.requireString(doc, "savedAt");

        List<User> users = mapArray(doc, "users", userMapper::fromDocument);
        List<Equipment> equipment = mapArray(doc, "equipment", equipmentMapper::fromDocument);
        List<LoanRecord> loans = mapArray(doc, "loans", loanMapper::fromDocument);
        List<Penalty> penalties = mapArray(doc, "penalties", penaltyMapper::fromDocument);
        validateReferences(users, equipment, loans, penalties);

        AppData data = new AppData();
        data.users().addAll(users);
        data.equipment().addAll(equipment);
        data.loans().addAll(loans);
        data.penalties().addAll(penalties);
        return data;
    }

    private static <T> List<T> mapArray(Map<String, Object> doc, String key,
                                        Function<Object, T> mapper) {
        List<T> items = new ArrayList<>();
        for (Object entry : Json.requireList(doc, key)) {
            items.add(mapper.apply(entry));
        }
        return items;
    }

    private static <T> List<Object> mapEach(List<T> items, Function<T, Map<String, Object>> mapper) {
        List<Object> documents = new ArrayList<>();
        for (T item : items) {
            documents.add(mapper.apply(item));
        }
        return documents;
    }

    private static void validateReferences(List<User> users, List<Equipment> equipment,
                                           List<LoanRecord> loans, List<Penalty> penalties) {
        Set<String> userIds = ids(users, User::getUserId);
        Set<String> equipmentIds = ids(equipment, Equipment::getId);
        Set<String> loanIds = ids(loans, LoanRecord::getLoanId);
        for (LoanRecord loan : loans) {
            if (!userIds.contains(loan.getBorrowerId())) {
                throw new IllegalArgumentException("Loan " + loan.getLoanId() + " references missing borrower");
            }
            if (!equipmentIds.contains(loan.getEquipmentId())) {
                throw new IllegalArgumentException("Loan " + loan.getLoanId() + " references missing equipment");
            }
        }
        Set<String> penalizedLoans = new HashSet<>();
        for (Penalty penalty : penalties) {
            if (!loanIds.contains(penalty.getLoanId())) {
                throw new IllegalArgumentException("Penalty " + penalty.getPenaltyId() + " references missing loan");
            }
            if (!penalizedLoans.add(penalty.getLoanId())) {
                throw new IllegalArgumentException("Two penalties on loan " + penalty.getLoanId());
            }
        }
    }

    private static <T> Set<String> ids(List<T> items, Function<T, String> key) {
        Set<String> set = new HashSet<>();
        for (T item : items) {
            set.add(key.apply(item));
        }
        return set;
    }
}
