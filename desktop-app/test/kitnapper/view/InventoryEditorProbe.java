package kitnapper.view;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import javax.swing.JComboBox;

import kitnapper.controller.InventoryController;
import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.service.AppContext;
import kitnapper.service.DataStoreImpl;
import kitnapper.service.persistence.JsonWriter;
import kitnapper.service.persistence.RootMapper;
import kitnapper.service.persistence.SeedData;

/**
 * Editor probe (M5 WP4, plan §9.4): the form's kind field per mode and
 * category (D19), the controller's add/update/photo paths against a temp
 * folder, and the messages a rejected import or an invalid form produces.
 * Runs as a display probe; nothing modal is shown.
 */
public final class InventoryEditorProbe {

    private static final LocalDate T = LocalDate.of(2026, 11, 16);
    private static final int TOOL_INDEX = 4;

    private InventoryEditorProbe() {
    }

    public static void main(String[] args) throws Exception {
        Theme.apply(true);
        Path dir = Files.createTempDirectory("kitnapper-editor-probe");
        try {
            Files.writeString(dir.resolve(DataStoreImpl.SAVE_FILE),
                JsonWriter.write(new RootMapper().toDocument(SeedData.create(T))),
                StandardCharsets.UTF_8);
            DataStoreImpl store = new DataStoreImpl(dir);
            store.load();
            AppContext ctx = new AppContext(store, () -> T);
            ctx.setCurrentUser(ctx.auth().login("admin@institution.edu", "admin123")
                .orElseThrow());
            javax.swing.SwingUtilities.invokeAndWait(() -> run(ctx, store, dir));
        } finally {
            EditorProbeSupport.deleteTree(dir);
        }
        UiProbeSupport.finish();
    }

    private static void run(AppContext ctx, DataStoreImpl store, Path dir) {
        kindFields(store);
        photoPipeline(ctx, store, dir);
        validation(ctx);
    }

    /** D19: add mode follows the category, edit mode follows the kind. */
    private static void kindFields(DataStoreImpl store) {
        EquipmentFields add = new EquipmentFields(null);
        JComboBox<?> combo = UiProbeSupport.find(add, JComboBox.class);
        UiProbeSupport.check("add mode starts on a digital field", add.showsWarranty());
        combo.setSelectedIndex(TOOL_INDEX);
        UiProbeSupport.check("a tool draft swaps to the interval field", !add.showsWarranty());
        combo.setSelectedIndex(0);
        UiProbeSupport.check("switching back restores the warranty field", add.showsWarranty());

        EquipmentFields edit = new EquipmentFields(item(store, "e-002"));
        JComboBox<?> editCombo = UiProbeSupport.find(edit, JComboBox.class);
        UiProbeSupport.check("edit mode shows the existing kind's field", edit.showsWarranty());
        editCombo.setSelectedIndex(TOOL_INDEX);
        UiProbeSupport.check("a category edit keeps the digital field", edit.showsWarranty());

        EquipmentFields mechanical = new EquipmentFields(item(store, "e-009"));
        UiProbeSupport.check("a mechanical item shows the interval field",
            !mechanical.showsWarranty());
        UiProbeSupport.check("a mechanical edit keeps its interval field",
            !mechanical.showsWarranty());
    }

    private static void photoPipeline(AppContext ctx, DataStoreImpl store, Path dir) {
        InventoryController controller = new InventoryController(ctx);
        PhotoStage staged = controller.stagePhoto(EditorProbeSupport.photo(dir, "first.png"));
        UiProbeSupport.check("staging a png succeeds", staged.ok());
        UiProbeSupport.check("the staged file is written",
            Files.exists(ctx.images().folder().resolve(staged.stagedName())));

        EquipmentForm form = new EquipmentForm("Probe Camera", EquipmentCategory.CAMERA,
            "SN-P-1", "1,000", "care@example.edu", "");
        UiProbeSupport.check("adding with a photo succeeds",
            controller.add(form, staged.stagedName()).isEmpty());
        Equipment added = item(store, "e-011");
        UiProbeSupport.check("the new item carries the photo",
            added != null && added.getPhotoFile().orElse("").equals("e-011.jpg"));
        UiProbeSupport.check("the promoted file exists",
            Files.exists(ctx.images().folder().resolve("e-011.jpg")));
        UiProbeSupport.check("the item kind follows the category",
            added instanceof kitnapper.model.DigitalEquipment);

        Path text = EditorProbeSupport.textFile(dir, "bad.txt");
        PhotoStage rejected = controller.stagePhoto(text);
        UiProbeSupport.check("a rejected import returns an inline message",
            !rejected.ok() && rejected.error().contains("Unsupported image type"));
        UiProbeSupport.check("a rejected import writes nothing", EditorProbeSupport.jpgCount(ctx.images().folder()) == 1);

        PhotoStage second = controller.stagePhoto(EditorProbeSupport.photo(dir, "second.png"));
        UiProbeSupport.check("replacing the photo succeeds",
            controller.update("e-011", form, second.stagedName(), false).isEmpty());
        UiProbeSupport.check("the replacement is no longer staged",
            !Files.exists(ctx.images().folder().resolve(second.stagedName())));
        UiProbeSupport.check("the replaced photo still loads",
            ctx.images().load("e-011.jpg", 160).isPresent());

        PhotoStage discarded = controller.stagePhoto(EditorProbeSupport.photo(dir, "third.png"));
        controller.discardStaged(discarded.stagedName());
        UiProbeSupport.check("discarding removes the staged file",
            !Files.exists(ctx.images().folder().resolve(discarded.stagedName())));

        UiProbeSupport.check("removing the photo succeeds",
            controller.update("e-011", form, null, true).isEmpty());
        UiProbeSupport.check("the photo reference is cleared",
            item(store, "e-011").getPhotoFile().isEmpty());
        UiProbeSupport.check("the photo file is deleted",
            !Files.exists(ctx.images().folder().resolve("e-011.jpg")));
    }

    private static void validation(AppContext ctx) {
        InventoryController controller = new InventoryController(ctx);
        UiProbeSupport.check("a blank name is rejected",
            message(controller.add(form(" ", "1", ""), null)).contains("Name is required"));
        UiProbeSupport.check("a blank serial is rejected",
            message(controller.add(new EquipmentForm("Name", EquipmentCategory.CAMERA,
                " ", "1", "", ""), null)).contains("Serial number is required"));
        UiProbeSupport.check("a non-numeric value is rejected",
            message(controller.add(form("Name", "abc", ""), null)).contains("number"));
        UiProbeSupport.check("a negative value is rejected",
            message(controller.add(form("Name", "-5", ""), null)).contains("zero or more"));
        UiProbeSupport.check("a non-numeric interval is rejected",
            message(controller.add(new EquipmentForm("Name", EquipmentCategory.TOOL,
                "SN-V", "1", "", "abc"), null)).contains("whole number"));
    }

    private static EquipmentForm form(String name, String value, String interval) {
        return new EquipmentForm(name, EquipmentCategory.CAMERA, "SN-V", value, "", interval);
    }

    private static String message(Optional<String> result) {
        return result.orElse("");
    }

    private static Equipment item(DataStoreImpl store, String id) {
        return store.equipment().stream()
            .filter(candidate -> candidate.getId().equals(id))
            .findFirst().orElse(null);
    }

}
