package kitnapper.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;

import kitnapper.model.EquipmentCategory;

/**
 * The §3.4/§3.5 fallback media tile — the only permitted gradient in the app:
 * one 2-stop same-hue pair from {@link Theme#tileGradient}, with the category
 * glyph at 48 px. M3 renders it for every card and preview because seed items
 * carry no photos (D11/D27); {@code PhotoTile} falls back to it from M5.
 */
final class GradientTile extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int RADIUS = 8;
    private static final int GLYPH_SIZE = 48;

    private final EquipmentCategory category;

    /** A 16:9 tile {@code width} px wide; height follows the aspect ratio. */
    GradientTile(EquipmentCategory category, int width) {
        this.category = category;
        setOpaque(false);
        setPreferredSize(new Dimension(width, Math.max(1, Math.round(width * 9f / 16f))));
        getAccessibleContext().setAccessibleName("No photo · " + category);
    }

    /** The §10 fallback glyph column, authoritative when a codepoint renders wrong. */
    static String glyphFor(EquipmentCategory category) {
        return switch (category) {
            case CAMERA -> "\u25A3";
            case LENS -> "\u25CE";
            case LAPTOP -> "\u25AD";
            case AUDIO -> "\u266A";
            case TOOL -> "\u2692";
            case OTHER -> "\u25A6";
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color[] stops = Theme.tileGradient(category);
        RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(),
            RADIUS, RADIUS);
        g2.setClip(shape);
        g2.setPaint(new GradientPaint(0, 0, stops[0], 0, getHeight(), stops[1]));
        g2.fill(shape);
        g2.setFont(Fonts.symbol(GLYPH_SIZE));
        g2.setColor(veil(Theme.textPrimary, Theme.isDark() ? 0.55f : 0.45f));
        String glyph = glyphFor(category);
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(glyph, (getWidth() - metrics.stringWidth(glyph)) / 2,
            (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
        g2.dispose();
    }

    /** Same RGB, new alpha — keeps the tint policy out of hex literals. */
    private static Color veil(Color base, float alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(),
            Math.round(alpha * 255));
    }
}
