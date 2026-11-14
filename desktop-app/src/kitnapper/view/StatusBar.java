package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 32 px status bar (design.md §5): left = item/loan counts, then selection
 * summary; right = monospace aggregate. Content is provisional in M2;
 * M3+ screens own the real counts.
 */
public final class StatusBar extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int HEIGHT = 32;

    private final JLabel left = UiFactory.label("", "secondary");
    private final JLabel right = UiFactory.label("", "secondary");

    public StatusBar() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 0));
        setPreferredSize(new Dimension(0, HEIGHT));
        setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        right.setFont(Fonts.mono(12, java.awt.Font.PLAIN));
        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
    }

    public void setLeft(String text) {
        left.setText(text == null ? "" : text);
    }

    public void setRight(String text) {
        right.setText(text == null ? "" : text);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Theme.bgApp);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Theme.borderSubtle);
        g.fillRect(0, 0, getWidth(), 1);
    }
}
