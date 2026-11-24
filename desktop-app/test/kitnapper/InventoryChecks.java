package kitnapper;

import java.nio.file.Path;

import kitnapper.model.DigitalEquipment;
import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.EquipmentStatus;
import kitnapper.model.GeneralEquipment;
import kitnapper.model.MechanicalTool;
import kitnapper.service.AppContext;
import kitnapper.service.DataStore;
import kitnapper.service.DataStoreImpl;
import kitnapper.service.ImageRules;
import kitnapper.service.ItemDraft;

/**
 * Inventory CRUD checks (plan §9.2): seeding baseline, kind selection at
 * creation (D19), the reload round trip, field validation, kind preservation
 * on a category edit, transitions with their loan-aware guards, and the
 * manager-only rule (INV-3).
 */
final class InventoryChecks {

    private InventoryChecks() {
    }

    static void run(AppContext ctx, DataStore store, Path dir) {
        baseline(ctx, store);
        addKinds(ctx, store, dir);
        validation(ctx, store);
        updates(ctx, store);
        statuses(ctx, store);
        roles(ctx, store);
    }

    private static void baseline(AppContext ctx, DataStore store) {
        ProbeSupport.check("the seed has ten items", store.equipment().size() == 10);
        ProbeSupport.check("the next id is e-011", ctx.inventory().peekNextId().equals("e-011"));
        ProbeSupport.check("the seed references its bundled photos (D50)",
                store.equipment().stream().allMatch(item -> item.getPhotoFile()
                        .filter(name -> name.equals(item.getId() + ImageRules.OUTPUT_SUFFIX))
                        .isPresent()));
    }

    private static void addKinds(AppContext ctx, DataStore store, Path dir) {
        ctx.inventory().add(draft("Test Camera", EquipmentCategory.CAMERA, "SN-TC-1", 1000,
                "care@example.edu", 0));
        Equipment camera = ProbeSupport.item(store, "e-011");
        ProbeSupport.check("add assigns the next id", camera != null);
        ProbeSupport.check("a camera is digital", camera instanceof DigitalEquipment);
        ProbeSupport.check("the warranty contact is stored", camera instanceof DigitalEquipment digital
                && digital.getWarrantyContact().orElse("").equals("care@example.edu"));
        ProbeSupport.check("a new item starts available",
                camera != null && camera.getStatus() == EquipmentStatus.AVAILABLE);

        ctx.inventory().add(draft("Test Tripod", EquipmentCategory.TOOL, "SN-TT-1", 500,
                null, 30));
        Equipment tool = ProbeSupport.item(store, "e-012");
        ProbeSupport.check("a tool is mechanical", tool instanceof MechanicalTool);
        ProbeSupport.check("the maintenance interval is stored",
                tool instanceof MechanicalTool mechanical
                        && mechanical.getMaintenanceIntervalDays() == 30);

        ctx.inventory().add(draft("Test Misc", EquipmentCategory.OTHER, "SN-TM-1", 50, null, 0));
        ProbeSupport.check("other is the general kind",
                ProbeSupport.item(store, "e-013") instanceof GeneralEquipment);
        ProbeSupport.check("the next id advances", ctx.inventory().peekNextId().equals("e-014"));

        DataStoreImpl reloaded = new DataStoreImpl(dir);
        reloaded.load();
        Equipment saved = ProbeSupport.item(reloaded, "e-012");
        ProbeSupport.check("an added item survives a reload", saved instanceof MechanicalTool mechanical
                && mechanical.getMaintenanceIntervalDays() == 30);
        ProbeSupport.check("the reloaded dataset has thirteen items",
                reloaded.equipment().size() == 13);
    }

    private static void validation(AppContext ctx, DataStore store) {
        int before = store.equipment().size();
        ProbeSupport.check("a blank name is rejected", ProbeSupport.throwsIAE(() ->
                ctx.inventory().add(draft("  ", EquipmentCategory.CAMERA, "SN-V-1", 10, null, 0))));
        ProbeSupport.check("a blank serial is rejected", ProbeSupport.throwsIAE(() ->
                ctx.inventory().add(draft("Valid", EquipmentCategory.CAMERA, " ", 10, null, 0))));
        ProbeSupport.check("a negative value is rejected", ProbeSupport.throwsIAE(() ->
                ctx.inventory().add(draft("Valid", EquipmentCategory.CAMERA, "SN-V-2", -1, null, 0))));
        ProbeSupport.check("a negative interval is rejected", ProbeSupport.throwsIAE(() ->
                ctx.inventory().add(draft("Valid", EquipmentCategory.TOOL, "SN-V-3", 10, null, -1))));
        ProbeSupport.check("a rejected add changes nothing", store.equipment().size() == before);
    }

    private static void updates(AppContext ctx, DataStore store) {
        ctx.inventory().update("e-011", draft("Renamed Camera", EquipmentCategory.TOOL,
                "SN-TC-2", 2000, "new@example.edu", 45));
        Equipment updated = ProbeSupport.item(store, "e-011");
        ProbeSupport.check("a category change keeps the digital kind",
                updated instanceof DigitalEquipment);
        ProbeSupport.check("a category change applies the category",
                updated.getCategory() == EquipmentCategory.TOOL);
        ProbeSupport.check("the name is updated", updated.getName().equals("Renamed Camera"));
        ProbeSupport.check("the value is updated", updated.getReplacementValue() == 2000);
        ProbeSupport.check("the kind's own field is updated",
                updated instanceof DigitalEquipment digital
                        && digital.getWarrantyContact().orElse("").equals("new@example.edu"));

        ctx.inventory().update("e-012", draft("Renamed Tripod", EquipmentCategory.TOOL,
                "SN-TT-1", 500, null, 90));
        ProbeSupport.check("a mechanical item keeps its interval field",
                ProbeSupport.item(store, "e-012") instanceof MechanicalTool mechanical
                        && mechanical.getMaintenanceIntervalDays() == 90);
        ProbeSupport.check("an unknown id is rejected", ProbeSupport.throwsISE(() ->
                ctx.inventory().update("e-999", draft("Ghost", EquipmentCategory.OTHER,
                        "SN-X", 1, null, 0))));
    }

    private static void statuses(AppContext ctx, DataStore store) {
        ctx.inventory().flagMaintenance("e-002");
        ProbeSupport.check("flagging moves an available item to maintenance",
                ProbeSupport.item(store, "e-002").getStatus() == EquipmentStatus.UNDER_MAINTENANCE);
        ProbeSupport.check("a double flag is rejected",
                ProbeSupport.throwsISE(() -> ctx.inventory().flagMaintenance("e-002")));
        ctx.inventory().clearMaintenance("e-002");
        ProbeSupport.check("clearing returns the item",
                ProbeSupport.item(store, "e-002").getStatus() == EquipmentStatus.AVAILABLE);
        ProbeSupport.check("clearing an available item is rejected",
                ProbeSupport.throwsISE(() -> ctx.inventory().clearMaintenance("e-002")));

        ctx.inventory().decommission("e-006");
        ProbeSupport.check("decommission retires the item",
                ProbeSupport.item(store, "e-006").getStatus() == EquipmentStatus.DECOMMISSIONED);
        ProbeSupport.check("decommission is terminal",
                ProbeSupport.throwsISE(() -> ctx.inventory().decommission("e-006")));
        ProbeSupport.check("a retired item is read-only", ProbeSupport.throwsISE(() ->
                ctx.inventory().update("e-006", draft("Nope", EquipmentCategory.LAPTOP,
                        "SN-N", 10, null, 0))));
        ProbeSupport.check("a retired item keeps its row", store.equipment().size() == 13);

        ctx.inventory().flagMaintenance("e-001");
        ProbeSupport.check("a loaned item can be flagged (D15)",
                ProbeSupport.item(store, "e-001").getStatus() == EquipmentStatus.UNDER_MAINTENANCE);
        ProbeSupport.check("clearing is blocked while the item is out",
                ProbeSupport.throwsISE(() -> ctx.inventory().clearMaintenance("e-001")));
        ProbeSupport.check("decommissioning is blocked while the item is out",
                ProbeSupport.throwsISE(() -> ctx.inventory().decommission("e-001")));
        ProbeSupport.check("the blocked item keeps its state",
                ProbeSupport.item(store, "e-001").getStatus() == EquipmentStatus.UNDER_MAINTENANCE);

        ctx.inventory().flagMaintenance("e-009");
        ctx.inventory().decommission("e-009");
        ProbeSupport.check("a maintenance item can be retired",
                ProbeSupport.item(store, "e-009").getStatus() == EquipmentStatus.DECOMMISSIONED);
    }

    private static void roles(AppContext ctx, DataStore store) {
        ProbeSupport.signIn(ctx, store, "student1@institution.edu");
        ProbeSupport.check("a borrower cannot add", ProbeSupport.throwsISE(() ->
                ctx.inventory().add(draft("Nope", EquipmentCategory.OTHER, "SN-R-1", 1, null, 0))));
        ProbeSupport.check("a borrower cannot decommission",
                ProbeSupport.throwsISE(() -> ctx.inventory().decommission("e-010")));
        ProbeSupport.check("a borrower cannot reset",
                ProbeSupport.throwsISE(() -> ctx.inventory().resetDemoData()));
        ProbeSupport.check("a borrower changes nothing", store.equipment().size() == 13);
        ProbeSupport.signIn(ctx, store, "admin@institution.edu");
    }

    private static ItemDraft draft(String name, EquipmentCategory category, String serial,
                                   double value, String warranty, int interval) {
        return new ItemDraft(name, category, serial, value, warranty, interval);
    }
}
