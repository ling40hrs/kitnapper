package kitnapper.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import kitnapper.model.Equipment;
import kitnapper.model.EquipmentCategory;
import kitnapper.model.GeneralEquipment;

/**
 * Fixtures and EDT plumbing for the photo display probe (plan §9.3): sample
 * photos, photo sources with controlled timing and failure, painting, and the
 * "read on the EDT, wait off it" helpers the gate's timers need. Test-only.
 */
final class PhotoProbeSupport {

    static final int MAGENTA = 0xE040A0;

    private PhotoProbeSupport() {
    }

    static PhotoTile tile(String name, String photoFile, PhotoSource source) {
        return new PhotoTile(item(name, photoFile), 240, source, PhotoTile.CARD_EDGE_PX);
    }

    static Equipment item(String name, String photoFile) {
        Equipment item = new GeneralEquipment("e-" + name.toLowerCase(),
            name, EquipmentCategory.OTHER, "SN-" + name, 1);
        item.setPhotoFile(photoFile);
        return item;
    }

    static PhotoSource photoSource() {
        return (photoFile, maxEdge) -> Optional.of(photo());
    }

    static PhotoSource slowSource(int delayMs) {
        return (photoFile, maxEdge) -> {
            sleep(delayMs);
            return Optional.of(photo());
        };
    }

    static PhotoSource failingSource() {
        return (photoFile, maxEdge) -> {
            throw new IllegalStateException("decode failed");
        };
    }

    static BufferedImage photo() {
        BufferedImage image = new BufferedImage(240, 135, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(new Color(MAGENTA));
        g2.fillRect(0, 0, 240, 135);
        g2.dispose();
        return image;
    }

    static int magentaCount(Component component) {
        BufferedImage image = paintOf(component);
        int count = 0;
        for (int y = 0; y < image.getHeight(); y += 3) {
            for (int x = 0; x < image.getWidth(); x += 3) {
                if ((image.getRGB(x, y) & 0xFFFFFF) == MAGENTA) {
                    count++;
                }
            }
        }
        return count;
    }

    static BufferedImage paintOf(Component component) {
        int width = Math.max(1, component.getWidth());
        int height = Math.max(1, component.getHeight());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        component.paint(g2);
        g2.dispose();
        return image;
    }

    static <T extends Component> T find(Component root, Class<T> type) {
        return root instanceof Container container ? UiProbeSupport.find(container, type) : null;
    }

    static void waitFor(String label, long timeoutMs, Callable<Boolean> condition) {
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(onEdt(condition))) {
                UiProbeSupport.check(label, true);
                return;
            }
            sleep(10);
        }
        UiProbeSupport.check(label, false);
    }

    static <T> T onEdt(Callable<T> call) {
        if (SwingUtilities.isEventDispatchThread()) {
            return invoke(call);
        }
        AtomicReference<T> result = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> result.set(invoke(call)));
        } catch (Exception failure) {
            throw new IllegalStateException("EDT call failed", failure);
        }
        return result.get();
    }

    static void onEdtRun(Runnable action) {
        onEdt(() -> {
            action.run();
            return null;
        });
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static <T> T invoke(Callable<T> call) {
        try {
            return call.call();
        } catch (Exception failure) {
            throw new IllegalStateException("check failed", failure);
        }
    }
}
