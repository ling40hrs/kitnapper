package kitnapper.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * The blank-image primitives {@link ImageStoreImpl} composes: bilinear
 * downscale, alpha flattening onto an opaque matte (D34), JPEG q0.85
 * encoding, and the atomic tmp-file move the save file also uses
 * (design §12). Split out so the store stays under the 200-line cap.
 */
final class PhotoCodec {

    private PhotoCodec() {
    }

    /** Scales down only when the longest edge exceeds {@code maxEdge}. */
    static BufferedImage downscale(BufferedImage source, int maxEdge) {
        int longest = Math.max(source.getWidth(), source.getHeight());
        if (maxEdge <= 0 || longest <= maxEdge) {
            return source;
        }
        double factor = (double) maxEdge / longest;
        int width = Math.max(1, (int) Math.round(source.getWidth() * factor));
        int height = Math.max(1, (int) Math.round(source.getHeight() * factor));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(source, 0, 0, width, height, null);
        } finally {
            g2.dispose();
        }
        return scaled;
    }

    /** Draws the image onto an opaque surface so JPEG has no alpha channel. */
    static BufferedImage flatten(BufferedImage source, int matteRgb) {
        BufferedImage opaque = new BufferedImage(source.getWidth(), source.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = opaque.createGraphics();
        try {
            g2.setColor(new Color(matteRgb));
            g2.fillRect(0, 0, source.getWidth(), source.getHeight());
            g2.drawImage(source, 0, 0, null);
        } finally {
            g2.dispose();
        }
        return opaque;
    }

    /** Writes {@code image} as JPEG into {@code target} via tmp file + move. */
    static void writeJpeg(BufferedImage image, Path target) {
        Path folder = target.getParent();
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try {
            Files.createDirectories(folder);
            try (OutputStream out = Files.newOutputStream(tmp);
                 ImageOutputStream imageOut = ImageIO.createImageOutputStream(out)) {
                ImageWriteParam params = writer.getDefaultWriteParam();
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(ImageRules.JPEG_QUALITY);
                writer.setOutput(imageOut);
                writer.write(null, new IIOImage(image, null, null), params);
            }
            move(tmp, target);
        } catch (IOException failure) {
            deleteQuietly(tmp);
            throw new UncheckedIOException("Could not write " + target, failure);
        } finally {
            writer.dispose();
        }
    }

    /** Atomic move with the Windows replace fallback (same as the save file). */
    static void move(Path from, Path to) throws IOException {
        Files.createDirectories(to.getParent());
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException notAtomic) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Deletes without surfacing an error; callers are cleanup paths only. */
    static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Best effort: a leftover file is removed by the next sweep.
        }
    }
}
