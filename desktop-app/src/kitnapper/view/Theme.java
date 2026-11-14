package kitnapper.view;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;

import kitnapper.model.EquipmentCategory;

/**
 * Every color token in the app (design.md §3). Hex values live here and
 * nowhere else; panels read the public fields at paint time, so a theme
 * switch is one {@link #apply} call plus a repaint — no UIManager tree
 * walks in views.
 */
public final class Theme {

    /** Skeleton pulse switch (design §13.4); the Settings toggle lands in M5. */
    public static boolean animateSkeletons = true;

    private static boolean dark = true;
    private static final List<Runnable> LISTENERS = new ArrayList<>();

    // Window + surfaces (§3.1/§3.2)
    public static Color bgApp, bgSurface, bgElevated, bgControl, bgControlHover;
    // Structure
    public static Color borderSubtle, borderStrong, hoverRow, scrollThumb, scrim;
    // Text
    public static Color textPrimary, textSecondary, textMuted, textDisabled;
    // Accent + status
    public static Color accent, accentHover, accentPressed, onAccent, accentText, accentTint;
    public static Color danger, closeHover, skeletonBlock;
    // Badge dots (§3.3) — status is never color alone; Badges lands in M3
    public static Color dotAvailable, dotLoaned, dotOverdue, dotMaintenance, dotDecommissioned;

    private Theme() { }

    public static boolean isDark() {
        return dark;
    }

    /** Switches the token set, refreshes UIManager defaults, notifies listeners. */
    public static void apply(boolean toDark) {
        dark = toDark;
        if (dark) {
            applyDark();
        } else {
            applyLight();
        }
        installDefaults();
        for (Runnable listener : LISTENERS) {
            listener.run();
        }
    }

    public static void addThemeListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    private static void applyDark() {
        bgApp = hex(0x1F1F1F);
        bgSurface = hex(0x272727);
        bgElevated = hex(0x2D2D2D);
        bgControl = hex(0x323232);
        bgControlHover = hex(0x3A3A3A);
        borderSubtle = hex(0x383838);
        borderStrong = hex(0x4A4A4A);
        hoverRow = hex(0x2A2A2A);
        scrollThumb = hex(0x5A5A5A);
        scrim = tint(0, 0, 0, 0.40);
        textPrimary = hex(0xF5F5F5);
        textSecondary = hex(0xC2C2C2);
        textMuted = hex(0x9B9B9B);
        textDisabled = hex(0x6E6E6E);
        accent = hex(0xF0A32E);
        accentHover = hex(0xF5B455);
        accentPressed = hex(0xD88F22);
        onAccent = hex(0x1A1408);
        accentText = hex(0xF3B65B);
        accentTint = tint(240, 163, 46, 0.12);
        danger = hex(0xE0644A);
        closeHover = hex(0xC42B1C);
        skeletonBlock = hex(0x3A3A3A);
        dotAvailable = hex(0x8FBF6A);
        dotLoaned = hex(0x7EA6C8);
        dotOverdue = hex(0xE06A4E);
        dotMaintenance = hex(0xD9A441);
        dotDecommissioned = hex(0x8C8378);
    }

    private static void applyLight() {
        bgApp = hex(0xF3F3F3);
        bgSurface = hex(0xFBFBFB);
        bgElevated = hex(0xFFFFFF);
        bgControl = hex(0xFFFFFF);
        bgControlHover = hex(0xF5F5F5);
        borderSubtle = hex(0xE3E3E3);
        borderStrong = hex(0xD2D2D2);
        hoverRow = hex(0xF0F0F0);
        scrollThumb = hex(0xC6C6C6);
        scrim = tint(0, 0, 0, 0.40);
        textPrimary = hex(0x1B1B1B);
        textSecondary = hex(0x5A5A5A);
        textMuted = hex(0x767676);
        textDisabled = hex(0xA6A6A6);
        accent = hex(0xF0A32E);
        accentHover = hex(0xF5B455);
        accentPressed = hex(0xD88F22);
        onAccent = hex(0x1A1408);
        accentText = hex(0xA35F00);
        accentTint = tint(240, 163, 46, 0.16);
        danger = hex(0xB9432F);
        closeHover = hex(0xC42B1C);
        skeletonBlock = hex(0xE9E9E9);
        dotAvailable = hex(0x3E7A24);
        dotLoaned = hex(0x35679C);
        dotOverdue = hex(0xB4472F);
        dotMaintenance = hex(0x8F6A1E);
        dotDecommissioned = hex(0x6B6259);
    }

    /** The §3.5 fallback-tile pair — the single permitted gradient (GradientTile, M3). */
    public static Color[] tileGradient(EquipmentCategory category) {
        if (!dark) {
            return new Color[] { hex(0xEDE6D9), hex(0xF6F1E7) };
        }
        return switch (category) {
            case CAMERA -> new Color[] { hex(0x4E4636), hex(0x5E5544) };
            case LENS -> new Color[] { hex(0x37424A), hex(0x43505A) };
            case LAPTOP -> new Color[] { hex(0x453A45), hex(0x554855) };
            case AUDIO -> new Color[] { hex(0x4A3530), hex(0x5A413B) };
            case TOOL -> new Color[] { hex(0x3F4034), hex(0x4E5040) };
            case OTHER -> new Color[] { hex(0x3B3B3B), hex(0x484848) };
        };
    }

    /** Cross-platform LAF defaults so standard components match the tokens. */
    private static void installDefaults() {
        Font ui = Fonts.text(14, Font.PLAIN);
        UIManager.put("Panel.background", bgApp);
        UIManager.put("Label.foreground", textPrimary);
        UIManager.put("Label.font", ui);
        UIManager.put("TextField.background", bgControl);
        UIManager.put("TextField.foreground", textPrimary);
        UIManager.put("TextField.font", ui);
        UIManager.put("PasswordField.background", bgControl);
        UIManager.put("PasswordField.foreground", textPrimary);
        UIManager.put("PasswordField.font", ui);
        UIManager.put("Button.background", bgControl);
        UIManager.put("Button.foreground", textPrimary);
        UIManager.put("Button.font", ui);
        UIManager.put("Table.background", bgSurface);
        UIManager.put("Table.foreground", textPrimary);
        UIManager.put("Table.gridColor", borderSubtle);
        UIManager.put("ToolTip.background", bgElevated);
        UIManager.put("ToolTip.foreground", textPrimary);
        UIManager.put("OptionPane.background", bgApp);
        UIManager.put("OptionPane.messageForeground", textPrimary);
        UIManager.put("Dialog.background", bgApp);
    }

    private static Color hex(int rgb) {
        return new Color(rgb);
    }

    private static Color tint(int r, int g, int b, double alpha) {
        return new Color(r, g, b, (int) Math.round(alpha * 255));
    }
}
