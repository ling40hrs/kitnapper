package kitnapper.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.plaf.metal.MetalComboBoxUI;

/**
 * Combo skin for the filter bar and the date pickers (design §6/§8). Metal
 * paints its own {@code getFocusColor()} fill across a focused combo's value,
 * which turns the field a solid LAF blue and drops the focus underline every
 * text field shows; this UI paints the control token instead and adds the same
 * 2 px accent underline used by {@link FieldSkin}. Everything else — the arrow
 * button, the popup list, the renderer — stays Metal's.
 */
final class ComboSkin extends MetalComboBoxUI {

    private static final int RADIUS = 4;

    private ComboSkin() {
    }

    /** Installs the skin; call before the caller sets font, colours or border. */
    static void install(JComboBox<?> combo) {
        combo.setUI(new ComboSkin());
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
}
