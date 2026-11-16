package kitnapper.view;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * The §6 toggle switch: 40×20 track, checked = {@code ACCENT} with an
 * {@code ON_ACCENT} knob, unchecked = {@code BORDER_STRONG} with a
 * {@code TEXT_PRIMARY} knob. Click or Space toggles; keyboard focus paints the
 * 2 px focus rectangle. Session-only state lives in the screen's actions.
 */
final class ToggleSwitch extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int WIDTH = 40;
    private static final int HEIGHT = 20;
    private static final int KNOB = 14;

    private boolean on;
    private Consumer<Boolean> onChange = value -> { };

    ToggleSwitch(String accessibleName) {
        setOpaque(false);
        setFocusable(true);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMaximumSize(new Dimension(WIDTH, HEIGHT));
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        getAccessibleContext().setAccessibleName(accessibleName);
        getAccessibleContext().setAccessibleDescription(accessibleName);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                toggle();
            }
        });
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "toggle");
        getActionMap().put("toggle", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                toggle();
            }
        });
    }

    boolean isOn() {
        return on;
    }

    /** Sets the state without notifying, for reflecting saved state. */
    void setOn(boolean value) {
        if (on != value) {
            on = value;
            repaint();
        }
    }

    void setOnChange(Consumer<Boolean> listener) {
        onChange = listener == null ? value -> { } : listener;
    }

    /** Flips the switch and notifies, the same path a click or Space takes. */
    void toggle() {
        on = !on;
        repaint();
        onChange.accept(on);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(on ? Theme.accent : Theme.borderStrong);
        g2.fill(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, HEIGHT, HEIGHT));
        double knobX = on ? WIDTH - KNOB - 3 : 3;
        g2.setColor(on ? Theme.onAccent : Theme.textPrimary);
        g2.fill(new Ellipse2D.Double(knobX, (HEIGHT - KNOB) / 2.0, KNOB, KNOB));
        if (isFocusOwner()) {
            g2.setColor(Theme.accent);
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, WIDTH - 1, HEIGHT - 1,
                HEIGHT, HEIGHT));
        }
        g2.dispose();
    }
}
