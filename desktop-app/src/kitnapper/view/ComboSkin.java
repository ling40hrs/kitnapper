package kitnapper.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.metal.MetalComboBoxButton;
import javax.swing.plaf.metal.MetalComboBoxUI;

/**
 * Combo skin for the filter bar and the date pickers (design §6/§8). Metal
 * paints its own {@code getFocusColor()} fill across a focused combo's value,
 * which turns the field a solid LAF blue and drops the focus underline every
 * text field shows; this UI paints the control token instead and adds the same
 * 2 px accent underline used by {@link FieldSkin}. The dropdown chevron is
 * painted here too (D52): Metal draws its arrow in
 * {@code MetalLookAndFeel.getControlInfo()}, which is a single shade away from
 * {@code BG_CONTROL}, so on the dark theme only the LAF button border was left
 * showing as an empty square. Everything else — the popup list and the
 * renderer — stays Metal's.
 */
final class ComboSkin extends MetalComboBoxUI {

    private static final int RADIUS = 4;
    private static final int CHEVRON_WIDTH = 10;
    private static final int CHEVRON_HEIGHT = 6;

    private ComboSkin() {
    }

    /** Installs the skin; call before the caller sets font, colours or border. */
    static void install(JComboBox<?> combo) {
        combo.setUI(new ComboSkin());
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        styleArrowButton();
    }

    /**
     * Keeps Metal's arrow button (it owns the popup click) but swaps its icon
     * for the themed chevron and drops the LAF border that framed it.
     */
    private void styleArrowButton() {
        if (!(arrowButton instanceof MetalComboBoxButton button)) {
            return;
        }
        button.setBorder(new EmptyBorder(0, 0, 0, 0));
        button.setContentAreaFilled(false);
        button.setFocusable(false);
        button.setOpaque(false);
        button.setComboIcon(new Chevron());
    }

    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        Color old = g.getColor();
        g.setColor(Theme.bgControl);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setColor(old);
    }

    /**
     * Paints the value as if the combo were unfocused. Basic hands the renderer
     * the list selection colours when the combo has focus, which is where the
     * LAF blue fill and white text came from.
     */
    @Override
    public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
        super.paintCurrentValue(g, bounds, false);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
        if (!(c instanceof JComboBox<?> combo) || !combo.hasFocus() || !combo.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.accent);
        g2.fillRoundRect(1, c.getHeight() - 4, c.getWidth() - 2, 2, RADIUS, RADIUS);
        g2.dispose();
    }

    /**
     * The §6 chevron, drawn in {@code TEXT_SECONDARY} at paint time so a theme
     * switch needs no new icon. {@code TEXT_MUTED} would fade into the control
     * fill at the 32 px size the combo uses.
     */
    private static final class Chevron implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c != null && !c.isEnabled() ? Theme.textDisabled : Theme.textSecondary);
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
            Path2D glyph = new Path2D.Float();
            glyph.moveTo(x + 0.5, y + 1.5);
            glyph.lineTo(x + CHEVRON_WIDTH / 2f, y + CHEVRON_HEIGHT - 1.5);
            glyph.lineTo(x + CHEVRON_WIDTH - 0.5, y + 1.5);
            g2.draw(glyph);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return CHEVRON_WIDTH;
        }

        @Override
        public int getIconHeight() {
            return CHEVRON_HEIGHT;
        }
    }
}
