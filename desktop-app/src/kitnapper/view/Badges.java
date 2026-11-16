package kitnapper.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.JPanel;

import kitnapper.model.EquipmentStatus;

/**
 * The §3.3 badge family: a 20 px amber-tinted pill, a 6 px semantic dot, and
 * the status word — status is never colour alone, and UNDER_MAINTENANCE adds a
 * wrench so it cannot be mistaken for the accent fill. Colours are read from
 * {@link Theme} at paint time, so a theme switch needs only a repaint.
 */
public final class Badges {

    static final int PILL_HEIGHT = 20;
    private static final int PAD = 8;
    private static final int DOT = 6;
    private static final int GAP = 6;
    private static final int GLYPH_GAP = 4;
    private static final int RADIUS = 10;

    private static final String WRENCH = Fonts.glyph("\uE90F", "\u2692");

    private Badges() { }

    /** Badge component for table cells and detail panes. */
    public static JComponent equipmentStatus(EquipmentStatus status) {
        return new Badge(status);
    }

    /** Paints the badge with its top-left corner at ({@code x}, {@code y}). */
    public static void paint(Graphics2D g, int x, int y, EquipmentStatus status) {
        paintPill(g, x, y, dotFor(status), glyphFor(status), wordOf(status));
    }

    /**
     * Paints one pill — dot, optional glyph, word — for any status family;
     * {@link StatusBadges} reuses the geometry for loans and penalties.
     * Returns the painted width.
     */
    static int paintPill(Graphics2D g, int x, int y, Color dot, String glyph, String word) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics wordMetrics = g2.getFontMetrics(wordFont());
        FontMetrics glyphMetrics = g2.getFontMetrics(glyphFont());
        int width = pillWidth(wordMetrics, glyphMetrics, glyph, word);
        g2.setColor(Theme.accentTint);
        g2.fill(new RoundRectangle2D.Double(x, y, width, PILL_HEIGHT, RADIUS, RADIUS));
        int cx = x + PAD;
        g2.setColor(dot);
        g2.fillOval(cx, y + (PILL_HEIGHT - DOT) / 2, DOT, DOT);
        cx += DOT + GAP;
        if (glyph != null) {
            g2.setFont(glyphFont());
            g2.setColor(Theme.textSecondary);
            FontMetrics glyphFont = g2.getFontMetrics();
            g2.drawString(glyph, cx,
                y + (PILL_HEIGHT - glyphFont.getHeight()) / 2 + glyphFont.getAscent());
            cx += glyphFont.stringWidth(glyph) + GLYPH_GAP;
        }
        g2.setFont(wordFont());
        g2.setColor(Theme.textPrimary);
        g2.drawString(word, cx,
            y + (PILL_HEIGHT - wordMetrics.getHeight()) / 2 + wordMetrics.getAscent());
        g2.dispose();
        return width;
    }

    /** Pill width for a dot, an optional glyph and a word; shared with {@link StatusBadges}. */
    static int pillWidth(FontMetrics wordMetrics, FontMetrics glyphMetrics, String glyph,
        String word) {
        int width = PAD * 2 + DOT + GAP + wordMetrics.stringWidth(word);
        if (glyph != null) {
            width += glyphMetrics.stringWidth(glyph) + GLYPH_GAP;
        }
        return width;
    }

    /** The §3.3 dot token for a status. */
    static Color dotFor(EquipmentStatus status) {
        return switch (status) {
            case AVAILABLE -> Theme.dotAvailable;
            case LOANED -> Theme.dotLoaned;
            case UNDER_MAINTENANCE -> Theme.dotMaintenance;
            case DECOMMISSIONED -> Theme.dotDecommissioned;
        };
    }

    /** Display word (§4: no all-caps styling — title case, spaces for underscores). */
    static String wordOf(EquipmentStatus status) {
        return switch (status) {
            case AVAILABLE -> "Available";
            case LOANED -> "Loaned";
            case UNDER_MAINTENANCE -> "Under maintenance";
            case DECOMMISSIONED -> "Decommissioned";
        };
    }

    private static Font wordFont() {
        return Fonts.text(12, Font.BOLD);
    }

    private static Font glyphFont() {
        return Fonts.icons(12);
    }

    /** The one status that adds a glyph beside the dot. */
    private static String glyphFor(EquipmentStatus status) {
        return status == EquipmentStatus.UNDER_MAINTENANCE ? WRENCH : null;
    }

    /** The reusable component form; paints itself and sizes to the pill. */
    private static final class Badge extends JPanel {

        private static final long serialVersionUID = 1L;

        private final EquipmentStatus status;

        Badge(EquipmentStatus status) {
            this.status = status;
            setOpaque(false);
            setFocusable(false);
            getAccessibleContext().setAccessibleName("Status " + wordOf(status));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(pillWidth(getFontMetrics(wordFont()),
                getFontMetrics(glyphFont()), glyphFor(status), wordOf(status)), PILL_HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Badges.paint((Graphics2D) g, 0, 0, status);
        }
    }
}
