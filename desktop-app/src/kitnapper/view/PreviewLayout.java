package kitnapper.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.Box.Filler;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Shared geometry for the four 280 px preview panes (design §5, decision D42
 * amended in M6): the label-above-value row factory, the adjustable gaps, the
 * compact density used below 560 px of preview height, the caption lines, and
 * the borderless scroll guard. The guard is what keeps every metadata row
 * reachable at the 1024×640 minimum — the measured shortfall there is 48 px
 * (catalog) to 174 px (inventory), far more than spacing can recover — while
 * the action bar stays pinned outside it. At 1280×800 and wider nothing
 * scrolls.
 */
final class PreviewLayout {

    static final int PAD = 16;
    static final int COMPACT_BELOW = 560;

    private static final int MEDIA_GAP = 12;
    private static final int MEDIA_GAP_COMPACT = 8;
    private static final int ROW_GAP = 10;
    private static final int ROW_GAP_COMPACT = 6;
    private static final int LABEL_GAP = 2;
    private static final int LABEL_GAP_COMPACT = 1;

    private PreviewLayout() { }

    /** Compact below 560 px of preview height; a zero size is not compact. */
    static boolean compactFor(int previewHeight) {
        return previewHeight > 0 && previewHeight < COMPACT_BELOW;
    }

    /** The 12 px media/title gap, tightened to 8 px when compact. */
    static JComponent mediaGap() {
        return new Gap(MEDIA_GAP, MEDIA_GAP_COMPACT);
    }

    /**
     * A 16:9 media slot that follows its real width (design §12). The info
     * column narrows when the pane grows a vertical scrollbar, so the height is
     * derived per layout rather than fixed; the slot is left-aligned and capped
     * so the {@code BoxLayout} column cannot stretch it.
     */
    static JPanel mediaSlot(int width) {
        JPanel slot = new JPanel(new MediaRatioLayout());
        slot.setOpaque(false);
        slot.setAlignmentX(0f);
        slot.setMaximumSize(new Dimension(width, MediaRatioLayout.heightFor(width)));
        return slot;
    }

    /** Any other spacing block: {@code compact} is its height when compact. */
    static JComponent gap(int normal, int compact) {
        return new Gap(normal, compact);
    }

    /** A metadata row: 12 px secondary label above its value. */
    static JPanel row(String labelText, JComponent value) {
        return row(UiFactory.label(labelText, "secondary"), value);
    }

    /** The same row with a caller-built label (the inventory kind caption). */
    static JPanel row(JComponent label, JComponent value) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        label.setAlignmentX(0f);
        value.setAlignmentX(0f);
        row.add(label);
        row.add(new Gap(LABEL_GAP, LABEL_GAP_COMPACT));
        row.add(value);
        row.add(new Gap(ROW_GAP, ROW_GAP_COMPACT));
        row.setAlignmentX(0f);
        return row;
    }

    /** Re-spaces every gap and row under {@code info} for the new density. */
    static void applyDensity(JComponent info, boolean compact) {
        apply(info, compact);
    }

    private static void apply(Component component, boolean compact) {
        if (component instanceof Gap gap) {
            gap.setCompact(compact);
        } else if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                apply(child, compact);
            }
        }
    }

    /** The rows scroll inside the pane only when the preview is too short. */
    static JScrollPane scroll(JComponent info) {
        return PreviewScroll.wrap(info);
    }

    /**
     * Two lines for the documented manager caption so its 248 px slot shows it
     * whole; one line when compact. The tooltip keeps the plain text.
     */
    static String captionLines(String caption, boolean compact) {
        int dash = caption.indexOf(" — ");
        if (compact || dash < 0) {
            return caption;
        }
        return "<html>" + caption.substring(0, dash + 2) + "<br>"
            + caption.substring(dash + 3) + "</html>";
    }

    /**
     * Greedy word wrap for a free-text value (the waive reason, FR-11 audit
     * trail) at {@code width} px over at most {@code maxLines} lines; text that
     * still does not fit ellipsises on the last line. Plain text is returned
     * unchanged so a short reason never grows an HTML line box.
     */
    static String wrapText(String text, int width, int maxLines) {
        if (text == null || text.isBlank()) {
            return "";
        }
        FontMetrics metrics = new JLabel().getFontMetrics(Fonts.text(14, Font.PLAIN));
        if (metrics.stringWidth(text) <= width) {
            return text;
        }
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        int used = 0;
        for (String word : text.trim().split("\\s+")) {
            int wordWidth = metrics.stringWidth(word + " ");
            if (used > 0 && used + wordWidth > width) {
                lines.add(line.toString().trim());
                line.setLength(0);
                used = 0;
            }
            line.append(word).append(' ');
            used += wordWidth;
        }
        lines.add(line.toString().trim());
        boolean truncated = lines.size() > maxLines;
        StringBuilder out = new StringBuilder("<html>");
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            if (i > 0) {
                out.append("<br>");
            }
            String value = lines.get(i);
            if (truncated && i == maxLines - 1) {
                value = ellipsise(metrics, value, width);
            }
            out.append(escape(value));
        }
        return out.toString();
    }

    private static String ellipsise(FontMetrics metrics, String text, int width) {
        String cut = text;
        while (!cut.isEmpty() && metrics.stringWidth(cut + "\u2026") > width) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "\u2026";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** A spacing block whose height follows the density (design §9 scale). */
    private static final class Gap extends Filler {

        private static final long serialVersionUID = 1L;

        private final int normal;
        private final int compact;

        Gap(int normal, int compact) {
            super(new Dimension(0, 0), new Dimension(0, normal),
                new Dimension(Integer.MAX_VALUE, normal));
            this.normal = normal;
            this.compact = compact;
            setAlignmentX(0f);
        }

        void setCompact(boolean state) {
            int height = state ? compact : normal;
            changeShape(new Dimension(0, 0), new Dimension(0, height),
                new Dimension(Integer.MAX_VALUE, height));
        }
    }
}
