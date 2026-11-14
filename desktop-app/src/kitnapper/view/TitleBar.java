package kitnapper.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JButton;

/**
 * 32 px custom title bar (design.md §5): app glyph + wordmark, screen
 * title, theme toggle, and min/max/close at 46×32 hit targets. Everything
 * except the buttons is the drag region (registered by the frame's
 * WindowChrome); double-click maximizes via that same region.
 */
public final class TitleBar extends javax.swing.JPanel {

    private static final long serialVersionUID = 1L;
    private static final int HEIGHT = 32;
    private static final int CONTROL_WIDTH = 46;

    private final WindowChrome chrome;
    private final ChromeButton themeButton;
    private final ChromeButton maximizeButton;
    private String screenTitle = "";

    /** @param chrome the frame's chrome; maximized-state glyphs track it */
    public TitleBar(WindowChrome chrome, Runnable onThemeToggle) {
        this.chrome = chrome;
        setOpaque(false);
        setPreferredSize(new Dimension(0, HEIGHT));
        if (chrome != null) {
            chrome.attachDragRegion(this);
        }
        themeButton = new ChromeButton(32, "Theme");
        themeButton.addActionListener(e -> {
            if (onThemeToggle != null) {
                onThemeToggle.run();
            }
        });
        ChromeButton minimize = new ChromeButton(CONTROL_WIDTH, "Minimize");
        ChromeButton maximize = new ChromeButton(CONTROL_WIDTH, "Maximize");
        ChromeButton close = new ChromeButton(CONTROL_WIDTH, "Close");
        maximizeButton = maximize;
        if (chrome != null) {
            minimize.addActionListener(e -> chrome.iconify());
            maximize.addActionListener(e -> chrome.toggleMaximize());
            close.addActionListener(e -> chrome.close());
            chrome.onMaximizeChange(() -> setMaximized(chrome.isMaximized()));
        }
        java.awt.GridLayout rightLayout = new java.awt.GridLayout(1, 4, 0, 0);
        javax.swing.JPanel right = new javax.swing.JPanel(rightLayout);
        right.setOpaque(false);
        right.add(themeButton);
        right.add(minimize);
        right.add(maximize);
        right.add(close);
        setLayout(new java.awt.BorderLayout());
        add(right, java.awt.BorderLayout.EAST);
    }

    /** Screen name shown right of the wordmark (secondary, 12 px). */
    public void setScreenTitle(String title) {
        this.screenTitle = title == null ? "" : title;
        repaint();
    }

    /** Swaps the maximize/restore glyph after a window state change. */
    public void setMaximized(boolean maximized) {
        maximizeButton.putClientProperty("restore", maximized);
        maximizeButton.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.bgApp);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(Theme.borderSubtle);
        g2.fillRect(0, getHeight() - 1, getWidth(), 1);
        g2.setFont(Fonts.icons(14));
        g2.setColor(Theme.accent);
        String appGlyph = Fonts.glyph("\uE80F", "▣");
        g2.drawString(appGlyph, 12, 21);
        int glyphWidth = g2.getFontMetrics().stringWidth(appGlyph);
        g2.setFont(Fonts.text(13, Font.BOLD));
        g2.setColor(Theme.textPrimary);
        FontMetrics metrics = g2.getFontMetrics();
        int x = 12 + glyphWidth + 6;
        g2.drawString("KitNapper", x, 21);
        if (!screenTitle.isEmpty()) {
            g2.setFont(Fonts.text(12, Font.PLAIN));
            g2.setColor(Theme.textSecondary);
            g2.drawString(screenTitle, x + metrics.stringWidth("KitNapper") + 12, 21);
        }
        g2.dispose();
        super.paintComponent(g);
    }

    /** Window-control button: flat hover fills, glyph-only content. */
    private static final class ChromeButton extends JButton {

        private static final long serialVersionUID = 1L;
        private final boolean isClose;

        ChromeButton(int width, String accessibleName) {
            setPreferredSize(new Dimension(width, HEIGHT));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            getAccessibleContext().setAccessibleName(accessibleName);
            isClose = "Close".equals(accessibleName);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            if (getModel().isRollover()) {
                g2.setColor(isClose ? Theme.closeHover : Theme.bgControl);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            boolean restore = Boolean.TRUE.equals(getClientProperty("restore"));
            String glyph = switch (getAccessibleContext().getAccessibleName()) {
                case "Minimize" -> Fonts.glyph("\uE921", "─");
                case "Maximize" -> restore ? Fonts.glyph("\uE923", "❐") : Fonts.glyph("\uE922", "▢");
                case "Close" -> Fonts.glyph("\uE711", "✕");
                default -> themeGlyph();
            };
            g2.setFont(Fonts.icons(12));
            g2.setColor(isClose && getModel().isRollover() ? Color.WHITE : Theme.textSecondary);
            FontMetrics metrics = g2.getFontMetrics();
            int x = (getWidth() - metrics.stringWidth(glyph)) / 2;
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(glyph, x, y);
            if (isFocusOwner()) {
                g2.setColor(Theme.accent);
                g2.setStroke(new java.awt.BasicStroke(2));
                g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
            }
            g2.dispose();
        }

        private String themeGlyph() {
            boolean dark = Theme.isDark();
            return dark ? Fonts.glyph("\uE706", "☀") : Fonts.glyph("\uE793", "☾");
        }
    }
}
