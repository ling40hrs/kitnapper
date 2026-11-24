package kitnapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import kitnapper.model.Equipment;
import kitnapper.service.AppContext;
import kitnapper.service.DataStore;
import kitnapper.service.ImageRules;
import kitnapper.service.SeedPhotos;

/**
 * Bundled seed-photo checks (D50, design §12): the ten shipped JPEGs are
 * installed beside the save file before the orphan sweep, install is
 * idempotent and never overwrites a manager's replacement, and an item with
 * no photo — or one whose bundled file is missing — is skipped, not fatal.
 */
final class SeedPhotoChecks {

    private SeedPhotoChecks() {
    }

    static void run(AppContext ctx, DataStore store) throws IOException {
        Path folder = ctx.images().folder();
        List<Equipment> items = store.equipment();
        long baseline = jpgCount(folder);

        ProbeSupport.check("every seed item references its own photo", items.stream()
                .allMatch(item -> item.getPhotoFile()
                        .orElse("").equals(item.getId() + ImageRules.OUTPUT_SUFFIX)));
        ProbeSupport.check("the ten seed photos are installed", baseline == items.size());
        ProbeSupport.check("a seed photo decodes at the preview edge",
                ctx.images().load("e-001.jpg", 640).isPresent());

        Path existing = folder.resolve("e-002.jpg");
        byte[] replaced = "manager-supplied".getBytes(StandardCharsets.UTF_8);
        Files.write(existing, replaced);
        SeedPhotos.install(ctx.images(), store.equipment());
        ProbeSupport.check("install never overwrites an existing photo",
                Arrays.equals(replaced, Files.readAllBytes(existing)));
        ProbeSupport.check("install is idempotent", jpgCount(folder) == baseline);

        Equipment first = items.get(0);
        String original = first.getPhotoFile().orElseThrow();
        Files.delete(folder.resolve(original));
        first.setPhotoFile(null);
        SeedPhotos.install(ctx.images(), store.equipment());
        ProbeSupport.check("an item with no photo is skipped",
                !Files.exists(folder.resolve(original)));

        first.setPhotoFile("e-999.jpg");
        SeedPhotos.install(ctx.images(), store.equipment());
        ProbeSupport.check("a missing bundled photo is skipped",
                !Files.exists(folder.resolve("e-999.jpg")));

        first.setPhotoFile(original);
        SeedPhotos.install(ctx.images(), store.equipment());
        ProbeSupport.check("restoring the reference reinstalls the photo",
                Files.exists(folder.resolve(original)));
    }

    private static long jpgCount(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) {
            return 0;
        }
        try (var entries = Files.list(folder)) {
            return entries.filter(file -> file.getFileName().toString()
                    .endsWith(ImageRules.OUTPUT_SUFFIX)).count();
        }
    }
}
