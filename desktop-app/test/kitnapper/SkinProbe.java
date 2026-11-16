package kitnapper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import kitnapper.view.Fonts;
import kitnapper.view.Theme;

/**
 * Token surface probe (plan §6.4, §9.4): every token is present and the
 * required text/switch contrast holds in both themes; the badge dots stay
 * distinguishable. Kept thin by design (WP0.5 note): WP3 rewrites the theme
 * surface and this is one of its two guards. Prints the recorded
 * light-pixel count for the baseline table without asserting the number.
 * Runs headless.
 */
public final class SkinProbe {

    private static int passed;
    private static int failed;

    private SkinProbe() { }

    public static void main(String[] args) {
        Fonts.load();
        Theme.apply(true);
        check("tokens-dark", tokensPresent() && dotsDistinct());
        check("contrast-dark", contrastHolds());
        Theme.apply(false);
        check("tokens-light", tokensPresent() && dotsDistinct());
        check("contrast-light", contrastHolds());
        for (Map.Entry<String, Boolean> entry : ScrollBarChecks.results().entrySet()) {
            check(entry.getKey(), entry.getValue());
        }

        System.out.println("light_pixels=" + lightPixelCount() + " total=6400");
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static boolean tokensPresent() {
        Color[] tokens = {
            Theme.bgApp, Theme.bgSurface, Theme.bgElevated, Theme.bgControl,
            Theme.bgControlHover, Theme.borderSubtle, Theme.borderStrong,
            Theme.hoverRow, Theme.scrollThumb, Theme.scrim, Theme.textPrimary,
            Theme.textSecondary, Theme.textMuted, Theme.textDisabled,
            Theme.accent, Theme.accentHover, Theme.accentPressed, Theme.onAccent,
            Theme.accentText, Theme.accentTint, Theme.danger, Theme.closeHover,
            Theme.skeletonBlock, Theme.dotAvailable, Theme.dotLoaned,
            Theme.dotOverdue, Theme.dotMaintenance, Theme.dotDecommissioned
        };
        for (Color token : tokens) {
            if (token == null) {
                return false;
            }
        }
        return true;
    }

    private static boolean dotsDistinct() {
        Set<Integer> rgb = new HashSet<>(Arrays.asList(
            Theme.dotAvailable.getRGB(), Theme.dotLoaned.getRGB(),
            Theme.dotOverdue.getRGB(), Theme.dotMaintenance.getRGB(),
            Theme.dotDecommissioned.getRGB()));
        return rgb.size() == 5;
    }

    private static boolean contrastHolds() {
        return contrast(Theme.textPrimary, Theme.bgApp) >= 4.5
            && contrast(Theme.textSecondary, Theme.bgApp) >= 3.0
            && contrast(Theme.onAccent, Theme.accent) >= 3.0;
    }

    /** Renders the dark sample once and counts near-white pixels (recorded, not asserted). */
    private static int lightPixelCount() {
        Theme.apply(true);
        BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Theme.bgApp);
        g.fillRect(0, 0, 80, 80);
        g.setColor(Theme.accent);
        g.fillRect(0, 36, 80, 3);
        g.setColor(Theme.textPrimary);
        g.setFont(Fonts.text(12, java.awt.Font.BOLD));
        g.drawString("KitNapper", 10, 24);
        g.dispose();
        int light = 0;
        for (int y = 0; y < 80; y++) {
            for (int x = 0; x < 80; x++) {
                Color pixel = new Color(image.getRGB(x, y));
                if (pixel.getRed() >= 200 && pixel.getGreen() >= 200
                        && pixel.getBlue() >= 200) {
                    light++;
                }
            }
        }
        return light;
    }

    /** WCAG 2.x contrast ratio between two opaque colors. */
    private static double contrast(Color a, Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(Color c) {
        return 0.2126 * linear(c.getRed()) + 0.7152 * linear(c.getGreen())
            + 0.0722 * linear(c.getBlue());
    }

    private static double linear(int channel) {
        double v = channel / 255.0;
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
