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
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import kitnapper.model.DigitalEquipment;
import kitnapper.model.Equipment;
import kitnapper.model.MechanicalTool;

/**
 * The inventory page's 280 px preview (design §5 "Inventory"): 16:9 photo,
 * title, status badge, metadata including the kind's own field, and the state
 * actions from {@link InventoryStateActions}. Filled by {@link InventoryScreen}
 * on every selection and reload; nothing selected shows the muted empty state.
 */
final class InventoryPreviewPane extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int WIDTH = 280;
    private static final int PAD = 16;
    private static final int MEDIA_WIDTH = WIDTH - PAD * 2 - 1;

    private final CardLayout states = new CardLayout();
    private final JPanel stack = new JPanel(states);
    private final PreviewMedia media;
    private final JPanel statusSlot = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JLabel title = new JLabel();
    private final JLabel categoryValue = valueLabel(false);
    private final JLabel serialValue = valueLabel(true);
    private final JLabel moneyValue = valueLabel(true);
    private final JLabel idValue = valueLabel(true);
    private final JLabel extraLabel = UiFactory.label("", "secondary");
    private final JLabel extraValue = valueLabel(false);
    private final JPanel extraRow;
    private final InventoryStateActions stateActions;
    private JPanel info;
    private boolean compact;

    InventoryPreviewPane(PhotoSource photos, Consumer<Equipment> onEdit,
                         Consumer<Equipment> onFlag, Consumer<Equipment> onClear,
                         Consumer<Equipment> onRetire) {
        super(new BorderLayout());
        this.media = new PreviewMedia(MEDIA_WIDTH, photos);
        this.stateActions = new InventoryStateActions(onEdit, onFlag, onClear, onRetire);
        setOpaque(false);
        setPreferredSize(new Dimension(WIDTH, 0));
        setMinimumSize(new Dimension(WIDTH, 0));
        stack.setOpaque(false);
        statusSlot.setOpaque(false);
        extraRow = PreviewLayout.row(extraLabel, extraValue);
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

    /** Paints the selection; {@code onLoan} blocks the state actions. */
    void show(Equipment item, boolean onLoan) {
        media.show(item);
        title.setText(item.getName());
        title.setToolTipText(item.getName());
        statusSlot.removeAll();
        statusSlot.add(Badges.equipmentStatus(item.getStatus()));
        categoryValue.setText(Formats.category(item.getCategory()));
        serialValue.setText(item.getSerialNumber());
        moneyValue.setText(Formats.money(item.getReplacementValue()));
        idValue.setText(item.getId());
        showKindRow(item);
        stateActions.show(item, onLoan, InventoryStateActions.hintFor(item, onLoan));
        statusSlot.revalidate();
        states.show(stack, "detail");
        revalidate();
        repaint();
    }

    /** Back to "Select an item to see details." */
    void clear() {
        media.clear();
        stateActions.clear();
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

    /** D19: only the concrete kind's own field is shown. */
    private void showKindRow(Equipment item) {
        if (item instanceof DigitalEquipment digital) {
            extraLabel.setText("Warranty contact");
            extraValue.setText(digital.getWarrantyContact().orElse("—"));
            extraRow.setVisible(true);
        } else if (item instanceof MechanicalTool tool) {
            extraLabel.setText("Maintenance interval");
            extraValue.setText(tool.getMaintenanceIntervalDays() == 0
                ? "—" : tool.getMaintenanceIntervalDays() + " days");
            extraRow.setVisible(true);
        } else {
            extraRow.setVisible(false);
        }
    }

    private JPanel detail() {
        JPanel detail = new JPanel(new BorderLayout());
        detail.setOpaque(false);
        detail.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));
        detail.add(PreviewLayout.scroll(info), BorderLayout.CENTER);
        detail.add(stateActions, BorderLayout.SOUTH);
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
        info.add(PreviewLayout.row(UiFactory.label("Status", "secondary"), statusSlot));
        info.add(PreviewLayout.row(UiFactory.label("Category", "secondary"), categoryValue));
        info.add(PreviewLayout.row(UiFactory.label("Serial", "secondary"), serialValue));
        info.add(PreviewLayout.row(UiFactory.label("Replacement value", "secondary"),
            moneyValue));
        info.add(PreviewLayout.row(UiFactory.label("ID", "secondary"), idValue));
        info.add(extraRow);
        return info;
    }

    private static JLabel valueLabel(boolean mono) {
        JLabel label = new JLabel();
        label.setFont(mono ? Fonts.mono(14, Font.PLAIN) : Fonts.text(14, Font.PLAIN));
        label.putClientProperty(UiFactory.ROLE, "title");
        label.setForeground(Theme.textPrimary);
        return label;
    }
}
