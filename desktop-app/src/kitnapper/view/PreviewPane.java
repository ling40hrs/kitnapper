package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import kitnapper.model.Equipment;

/**
 * The §8.3 280 px preview pane: {@code BG_ELEVATED} with a 1 px left border,
 * 16:9 media ({@link PreviewMedia}), title, five metadata rows, and one pinned
 * primary action with a hint caption beneath. Borrowers see <b>Check out</b>
 * (enabled only when the item is borrowable); managers see the caption without
 * the button. Nothing selected shows the muted empty state instead.
 */
final class PreviewPane extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int WIDTH = 280;
    private static final int PAD = 16;
    private static final int MEDIA_WIDTH = WIDTH - PAD * 2 - 1;

    private final Consumer<Equipment> onCheckout;
    private final CardLayout states = new CardLayout();
    private final JPanel stack = new JPanel(states);
    private final PreviewMedia media;
    private final JPanel statusSlot = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JLabel title = new JLabel();
    private final JLabel categoryValue = valueLabel(false);
    private final JLabel serialValue = valueLabel(true);
    private final JLabel moneyValue = valueLabel(true);
    private final JLabel idValue = valueLabel(true);
    private final JButton checkout = UiFactory.primaryButton("Check out");
    private final JLabel hint = UiFactory.label("", "muted");
    private final JPanel info;
    private Equipment shown;
    private boolean compact;

    PreviewPane(Consumer<Equipment> onCheckout, PhotoSource photos) {
        super(new BorderLayout());
        this.onCheckout = onCheckout == null ? item -> { } : onCheckout;
        this.media = new PreviewMedia(MEDIA_WIDTH, photos);
        setOpaque(false);
        setPreferredSize(new Dimension(WIDTH, 0));
        setMinimumSize(new Dimension(WIDTH, 0));
        stack.setOpaque(false);
        statusSlot.setOpaque(false);
        info = infoPanel();
        stack.add(detail(), "detail");
        stack.add(new EmptyState(Fonts.glyph("\uE721", "\u2315"),
            "Select an item to see details.", null, null), "empty");
        add(stack, BorderLayout.CENTER);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                setCompact(PreviewLayout.compactFor(getHeight()));
            }
        });
        clear();
    }

    /** Tightens the metadata spacing below 560 px of preview height (D42). */
    private void setCompact(boolean next) {
        if (compact == next) {
            return;
        }
        compact = next;
        PreviewLayout.applyDensity(info, compact);
        revalidate();
    }

    /** Paint state for the current selection, as decided by {@link CatalogScreen}. */
    void show(Equipment item, boolean actionVisible, boolean actionEnabled, String hintText) {
        shown = item;
        media.show(item);
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
        hint.setText(hasHint ? PreviewLayout.captionLines(hintText, compact) : "");
        hint.setToolTipText(hasHint ? hintText : null);
        hint.getAccessibleContext().setAccessibleName(hasHint ? hintText : null);
        hint.setVisible(hasHint);
        statusSlot.revalidate();
        states.show(stack, "detail");
        revalidate();
        repaint();
    }

    /** Back to "Select an item to see details." */
    void clear() {
        shown = null;
        media.clear();
        states.show(stack, "empty");
        repaint();
    }

    void refreshTheme() {
        media.refreshTheme();
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
        detail.add(PreviewLayout.scroll(info), BorderLayout.CENTER);
        detail.add(actionBar(), BorderLayout.SOUTH);
        return detail;
    }

    private JPanel infoPanel() {
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        media.setOpaque(false);
        info.add(media);
        info.add(PreviewLayout.mediaGap());
        title.setFont(Fonts.display(16, Font.BOLD));
        title.putClientProperty(UiFactory.ROLE, "title");
        title.setForeground(Theme.textPrimary);
        title.setAlignmentX(0f);
        info.add(title);
        info.add(PreviewLayout.mediaGap());
        info.add(PreviewLayout.row("Status", statusSlot));
        info.add(PreviewLayout.row("Category", categoryValue));
        info.add(PreviewLayout.row("Serial", serialValue));
        info.add(PreviewLayout.row("Replacement value", moneyValue));
        info.add(PreviewLayout.row("ID", idValue));
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

    private static JLabel valueLabel(boolean mono) {
        JLabel label = new JLabel();
        label.setFont(mono ? Fonts.mono(14, Font.PLAIN) : Fonts.text(14, Font.PLAIN));
        label.putClientProperty(UiFactory.ROLE, "title");
        label.setForeground(Theme.textPrimary);
        return label;
    }
}
