package kitnapper.view;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

/** Scratch-file fixtures for the editor probe (M5 WP4). Test-only. */
final class EditorProbeSupport {

    private EditorProbeSupport() {
    }

    static Path photo(Path dir, String name) {
        Path file = dir.resolve(name);
        try {
            ImageIO.write(new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB), "png",
                file.toFile());
        } catch (IOException failed) {
            throw new IllegalStateException("could not write a probe photo", failed);
        }
        return file;
    }

    static Path textFile(Path dir, String name) {
        try {
            return Files.writeString(dir.resolve(name), "not a photo");
        } catch (IOException failed) {
            throw new IllegalStateException("could not write a probe file", failed);
        }
    }

    static long jpgCount(Path folder) {
        if (!Files.isDirectory(folder)) {
            return 0;
        }
        try (Stream<Path> entries = Files.list(folder)) {
            return entries.filter(file -> file.getFileName().toString().endsWith(".jpg")).count();
        } catch (IOException missing) {
            return 0;
        }
    }

    static void deleteTree(Path dir) {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Best effort cleanup of a scratch directory.
                }
            });
        } catch (IOException ignored) {
            // Best effort cleanup of a scratch directory.
        }
    }
}
