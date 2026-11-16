package kitnapper.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box.Filler;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
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
        JScrollPane pane = new JScrollPane(info,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        pane.getVerticalScrollBar().setUnitIncrement(16);
        return pane;
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
