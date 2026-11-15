package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import kitnapper.model.Equipment;

/**
 * The §8.3 280 px preview pane: {@code BG_ELEVATED} with a 1 px left border,
 * 16:9 fallback media, title, five metadata rows, and one pinned primary
 * action with a hint caption beneath. Borrowers see <b>Check out</b> (enabled
 * only when the item is borrowable); managers see the caption without the
 * button. Nothing selected shows the muted empty state instead.
 */
final class PreviewPane extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int WIDTH = 280;
    private static final int PAD = 16;
    private static final int MEDIA_WIDTH = WIDTH - PAD * 2 - 1;

    private final Consumer<Equipment> onCheckout;
    private final CardLayout states = new CardLayout();
    private final JPanel stack = new JPanel(states);
    private final JPanel mediaSlot = new JPanel(new BorderLayout());
    private final JPanel statusSlot = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JLabel title = new JLabel();
    private final JLabel categoryValue = valueLabel(false);
    private final JLabel serialValue = valueLabel(true);
    private final JLabel moneyValue = valueLabel(true);
    private final JLabel idValue = valueLabel(true);
    private final JButton checkout = UiFactory.primaryButton("Check out");
    private final JLabel hint = UiFactory.label("", "muted");
    private Equipment shown;

    PreviewPane(Consumer<Equipment> onCheckout) {
        super(new BorderLayout());
        this.onCheckout = onCheckout == null ? item -> { } : onCheckout;
        setOpaque(false);
        setPreferredSize(new Dimension(WIDTH, 0));
        setMinimumSize(new Dimension(WIDTH, 0));
        stack.setOpaque(false);
        statusSlot.setOpaque(false);
        stack.add(detail(), "detail");
        stack.add(new EmptyState(Fonts.glyph("\uE721", "\u2315"),
            "Select an item to see details.", null, null), "empty");
        add(stack, BorderLayout.CENTER);
        clear();
    }

    /** Paint state for the current selection, as decided by {@link CatalogScreen}. */
    void show(Equipment item, boolean actionVisible, boolean actionEnabled, String hintText) {
        shown = item;
        mediaSlot.removeAll();
        mediaSlot.add(new GradientTile(item.getCategory(), MEDIA_WIDTH), BorderLayout.CENTER);
        title.setText(item.getName());
        title.setToolTipText(item.getName());
        statusSlot.removeAll();
        statusSlot.add(Badges.equipmentStatus(item.getStatus()));
        categoryValue.setText(Formats.category(item.getCategory()));
        serialValue.setText(item.getSerialNumber());
        moneyValue.setText(Formats.money(item.getReplacementValue()));
        idValue.setText(item.getId());
        checkout.setVisible(actionVisible);
        checkout.setEnabled(actionEnabled);
        boolean hasHint = hintText != null && !hintText.isBlank();
        hint.setText(hasHint ? hintText : "");
        hint.setVisible(hasHint);
        mediaSlot.revalidate();
        statusSlot.revalidate();
        states.show(stack, "detail");
        revalidate();
        repaint();
    }

    /** Back to "Select an item to see details." */
    void clear() {
        shown = null;
        states.show(stack, "empty");
        repaint();
    }

    void refreshTheme() {
        UiFactory.refresh(this);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Theme.bgElevated);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(Theme.borderSubtle);
        g2.fillRect(0, 0, 1, getHeight());
        g2.dispose();
    }

    private JPanel detail() {
        JPanel detail = new JPanel(new BorderLayout());
        detail.setOpaque(false);
        detail.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));
        detail.add(info(), BorderLayout.NORTH);
        detail.add(actionBar(), BorderLayout.SOUTH);
        return detail;
    }

    private JPanel info() {
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        mediaSlot.setOpaque(false);
        info.add(mediaSlot);
        info.add(Box.createVerticalStrut(12));
        title.setFont(Fonts.display(16, Font.BOLD));
        title.putClientProperty(UiFactory.ROLE, "title");
        title.setForeground(Theme.textPrimary);
        title.setAlignmentX(0f);
        info.add(title);
        info.add(Box.createVerticalStrut(12));
        info.add(row("Status", statusSlot));
        info.add(row("Category", categoryValue));
        info.add(row("Serial", serialValue));
        info.add(row("Replacement value", moneyValue));
        info.add(row("ID", idValue));
        return info;
    }

    private JPanel actionBar() {
        JPanel bar = new JPanel();
        bar.setOpaque(false);
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        checkout.setAlignmentX(0f);
        checkout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        checkout.addActionListener(e -> {
            if (shown != null) {
                onCheckout.accept(shown);
            }
        });
        hint.setAlignmentX(0f);
        bar.add(checkout);
        bar.add(Box.createVerticalStrut(8));
        bar.add(hint);
        return bar;
    }

    private static JPanel row(String labelText, JComponent value) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        JLabel label = UiFactory.label(labelText, "secondary");
        label.setAlignmentX(0f);
        value.setAlignmentX(0f);
        row.add(label);
        row.add(Box.createVerticalStrut(2));
        row.add(value);
        row.add(Box.createVerticalStrut(10));
        row.setAlignmentX(0f);
        return row;
    }

    private static JLabel valueLabel(boolean mono) {
        JLabel label = new JLabel();
        label.setFont(mono ? Fonts.mono(14, Font.PLAIN) : Fonts.text(14, Font.PLAIN));
        label.putClientProperty(UiFactory.ROLE, "title");
        label.setForeground(Theme.textPrimary);
        return label;
    }
}
