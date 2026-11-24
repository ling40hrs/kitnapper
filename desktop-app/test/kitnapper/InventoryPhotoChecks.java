package kitnapper;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import javax.imageio.ImageIO;

import kitnapper.service.ImageRules;
import kitnapper.service.ImageStore;

/**
 * Photo pipeline checks (plan §9.2, design §12): validation, downscaling,
 * alpha flattening, tolerant reads, promotion, the orphan sweep, and the
 * wipe. Runs headless against a scratch folder.
 */
final class InventoryPhotoChecks {

    private static final int MATTE = 0x102030;
    /** Display decode targets the store must honour (design §12). */
    private static final int CARD_EDGE = 160;
    private static final int PREVIEW_EDGE = 640;

    private InventoryPhotoChecks() {
    }

    static void run(ImageStore images, Path dir) throws IOException {
        // D50: the folder already holds the ten bundled seed photos, so the
        // import checks count from this baseline instead of from zero.
        long base = jpgCount(images.folder());
        imports(images, dir, base);
        rejections(images, dir, base);
        tolerantReads(images, dir);
        promoteAndSweep(images, dir);
        ProbeSupport.check("photo folder sits beside the save file",
                images.folder().equals(dir.resolve(ImageRules.FOLDER)));
        images.deleteAll();
        ProbeSupport.check("deleteAll empties the folder", jpgCount(images.folder()) == 0);
    }

    private static void imports(ImageStore images, Path dir, long base) throws IOException {
        Path large = write(dir, "large.jpg", 3000, 2000, TYPE_INT_RGB, Color.ORANGE, "jpg");
        String staged = images.stage(large, MATTE);
        ProbeSupport.check("stage names the file as staging",
                staged.startsWith(ImageRules.STAGING_PREFIX));
        ProbeSupport.check("stage writes one file", jpgCount(images.folder()) == base + 1);
        BufferedImage loaded = images.load(staged, ImageRules.MAX_EDGE_PX).orElseThrow();
        ProbeSupport.check("downscale caps the longest edge",
                Math.max(loaded.getWidth(), loaded.getHeight()) == ImageRules.MAX_EDGE_PX);
        ProbeSupport.check("downscale keeps the aspect ratio",
                loaded.getWidth() == 1024 && loaded.getHeight() == 683);

        Path small = write(dir, "small.png", 100, 60, TYPE_INT_RGB, Color.BLUE, "png");
        String smallStaged = images.stage(small, MATTE);
        BufferedImage smallLoaded = images.load(smallStaged, CARD_EDGE).orElseThrow();
        ProbeSupport.check("a small photo is never upscaled", smallLoaded.getWidth() == 100);

        Path alpha = write(dir, "alpha.png", 200, 200, BufferedImage.TYPE_INT_ARGB, null, "png");
        String alphaStaged = images.stage(alpha, MATTE);
        BufferedImage flattened = images.load(alphaStaged, PREVIEW_EDGE).orElseThrow();
        ProbeSupport.check("alpha is flattened onto the matte",
                (flattened.getRGB(1, 1) & 0xFFFFFF) == MATTE);
        ProbeSupport.check("a stored photo is opaque", flattened.getRGB(1, 1) >>> 24 == 0xFF);
    }

    private static void rejections(ImageStore images, Path dir, long base) throws IOException {
        Path text = Files.writeString(dir.resolve("notes.txt"), "not an image");
        ProbeSupport.check("unsupported extension rejected",
                ProbeSupport.throwsIAE(() -> images.stage(text, MATTE)));

        Path huge = dir.resolve("huge.jpg");
        try (RandomAccessFile file = new RandomAccessFile(huge.toFile(), "rw")) {
            file.setLength(ImageRules.MAX_SOURCE_BYTES + 1);
        }
        ProbeSupport.check("oversized source rejected",
                ProbeSupport.throwsIAE(() -> images.stage(huge, MATTE)));

        Path corrupt = Files.write(dir.resolve("corrupt.jpg"),
                "definitely not a jpeg".getBytes(StandardCharsets.UTF_8));
        ProbeSupport.check("undecodable source rejected",
                ProbeSupport.throwsIAE(() -> images.stage(corrupt, MATTE)));
        ProbeSupport.check("missing source rejected",
                ProbeSupport.throwsIAE(() -> images.stage(dir.resolve("gone.jpg"), MATTE)));
        ProbeSupport.check("a rejected import writes nothing", jpgCount(images.folder()) == base + 3);
    }

    private static void tolerantReads(ImageStore images, Path dir) throws IOException {
        ProbeSupport.check("no photo name is empty", images.load(null, 640).isEmpty());
        ProbeSupport.check("a missing photo is empty", images.load("e-999.jpg", 640).isEmpty());
        ProbeSupport.check("a traversal name is empty", images.load("../evil.jpg", 640).isEmpty());
        Files.write(images.folder().resolve("e-777.jpg"), new byte[] {1, 2, 3});
        ProbeSupport.check("a corrupt stored photo is empty",
                images.load("e-777.jpg", 640).isEmpty());
    }

    private static void promoteAndSweep(ImageStore images, Path dir) throws IOException {
        Path source = write(dir, "promote.png", 320, 180, TYPE_INT_RGB, Color.GREEN, "png");
        String staged = images.stage(source, MATTE);
        String name = images.promote(staged, "e-777");
        ProbeSupport.check("promote names the photo after the item", name.equals("e-777.jpg"));
        ProbeSupport.check("the promoted photo loads", images.load("e-777.jpg", 640).isPresent());
        ProbeSupport.check("promote consumes the staged file",
                !Files.exists(images.folder().resolve(staged)));
        ProbeSupport.check("promoting a consumed stage fails",
                ProbeSupport.throwsIAE(() -> images.promote(staged, "e-777")));
        ProbeSupport.check("an unsafe equipment id is rejected",
                ProbeSupport.throwsIAE(() -> images.promote("e-777.jpg", "../evil")));

        Files.write(images.folder().resolve("staging-1-1.jpg"), new byte[] {1});
        Files.write(images.folder().resolve("e-999.jpg"), new byte[] {1});
        Path notes = Files.writeString(images.folder().resolve("notes.txt"), "keep me");
        images.sweep(Set.of("e-777"));
        ProbeSupport.check("sweep keeps a live photo",
                Files.exists(images.folder().resolve("e-777.jpg")));
        ProbeSupport.check("sweep deletes an orphan photo",
                !Files.exists(images.folder().resolve("e-999.jpg")));
        ProbeSupport.check("sweep deletes staged files",
                !Files.exists(images.folder().resolve("staging-1-1.jpg")));
        ProbeSupport.check("sweep leaves unrelated files", Files.exists(notes));
        images.delete("e-777.jpg");
        ProbeSupport.check("delete removes one photo",
                !Files.exists(images.folder().resolve("e-777.jpg")));
    }

    private static Path write(Path dir, String name, int width, int height, int type,
                              Color fill, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, type);
        if (fill != null) {
            var g2 = image.createGraphics();
            try {
                g2.setColor(fill);
                g2.fillRect(0, 0, width, height);
            } finally {
                g2.dispose();
            }
        }
        Path file = dir.resolve(name);
        ImageIO.write(image, format, file.toFile());
        return file;
    }

    private static long jpgCount(Path folder) {
        if (!Files.isDirectory(folder)) {
            return 0;
        }
        try (var entries = Files.list(folder)) {
            return entries.filter(file -> file.getFileName().toString()
                    .endsWith(ImageRules.OUTPUT_SUFFIX)).count();
        } catch (IOException missing) {
            return 0;
        }
    }
}
