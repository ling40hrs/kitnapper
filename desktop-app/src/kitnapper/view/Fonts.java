package kitnapper.view;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Font stack resolution, run once at startup (design.md §4, §10):
 * Segoe UI Variable Display → Segoe UI Variable Text → Segoe UI → bundled
 * Inter → SansSerif, and Segoe Fluent Icons → Segoe MDL2 Assets for glyphs.
 * System fonts are probed, never bundled; Inter is the cross-machine
 * fallback loaded from the app folder's {@code resources/fonts/}.
 */
public final class Fonts {

    private static final String[] INTER_FILES = {
        "Inter-Regular.ttf", "Inter-SemiBold.ttf", "Inter-Bold.ttf"
    };

    private static String displayFamily = "SansSerif";
    private static String textFamily = "SansSerif";
    private static String monoFamily = "Monospaced";
    private static String iconFamily;

    private Fonts() { }

    /** Probes installed families and registers the bundled Inter fallback. */
    public static void load() {
        registerInter();
        Set<String> installed = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        if (installed.contains("Inter")) {
            textFamily = "Inter";
        }
        if (installed.contains("Segoe UI Variable Display")) {
            displayFamily = "Segoe UI Variable Display";
        } else if (installed.contains("Segoe UI")) {
            displayFamily = "Segoe UI";
        }
        if (installed.contains("Segoe UI Variable Text")) {
            textFamily = "Segoe UI Variable Text";
        } else if (installed.contains("Segoe UI")) {
            textFamily = "Segoe UI";
        }
        if (installed.contains("Consolas")) {
            monoFamily = "Consolas";
        }
        if (installed.contains("Segoe Fluent Icons")) {
            iconFamily = "Segoe Fluent Icons";
        } else if (installed.contains("Segoe MDL2 Assets")) {
            iconFamily = "Segoe MDL2 Assets";
        }
    }

    /** Titles and KPI numbers (§4: display is reserved for ≥ 20 px). */
    public static Font display(int size, int style) {
        return new Font(displayFamily, style, size);
    }

    public static Font text(int size, int style) {
        return new Font(textFamily, style, size);
    }

    /** Monospace for IDs, serial numbers, and money in tables (§4). */
    public static Font mono(int size, int style) {
        return new Font(monoFamily, style, size);
    }

    public static Font icons(int size) {
        return new Font(iconFamily != null ? iconFamily : textFamily, Font.PLAIN, size);
    }

    /**
     * Fluent codepoint when the icon font can render it, else the §10
     * Unicode fallback column.
     */
    public static String glyph(String fluentCodepoint, String fallback) {
        if (iconFamily != null
            && new Font(iconFamily, Font.PLAIN, 12).canDisplay(fluentCodepoint.charAt(0))) {
            return fluentCodepoint;
        }
        return fallback;
    }

    private static void registerInter() {
        for (String file : INTER_FILES) {
            try (InputStream in = Fonts.class.getResourceAsStream("/fonts/" + file)) {
                if (in != null) {
                    GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .registerFont(Font.createFont(Font.TRUETYPE_FONT, in));
                    continue;
                }
                File local = new File("resources/fonts/" + file);
                if (local.isFile()) {
                    GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .registerFont(Font.createFont(Font.TRUETYPE_FONT, local));
                }
            } catch (Exception ignored) {
                // Missing Inter is fine: the Segoe chain or SansSerif takes over.
            }
        }
    }
}
