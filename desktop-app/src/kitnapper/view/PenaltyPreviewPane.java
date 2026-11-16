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

import kitnapper.model.PenaltyStatus;

/**
 * The §8.6 penalty preview: the frozen amount, the loan it belongs to, the
 * settlement badge and the waive reason when one was recorded. While the
 * penalty is UNPAID it pins {@code Mark paid} (primary) and {@code Waive…}
 * (secondary); settled penalties show no actions (REQ-011). Nothing selected
 * shows the muted empty state instead.
 */
final class PenaltyPreviewPane extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int WIDTH = 280;
    private static final int PAD = 16;

    private final Consumer<PenaltyRow> onMarkPaid;
    private final Consumer<PenaltyRow> onWaive;
    private final CardLayout states = new CardLayout();
    private final JPanel stack = new JPanel(states);
    private final JPanel statusSlot = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JLabel amountValue = new JLabel();
    private final JLabel loanValue = valueLabel(true);
    private final JLabel itemValue = valueLabel(false);
    private final JLabel borrowerValue = valueLabel(false);
    private final JLabel assessedValue = valueLabel(true);
    private final JLabel reasonValue = valueLabel(false);
    private final JPanel reasonRow;
    private final JButton markPaid = UiFactory.primaryButton("Mark paid");
    private final JButton waive = UiFactory.secondaryButton("Waive\u2026");
    private JPanel info;
    private PenaltyRow shown;
    private boolean compact;

    PenaltyPreviewPane(Consumer<PenaltyRow> onMarkPaid, Consumer<PenaltyRow> onWaive) {
        super(new BorderLayout());
        this.onMarkPaid = onMarkPaid == null ? row -> { } : onMarkPaid;
        this.onWaive = onWaive == null ? row -> { } : onWaive;
        setOpaque(false);
        setPreferredSize(new Dimension(WIDTH, 0));
        setMinimumSize(new Dimension(WIDTH, 0));
        stack.setOpaque(false);
        statusSlot.setOpaque(false);
        reasonRow = PreviewLayout.row("Waive reason", reasonValue);
        reasonRow.setVisible(false);
        info = infoPanel();
        stack.add(detail(), "detail");
        stack.add(new EmptyState(Fonts.glyph("\uE721", "\u2315"),
            "Select a penalty to see details.", null, null), "empty");
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

    /** Paint state for the selection; actions only while UNPAID (§8.6). */
    void show(PenaltyRow row, boolean actionsVisible) {
        shown = row;
        amountValue.setText(Formats.money(row.amount()));
        statusSlot.removeAll();
        statusSlot.add(StatusBadges.penaltyStatus(row.status()));
        loanValue.setText(row.loanId());
        itemValue.setText(row.itemName());
        borrowerValue.setText(row.borrowerName());
        assessedValue.setText(Formats.date(row.assessedDate()));
        String reason = row.waiveReason();
        reasonValue.setText(reason == null ? "" : PreviewLayout.wrapText(reason, 248, 3));
        reasonValue.setToolTipText(reason);
        reasonRow.setVisible(reason != null);
        boolean unpaid = row.status() == PenaltyStatus.UNPAID;
        markPaid.setVisible(actionsVisible && unpaid);
        waive.setVisible(actionsVisible && unpaid);
        statusSlot.revalidate();
        states.show(stack, "detail");
        revalidate();
        repaint();
    }

    /** Back to "Select a penalty to see details." */
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
        detail.add(PreviewLayout.scroll(info), BorderLayout.CENTER);
        detail.add(actionBar(), BorderLayout.SOUTH);
        return detail;
    }

    private JPanel infoPanel() {
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel caption = UiFactory.label("Late fee", "secondary");
        caption.setAlignmentX(0f);
        amountValue.setFont(Fonts.mono(28, Font.BOLD));
        amountValue.putClientProperty(UiFactory.ROLE, "title");
        amountValue.setAlignmentX(0f);
        info.add(caption);
        info.add(PreviewLayout.gap(4, 3));
        info.add(amountValue);
        info.add(PreviewLayout.gap(16, 10));
        info.add(PreviewLayout.row("Status", statusSlot));
        info.add(PreviewLayout.row("Loan", loanValue));
        info.add(PreviewLayout.row("Item", itemValue));
        info.add(PreviewLayout.row("Borrower", borrowerValue));
        info.add(PreviewLayout.row("Assessed", assessedValue));
        info.add(reasonRow);
        return info;
    }

    private JPanel actionBar() {
        JPanel bar = new JPanel();
        bar.setOpaque(false);
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        for (JButton button : new JButton[] { markPaid, waive }) {
            button.setAlignmentX(0f);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            bar.add(button);
            bar.add(Box.createVerticalStrut(8));
        }
        markPaid.addActionListener(e -> notify(onMarkPaid));
        waive.addActionListener(e -> notify(onWaive));
        return bar;
    }

    private void notify(Consumer<PenaltyRow> action) {
        if (shown != null) {
            action.accept(shown);
        }
    }

    private static JLabel valueLabel(boolean mono) {
        JLabel label = new JLabel();
        label.setFont(mono ? Fonts.mono(14, Font.PLAIN) : Fonts.text(14, Font.PLAIN));
        label.putClientProperty(UiFactory.ROLE, "title");
        label.setForeground(Theme.textPrimary);
        return label;
    }
}
