package kitnapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import kitnapper.service.AppContext;
import kitnapper.service.DataStoreImpl;
import kitnapper.service.persistence.JsonWriter;
import kitnapper.service.persistence.RootMapper;
import kitnapper.service.persistence.SeedData;

/**
 * Inventory, photo, and reset probe (M5 WP1, plan §9.2). Seeds a fixed date
 * T into a temp folder, then proves the inventory service, the photo pipeline,
 * and the demo reset without touching a window — headless and deterministic.
 */
public final class InventoryProbe {

    private static final LocalDate T = LocalDate.of(2026, 11, 16);

    private InventoryProbe() {
    }

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("kitnapper-inventory-probe");
        try {
            Files.writeString(dir.resolve(DataStoreImpl.SAVE_FILE),
                    JsonWriter.write(new RootMapper().toDocument(SeedData.create(T))),
                    StandardCharsets.UTF_8);
            DataStoreImpl store = new DataStoreImpl(dir);
            store.load();
            AppContext context = new AppContext(store, () -> T);
            ProbeSupport.signIn(context, store, "admin@institution.edu");
            InventoryChecks.run(context, store, dir);
            InventoryPhotoChecks.run(context.images(), dir);
            ResetChecks.run(context, store);
        } finally {
            ProbeSupport.deleteTree(dir);
        }
        System.out.println(ProbeSupport.tally());
        if (ProbeSupport.failing()) {
            System.exit(1);
        }
    }
}
