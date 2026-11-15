package kitnapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import kitnapper.model.Equipment;
import kitnapper.model.EquipmentStatus;
import kitnapper.model.LoanRecord;
import kitnapper.model.Penalty;
import kitnapper.model.PenaltyStatus;
import kitnapper.model.PenaltyType;
import kitnapper.service.DataStore;
import kitnapper.service.DataStoreImpl;
import kitnapper.service.Ids;
import kitnapper.service.persistence.SeedData;

/**
 * Invariant and persistence sections of the data probe (plan §6.4, §9.4):
 * invariants INV-1/2/4/5 plus the entity guards, JSON round-trip, and the
 * corrupt-save fallback (D18). Runs against pure entities and a private
 * scratch directory; exits non-zero through the caller's tally.
 */
final class DataInvariantChecks {

    private DataInvariantChecks() {
    }

    static void run(LocalDate t) throws IOException {
        Path dir = Files.createTempDirectory("kitnapper-invariant-probe");
        try {
            invariants(t);
            roundTrip(dir, t);
        } finally {
            ProbeSupport.deleteTree(dir);
        }
    }

    private static void invariants(LocalDate t) {
        kitnapper.service.AppData data = SeedData.create(t);
        Equipment loaned = data.equipment().get(0);
        Equipment free = data.equipment().get(1);
        ProbeSupport.check("inv1-gate", ProbeSupport.throwsISE(() -> loaned.markLoaned())
            && free.getStatus() == EquipmentStatus.AVAILABLE);
        free.markLoaned();
        free.markAvailable();
        ProbeSupport.check("inv1-transition",
            free.getStatus() == EquipmentStatus.AVAILABLE);
        LoanRecord loan = LoanRecord.checkout("L-900", "u-student1", "e-009", t, t.plusDays(3));
        loan.completeReturn(t);
        ProbeSupport.check("inv5-double-return",
            ProbeSupport.throwsISE(() -> loan.completeReturn(t.plusDays(1))));
        ProbeSupport.check("inv2-window",
            ProbeSupport.throwsIAE(() -> LoanRecord.checkout("L-901", "u-b", "e-009",
                t, t.minusDays(1)))
            && ProbeSupport.throwsIAE(() -> LoanRecord.checkout("L-902", "u-b", "e-009",
                t, t.plusDays(15)))
            && LoanRecord.checkout("L-903", "u-b", "e-009", t, t.plusDays(14)) != null);
        ProbeSupport.check("inv4-amount",
            ProbeSupport.throwsIAE(() -> new Penalty("p-900", "L-900",
                PenaltyType.LATE_FEE, -1, t))
            && ProbeSupport.throwsIAE(() -> new Penalty("p-901", "L-900",
                PenaltyType.LATE_FEE, Double.NaN, t))
            && new Penalty("p-902", "L-900", PenaltyType.LATE_FEE, 0, t) != null);
        free.decommission();
        ProbeSupport.check("decommission-terminal",
            free.getStatus() == EquipmentStatus.DECOMMISSIONED
            && ProbeSupport.throwsISE(() -> free.markLoaned())
            && ProbeSupport.throwsISE(() -> free.decommission()));
        Equipment spare = data.equipment().get(3);
        spare.markUnderMaintenance();
        boolean repaired = spare.getStatus() == EquipmentStatus.UNDER_MAINTENANCE;
        spare.markAvailable();
        ProbeSupport.check("transitions-maintenance", repaired
            && ProbeSupport.throwsISE(() -> free.markUnderMaintenance())
            && spare.getStatus() == EquipmentStatus.AVAILABLE);
        ProbeSupport.check("entity-validation",
            ProbeSupport.throwsIAE(() -> data.equipment().get(2).updateDetails("  ",
                data.equipment().get(2).getCategory(), "SN-X", 1))
            && ProbeSupport.throwsIAE(() -> data.equipment().get(2).updateDetails("Name",
                data.equipment().get(2).getCategory(), "SN-X", -5)));
        ProbeSupport.check("photo-guard",
            ProbeSupport.throwsIAE(() -> data.equipment().get(2).setPhotoFile("../x.jpg"))
            && ProbeSupport.throwsIAE(() -> data.equipment().get(2).setPhotoFile("a/b.jpg")));
        ProbeSupport.check("polymorphic-fee",
            loan.accruedFee(t, (l, asOf) -> 7.0) == 7.0
            && loan.accruedFee(t, new kitnapper.model.LateFeeStrategy()) == 0.0);
        LoanRecord odd = LoanRecord.checkout("L-xyz", "u-b", "e-009", t, t.plusDays(1));
        ProbeSupport.check("ids-generation",
            Ids.nextLoanId(List.of(loan, odd)).equals("L-901")
            && Ids.nextPenaltyId(List.of()).equals("p-001"));
    }

    private static void roundTrip(Path dir, LocalDate t) throws IOException {
        DataStore store = new DataStoreImpl(dir);
        store.load();
        store.save();
        DataStore reloaded = new DataStoreImpl(dir);
        reloaded.load();
        ProbeSupport.check("roundtrip-counts", reloaded.users().size() == 3
            && reloaded.equipment().size() == 10 && reloaded.loans().size() == 3
            && reloaded.penalties().isEmpty());
        ProbeSupport.check("roundtrip-shape",
            ProbeSupport.loan(reloaded, "L-003").getDueDate().equals(t.minusDays(4))
            && ProbeSupport.loanedItem(reloaded, "e-001"));
        store.penalties().add(new Penalty("p-001", "L-003", PenaltyType.LATE_FEE, 200, t));
        store.save();
        DataStore withPenalty = new DataStoreImpl(dir);
        withPenalty.load();
        ProbeSupport.check("roundtrip-penalty", withPenalty.penalties().size() == 1
            && withPenalty.penalties().get(0).getAmount() == 200
            && withPenalty.penalties().get(0).getType() == PenaltyType.LATE_FEE
            && withPenalty.penalties().get(0).getStatus() == PenaltyStatus.UNPAID);
        Files.writeString(dir.resolve(DataStoreImpl.SAVE_FILE), "not json {{{",
                StandardCharsets.UTF_8);
        DataStore corrupt = new DataStoreImpl(dir);
        corrupt.load();
        ProbeSupport.check("corrupt-bytes-fallback", corrupt.users().size() == 3
            && corrupt.equipment().size() == 10 && corrupt.penalties().isEmpty()
            && Files.readString(dir.resolve(DataStoreImpl.SAVE_FILE),
                    StandardCharsets.UTF_8).contains("\"schemaVersion\": 1"));
        Files.writeString(dir.resolve(DataStoreImpl.SAVE_FILE),
            "{\"schemaVersion\":99,\"savedAt\":\"2026-11-14\",\"users\":[],"
            + "\"equipment\":[],\"loans\":[],\"penalties\":[]}",
            StandardCharsets.UTF_8);
        DataStore wrongSchema = new DataStoreImpl(dir);
        wrongSchema.load();
        ProbeSupport.check("corrupt-schema-fallback", wrongSchema.users().size() == 3
            && wrongSchema.equipment().size() == 10);
    }
}
