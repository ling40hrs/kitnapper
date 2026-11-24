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
 * Seed-photo probe (D50, design §12). Seeds a fixed date T into a temp folder
 * and proves the bundled photos install on load, survive the orphan sweep, and
 * behave on re-install — headless and deterministic.
 */
public final class SeedPhotosProbe {

    private static final LocalDate T = LocalDate.of(2026, 11, 16);

    private SeedPhotosProbe() {
    }

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("kitnapper-seed-photos-probe");
        try {
            Files.writeString(dir.resolve(DataStoreImpl.SAVE_FILE),
                    JsonWriter.write(new RootMapper().toDocument(SeedData.create(T))),
                    StandardCharsets.UTF_8);
            DataStoreImpl store = new DataStoreImpl(dir);
            store.load();
            AppContext context = new AppContext(store, () -> T);
            ProbeSupport.signIn(context, store, "admin@institution.edu");
            SeedPhotoChecks.run(context, store);
        } finally {
            ProbeSupport.deleteTree(dir);
        }
        System.out.println(ProbeSupport.tally());
        if (ProbeSupport.failing()) {
            System.exit(1);
        }
    }
}
