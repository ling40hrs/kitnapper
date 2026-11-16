package kitnapper.view;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import kitnapper.controller.BorrowingController;
import kitnapper.controller.CatalogActions;
import kitnapper.controller.CatalogController;
import kitnapper.controller.InventoryController;
import kitnapper.controller.ItemPhotos;
import kitnapper.controller.SettingsController;
import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.EquipmentStatus;
import kitnapper.model.UserRole;
import kitnapper.service.AppContext;
import kitnapper.service.DataStoreImpl;
import kitnapper.service.persistence.JsonWriter;
import kitnapper.service.persistence.RootMapper;
import kitnapper.service.persistence.SeedData;

/**
 * M5 end-to-end flow (SRS §6.2 steps 11–12, plan §10.1): add an item with a
 * real photo, see it on the catalog preview, decommission it, restart and find
 * both still there, then reset the demo data and confirm the baseline plus an
 * empty photo folder. Chains the real controllers and screens, not a mock.
 */
public final class M5FlowProbe {

    private static final LocalDate T = LocalDate.of(2026, 11, 16);
    private static final int MAGENTA = 0xE040A0;

    private M5FlowProbe() {
    }

    public static void main(String[] args) throws Exception {
        Theme.apply(true);
        Path dir = Files.createTempDirectory("kitnapper-flow-probe");
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
        ItemPhotos photos = new ItemPhotos(ctx);
        InventoryController inventory = new InventoryController(ctx);

        PhotoStage staged = inventory.stagePhoto(photo(dir));
        UiProbeSupport.check("step 11: the chosen photo stages", staged.ok());
        EquipmentForm form = new EquipmentForm("Flow Camera", EquipmentCategory.CAMERA,
            "SN-FLOW-1", "2,500", "", "");
        UiProbeSupport.check("step 11: the item saves with its photo",
            inventory.add(form, staged.stagedName()).isEmpty());

        Equipment added = byName(store, "Flow Camera");
        UiProbeSupport.check("step 11: the new item is listed", added != null);
        UiProbeSupport.check("step 11: the photo is attached to the item",
            added != null && added.getPhotoFile().orElse("").equals(added.getId() + ".jpg"));

        // Decode once and memoise it, so the preview paints from the cache and
        // this probe stays fully on the EDT (the async gate is PhotoUiProbe's job).
        String photoName = added.getPhotoFile().orElseThrow();
        BufferedImage decoded = ctx.images().load(photoName, PhotoTile.PREVIEW_EDGE_PX)
            .orElseThrow();
        PhotoCache.put(photoName, PhotoTile.PREVIEW_EDGE_PX, decoded);

        MainFrame frame = new MainFrame(UserRole.MANAGER, "A. Manager", "",
            key -> { }, () -> { }, () -> { });
        CatalogController catalog = new CatalogController(ctx);
        CatalogScreen screen = new CatalogScreen(
            new CatalogActions(catalog, new BorrowingController(ctx), (left, right) -> { }),
            frame.searchField(), photos);
        frame.mount("catalog", screen, screen.commands(), screen::onShown);
        frame.showScreen("catalog");
        frame.setVisible(true);
        UiProbeSupport.settle(frame);

        select(screen, added);
        UiProbeSupport.check("step 11: the catalog preview shows the photo",
            magentaCount(screen.preview()) > 100);

        UiProbeSupport.check("step 11: decommission succeeds",
            inventory.decommission(added).isEmpty());
        screen.reload();
        UiProbeSupport.check("step 11: the retired item stays listed",
            screen.table().getRowCount() == 11);
        UiProbeSupport.check("step 11: the retired item keeps its photo",
            added.getStatus() == EquipmentStatus.DECOMMISSIONED
                && added.getPhotoFile().isPresent());
        UiProbeSupport.check("step 11: the retired item cannot be checked out",
            !catalog.canCheckout(added));

        DataStoreImpl reloaded = new DataStoreImpl(dir);
        reloaded.load();
        Equipment persisted = byName(reloaded, "Flow Camera");
        UiProbeSupport.check("step 11: a restart keeps the item", persisted != null);
        UiProbeSupport.check("step 11: a restart keeps the photo reference",
            persisted != null && persisted.getPhotoFile().isPresent());
        AppContext restarted = new AppContext(reloaded, () -> T);
        UiProbeSupport.check("step 11: a restart keeps the photo loadable",
            persisted != null && restarted.images()
                .load(persisted.getPhotoFile().orElseThrow(), 640).isPresent());

        SettingsController settings = new SettingsController(ctx, dark -> Theme.apply(dark),
            () -> { });
        ctx.setCurrentUser(ctx.auth().login("admin@institution.edu", "admin123").orElseThrow());
        UiProbeSupport.check("step 12: reset succeeds", settings.resetDemoData().isEmpty());
        UiProbeSupport.check("step 12: the baseline is restored",
            store.equipment().size() == 10 && store.loans().size() == 3
                && store.penalties().isEmpty());
        UiProbeSupport.check("step 12: the photo folder is empty",
            EditorProbeSupport.jpgCount(ctx.images().folder()) == 0);
        UiProbeSupport.check("step 12: the added item is gone", byName(store, "Flow Camera") == null);
        frame.dispose();
    }

    private static void select(CatalogScreen screen, Equipment item) {
        for (int row = 0; row < screen.table().getRowCount(); row++) {
            Equipment candidate = screen.table().itemAtView(row);
            if (candidate != null && candidate.getId().equals(item.getId())) {
                screen.table().setRowSelectionInterval(row, row);
                return;
            }
        }
    }

    private static Equipment byName(DataStoreImpl store, String name) {
        return store.equipment().stream()
            .filter(candidate -> candidate.getName().equals(name))
            .findFirst().orElse(null);
    }

    private static int magentaCount(PreviewPane preview) {
        PhotoTile tile = UiProbeSupport.find(preview, PhotoTile.class);
        BufferedImage shot = PhotoProbeSupport.paintOf(tile);
        int count = 0;
        for (int y = 0; y < shot.getHeight(); y += 3) {
            for (int x = 0; x < shot.getWidth(); x += 3) {
                if ((shot.getRGB(x, y) & 0xFFFFFF) == MAGENTA) {
                    count++;
                }
            }
        }
        return count;
    }

    private static Path photo(Path dir) {
        Path file = dir.resolve("flow.png");
        BufferedImage image = new BufferedImage(240, 135, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(new java.awt.Color(MAGENTA));
        g2.fillRect(0, 0, 240, 135);
        g2.dispose();
        try {
            javax.imageio.ImageIO.write(image, "png", file.toFile());
        } catch (java.io.IOException failed) {
            throw new IllegalStateException("could not write the probe photo", failed);
        }
        return file;
    }
}
