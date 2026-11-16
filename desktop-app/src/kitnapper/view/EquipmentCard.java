package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import kitnapper.model.Equipment;

/**
 * One §8.4 catalog card: 8 px radius {@code BG_SURFACE}, 1 px
 * {@code BORDER_SUBTLE} (raised to {@code BORDER_STRONG} on hover, {@code ACCENT}
 * plus {@code ACCENT_TINT} when selected), 16:9 media ({@link PhotoTile} — the
 * photo, or the category fallback), then name, category, badge and monospace
 * value. No shadow and no gradient beyond the media tile. Clicking reports the
 * item to {@link CatalogCards}.
 */
final class EquipmentCard extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int RADIUS = 8;
    private static final int PAD = 12;
    private static final int MEDIA_WIDTH = 200;
    private static final int NAME_LIMIT = 26;

    private final Equipment item;
    private final Consumer<Equipment> onClick;
    private boolean selected;
    private boolean hovered;

    EquipmentCard(Equipment item, Consumer<Equipment> onClick, PhotoSource photos) {
        super(new BorderLayout(0, 8));
        this.item = item;
        this.onClick = onClick == null ? ignored -> { } : onClick;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(new PhotoTile(item, MEDIA_WIDTH, photos, PhotoTile.CARD_EDGE_PX), BorderLayout.NORTH);
        add(body(), BorderLayout.CENTER);
        setToolTipText(item.getName());
        getAccessibleContext().setAccessibleName(
            "Item " + item.getName() + ", " + Badges.wordOf(item.getStatus()));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                EquipmentCard.this.onClick.accept(EquipmentCard.this.item);
            }
        });
    }

    Equipment item() {
        return item;
    }

    void setSelected(boolean value) {
        if (selected != value) {
            selected = value;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, getWidth() - 1,
            getHeight() - 1, RADIUS, RADIUS);
        g2.setColor(Theme.bgSurface);
        g2.fill(shape);
        if (selected) {
            g2.setColor(Theme.accentTint);
            g2.fill(shape);
        }
        g2.setColor(selected ? Theme.accent : hovered ? Theme.borderStrong : Theme.borderSubtle);
        g2.draw(shape);
        g2.dispose();
    }

    private JPanel body() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(text(nameText(), 14, Font.BOLD, "title"));
        body.add(Box.createVerticalStrut(2));
        body.add(text(Formats.category(item.getCategory()), 12, Font.PLAIN, "secondary"));
        body.add(Box.createVerticalStrut(8));
        body.add(footer());
        return body;
    }

    private JPanel footer() {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(Badges.equipmentStatus(item.getStatus()), BorderLayout.WEST);
        JLabel value = new JLabel(Formats.money(item.getReplacementValue()));
        value.setFont(Fonts.mono(12, Font.PLAIN));
        value.putClientProperty(UiFactory.ROLE, "title");
        value.setForeground(Theme.textPrimary);
        row.add(value, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    /** Ellipsized name; the full text stays in the tooltip and accessible name. */
    private String nameText() {
        String name = item.getName();
        return name.length() <= NAME_LIMIT ? name : name.substring(0, NAME_LIMIT - 1) + "\u2026";
    }

    private static JLabel text(String value, int size, int style, String role) {
        JLabel label = new JLabel(value);
        label.setFont(Fonts.text(size, style));
        label.putClientProperty(UiFactory.ROLE, role);
        label.setForeground("secondary".equals(role) ? Theme.textSecondary : Theme.textPrimary);
        label.setAlignmentX(0f);
        return label;
    }
}
