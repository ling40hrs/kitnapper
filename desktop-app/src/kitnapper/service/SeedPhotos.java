package kitnapper.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

import kitnapper.model.Equipment;

/**
 * Restores the bundled seed photos into {@code kitnapper-images/} (design §12,
 * D50). The ten images in {@code resources/photos} are shipped already encoded
 * by the §12 pipeline (1024 px, JPEG q0.85), so installing one is a file copy,
 * never a decode: first launch and "reset demo data" stay instant.
 *
 * <p>The copy is idempotent and never overwrites: an item whose photo file
 * already exists keeps it, so a manager's replacement survives a restart, and
 * an item with no {@code photoFile} is skipped entirely. A missing or
 * unreadable bundled image is logged and skipped — the category tile takes
 * over (NFR-08), exactly as for any other absent photo.
 */
public final class SeedPhotos {

    /** Classpath folder inside the jar, next to {@code /fonts}. */
    private static final String RESOURCE_PREFIX = "/photos/";

    /** Filesystem fallback, the same shape {@code view/Fonts} uses. */
    private static final String FALLBACK_PREFIX = "resources/photos/";

    private SeedPhotos() {
    }

    /**
     * Copies every bundled photo an item references but does not have yet.
     *
     * @param images the photo store whose folder receives the files
     * @param items  the current equipment list (seed data on first launch)
     */
    public static void install(ImageStore images, List<Equipment> items) {
        Objects.requireNonNull(images, "images");
        if (items == null) {
            return;
        }
        for (Equipment item : items) {
            String name = item.getPhotoFile().orElse(null);
            if (name == null) {
                continue;
            }
            Path target = images.folder().resolve(name);
            if (Files.exists(target)) {
                continue;
            }
            copyBundled(name, target);
        }
    }

    private static void copyBundled(String name, Path target) {
        try (InputStream bundled = open(name)) {
            if (bundled == null) {
                System.err.println("KitNapper: bundled photo missing (" + name
                        + "); the category tile is used.");
                return;
            }
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.copy(bundled, tmp, StandardCopyOption.REPLACE_EXISTING);
            move(tmp, target);
        } catch (IOException failure) {
            System.err.println("KitNapper: could not install bundled photo (" + name
                    + "); the category tile is used.");
        }
    }

    /** The bundled JPEG from the jar, or from {@code resources/photos} on disk. */
    private static InputStream open(String name) throws IOException {
        InputStream in = SeedPhotos.class.getResourceAsStream(RESOURCE_PREFIX + name);
        if (in != null) {
            return in;
        }
        File local = new File(FALLBACK_PREFIX + name);
        return local.isFile() ? new FileInputStream(local) : null;
    }

    /** Atomic move with the Windows replace fallback the save file also uses. */
    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException notAtomic) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
