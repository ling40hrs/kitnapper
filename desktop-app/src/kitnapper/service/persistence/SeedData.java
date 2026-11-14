package kitnapper.service.persistence;

import java.time.LocalDate;

import kitnapper.model.Borrower;
import kitnapper.model.DigitalEquipment;
import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.LoanRecord;
import kitnapper.model.Manager;
import kitnapper.model.MechanicalTool;
import kitnapper.service.AppData;

/**
 * The PRD 9 demo dataset, computed relative to the date seeding runs (D4).
 * First launch and "reset demo data" both produce: three users, ten items
 * (digital kinds for camera/lens/laptop/audio categories, mechanical for
 * tools), two ACTIVE loans and one loan due four days ago so the overdue
 * dashboard has exactly one row with PHP 200.00 accrued live. The three
 * borrowed items are marked LOANED; the seed has no penalties (frozen
 * only on return) and no photos (D11).
 */
public final class SeedData {

    private SeedData() {
    }

    /** Builds the seed aggregate for the given seeding date {@code today}. */
    public static AppData create(LocalDate today) {
        AppData data = new AppData();
        addUsers(data);
        addEquipment(data);
        addLoans(data, today);
        return data;
    }

    private static void addUsers(AppData data) {
        data.users().add(new Manager("u-admin", "A. Manager",
                "admin@institution.edu", "admin123"));
        data.users().add(new Borrower("u-student1", "Student One",
                "student1@institution.edu", "student123"));
        data.users().add(new Borrower("u-student2", "Student Two",
                "student2@institution.edu", "student123"));
    }

    private static void addEquipment(AppData data) {
        data.equipment().add(digital("e-001", "Sony A7 IV",
                EquipmentCategory.CAMERA, "SN-A7IV-001", 150000.0));
        data.equipment().add(digital("e-002", "Canon EOS R6",
                EquipmentCategory.CAMERA, "SN-EOSR6-001", 145000.0));
        data.equipment().add(digital("e-003", "Sony FE 24-70mm f/2.8 GM",
                EquipmentCategory.LENS, "SN-FE2470-001", 120000.0));
        data.equipment().add(digital("e-004", "Canon RF 24-70mm f/2.8 L",
                EquipmentCategory.LENS, "SN-RF2470-001", 115000.0));
        data.equipment().add(digital("e-005", "MacBook Pro M3 (14\")",
                EquipmentCategory.LAPTOP, "SN-MBP14-001", 120000.0));
        data.equipment().add(digital("e-006", "Lenovo ThinkPad P1 Gen 6",
                EquipmentCategory.LAPTOP, "SN-TPP1G6-001", 130000.0));
        data.equipment().add(digital("e-007", "Rode Wireless PRO kit",
                EquipmentCategory.AUDIO, "SN-RWPRO-001", 28000.0));
        data.equipment().add(digital("e-008", "Zoom H6 recorder",
                EquipmentCategory.AUDIO, "SN-ZH6-001", 20000.0));
        data.equipment().add(tool("e-009", "Manfrotto 504X fluid-head tripod",
                "SN-504X-001", 35000.0));
        data.equipment().add(tool("e-010", "DeWalt 20V cordless drill kit",
                "SN-DW20V-001", 9000.0));
    }

    private static void addLoans(AppData data, LocalDate today) {
        LoanRecord withinDue = LoanRecord.checkout("L-001", "u-student1", "e-001",
                today.minusDays(2), today.plusDays(5));
        LoanRecord dueSoon = LoanRecord.checkout("L-002", "u-student2", "e-005",
                today.minusDays(6), today.plusDays(1));
        LoanRecord overdue = LoanRecord.checkout("L-003", "u-student1", "e-007",
                today.minusDays(11), today.minusDays(4));
        data.loans().add(withinDue);
        data.loans().add(dueSoon);
        data.loans().add(overdue);
        byId(data, "e-001").markLoaned();
        byId(data, "e-005").markLoaned();
        byId(data, "e-007").markLoaned();
    }

    /** Digital kind, no warranty contact on record (D19). */
    private static Equipment digital(String id, String name, EquipmentCategory category,
                                     String serialNumber, double replacementValue) {
        return new DigitalEquipment(id, name, category, serialNumber, replacementValue, null);
    }

    /** Mechanical kind, no maintenance interval on record (D19). */
    private static Equipment tool(String id, String name, String serialNumber,
                                  double replacementValue) {
        return new MechanicalTool(id, name, EquipmentCategory.TOOL, serialNumber,
                replacementValue, 0);
    }

    private static Equipment byId(AppData data, String id) {
        return data.equipment().stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
