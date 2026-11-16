package kitnapper.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

/**
 * JDK-only photo pipeline (design §12): validate extension, size and
 * decodability, downscale to 1024 px longest edge, flatten transparency onto
 * the caller's matte, re-encode JPEG q0.85, and write atomically through
 * {@link PhotoCodec}. Reads are deliberately tolerant — a missing or corrupt
 * photo is empty, never an exception (NFR-08).
 */
public final class ImageStoreImpl implements ImageStore {

    private static final AtomicInteger STAGED = new AtomicInteger();

    private final Path folder;

    /** @param appFolder the folder holding {@code kitnapper-data.json} */
    public ImageStoreImpl(Path appFolder) {
        Objects.requireNonNull(appFolder, "appFolder");
        this.folder = appFolder.resolve(ImageRules.FOLDER);
    }

    @Override
    public Path folder() {
        return folder;
    }

    @Override
    public String stage(Path source, int matteRgb) {
        Objects.requireNonNull(source, "source");
        BufferedImage prepared = PhotoCodec.downscale(decode(source), ImageRules.MAX_EDGE_PX);
        String name = ImageRules.STAGING_PREFIX + System.currentTimeMillis()
                + "-" + STAGED.incrementAndGet() + ImageRules.OUTPUT_SUFFIX;
        PhotoCodec.writeJpeg(PhotoCodec.flatten(prepared, matteRgb), folder.resolve(name));
        return name;
    }

    @Override
    public String promote(String stagedName, String equipmentId) {
        String name = requireBareName(equipmentId) + ImageRules.OUTPUT_SUFFIX;
        Path from = existing(stagedName);
        Path to = folder.resolve(name);
        try {
            Files.createDirectories(folder);
            PhotoCodec.move(from, to);
        } catch (IOException failure) {
            throw new java.io.UncheckedIOException("Could not store " + to, failure);
        }
        return name;
    }

    @Override
    public Optional<BufferedImage> load(String photoFile, int maxEdge) {
        Path file = optionalFile(photoFile);
        if (file == null) {
            return Optional.empty();
        }
        try {
            BufferedImage decoded = ImageIO.read(file.toFile());
            return decoded == null
                    ? Optional.empty()
                    : Optional.of(PhotoCodec.downscale(decoded, maxEdge));
        } catch (IOException | RuntimeException unreadable) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String photoFile) {
        Path file = optionalFile(photoFile);
        if (file != null) {
            PhotoCodec.deleteQuietly(file);
        }
    }

    @Override
    public void deleteAll() {
        forEachFile(PhotoCodec::deleteQuietly);
    }

    @Override
    public void sweep(Collection<String> equipmentIds) {
        Set<String> keep = equipmentIds == null ? new HashSet<>() : new HashSet<>(equipmentIds);
        forEachFile(file -> {
            String stem = stemOf(file.getFileName().toString());
            if (stem == null) {
                return;
            }
            if (stem.startsWith(ImageRules.STAGING_PREFIX) || !keep.contains(stem)) {
                PhotoCodec.deleteQuietly(file);
            }
        });
    }

    /** Reads and validates a chosen source file (extension, size, contents). */
    private BufferedImage decode(Path source) {
        requireExtension(source);
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException(ImageMessages.SOURCE_MISSING);
        }
        try {
            if (Files.size(source) > ImageRules.MAX_SOURCE_BYTES) {
                throw new IllegalArgumentException(ImageMessages.TOO_LARGE);
            }
            BufferedImage decoded = ImageIO.read(source.toFile());
            if (decoded == null) {
                throw new IllegalArgumentException(ImageMessages.UNREADABLE);
            }
            return decoded;
        } catch (IOException unreadable) {
            throw new IllegalArgumentException(ImageMessages.UNREADABLE, unreadable);
        }
    }

    private static void requireExtension(Path source) {
        String name = source.getFileName() == null ? "" : source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ImageRules.EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(ImageMessages.unsupportedType(extension));
        }
    }

    /** Every regular file in the folder, skipping a missing directory. */
    private void forEachFile(Consumer<Path> action) {
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (Stream<Path> entries = Files.list(folder)) {
            for (Path entry : entries.toList()) {
                if (Files.isRegularFile(entry)) {
                    action.accept(entry);
                }
            }
        } catch (IOException ignored) {
            // Best effort: the folder is advisory state, never required.
        }
    }

    private static String stemOf(String name) {
        if (!name.endsWith(ImageRules.OUTPUT_SUFFIX)) {
            return null;
        }
        return name.substring(0, name.length() - ImageRules.OUTPUT_SUFFIX.length());
    }

    private Path existing(String name) {
        Path file = folder.resolve(requireBareName(name));
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException(ImageMessages.STAGED_MISSING);
        }
        return file;
    }

    private Path optionalFile(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            Path file = folder.resolve(requireBareName(name));
            return Files.isRegularFile(file) ? file : null;
        } catch (IllegalArgumentException unsafe) {
            return null;
        }
    }

    private static String requireBareName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.contains("/") || trimmed.contains("\\")
                || trimmed.contains("..")) {
            throw new IllegalArgumentException(ImageMessages.UNSAFE_NAME);
        }
        return trimmed;
    }
}
