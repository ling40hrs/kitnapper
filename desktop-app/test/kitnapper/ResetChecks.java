package kitnapper;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import kitnapper.model.Equipment;
import kitnapper.model.Manager;
import kitnapper.model.User;
import kitnapper.service.AppContext;
import kitnapper.service.DataStore;
import kitnapper.service.ImageRules;

/**
 * Demo-reset checks (plan §9.2, FR-12, D11, D33): the dataset returns to the
 * PRD §9 baseline, the photo folder is emptied, references are dropped, and
 * the signed-in manager is re-bound to the freshly seeded instance.
 */
final class ResetChecks {

    private ResetChecks() {
    }

    static void run(AppContext ctx, DataStore store) throws IOException {
        Path source = Files.createTempFile("kitnapper-reset", ".png");
        ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB), "png", source.toFile());
        String staged = ctx.images().stage(source, 0x101010);
        ctx.inventory().setPhoto("e-011", staged);

        Equipment withPhoto = ProbeSupport.item(store, "e-011");
        ProbeSupport.check("a photo is attached before the reset",
                withPhoto != null && withPhoto.getPhotoFile().isPresent());
        ProbeSupport.check("the attached photo file exists",
                withPhoto != null && Files.exists(ctx.images().folder()
                        .resolve(withPhoto.getPhotoFile().orElseThrow())));

        User before = ctx.currentUser();
        ctx.inventory().resetDemoData();
        ProbeSupport.check("the reset restores ten items", store.equipment().size() == 10);
        ProbeSupport.check("the reset restores three loans", store.loans().size() == 3);
        ProbeSupport.check("the reset clears penalties", store.penalties().isEmpty());
        ProbeSupport.check("the reset drops photo references", store.equipment().stream()
                .allMatch(item -> item.getPhotoFile().isEmpty()));
        ProbeSupport.check("the reset wipes the photo folder",
                !Files.isDirectory(ctx.images().folder())
                        || jpgCount(ctx.images().folder()) == 0);

        ctx.rebindCurrentUser();
        User after = ctx.currentUser();
        ProbeSupport.check("the session is re-bound to a fresh instance", after != null && after != before);
        ProbeSupport.check("the session keeps the same account",
                after != null && after.getEmail().equals("admin@institution.edu"));
        ProbeSupport.check("the session user is in the new dataset",
                after != null && store.users().contains(after));
        ProbeSupport.check("the session role is still manager", after instanceof Manager);
    }

    private static long jpgCount(Path folder) {
        try (var entries = Files.list(folder)) {
            return entries.filter(file -> file.getFileName().toString()
                    .endsWith(ImageRules.OUTPUT_SUFFIX)).count();
        } catch (IOException missing) {
            return 0;
        }
    }
}
