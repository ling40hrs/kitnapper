package kitnapper.view;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The §8.2 fee block: "Accrued now" while the loan is out, the frozen penalty
 * with its settlement word once returned, and the waive reason beneath when
 * one was recorded. Both figures come from the controller (D21) — this block
 * only formats them.
 */
final class LoanFeeBlock extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel caption = UiFactory.label("Accrued now", "secondary");
    private final JLabel amount = new JLabel();
    private final JLabel detail = UiFactory.label("", "muted");

    LoanFeeBlock() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(0f);
        caption.setAlignmentX(0f);
        amount.setFont(Fonts.mono(18, java.awt.Font.BOLD));
        amount.putClientProperty(UiFactory.ROLE, "title");
        amount.setAlignmentX(0f);
        detail.setAlignmentX(0f);
        add(caption);
        add(Box.createVerticalStrut(4));
        add(amount);
        add(Box.createVerticalStrut(2));
        add(detail);
    }

    /** Repaints the block for this row's accrual state. */
    void show(LoanRow row) {
        PenaltyRow penalty = row.penalty();
        if (row.returnDate() == null) {
            caption.setText("Accrued now");
            amount.setText(Formats.money(row.accruedFee()));
            detail.setText("Live until the loan is returned.");
            detail.setVisible(true);
        } else {
            caption.setText("Frozen penalty");
            amount.setText(penalty == null ? Formats.money(0)
                : Formats.money(penalty.amount()) + " \u00B7 "
                    + StatusBadges.penaltyWord(penalty.status()));
            String reason = penalty == null ? null : penalty.waiveReason();
            detail.setText(reason == null ? "" : reason);
            detail.setVisible(reason != null);
        }
        revalidate();
        repaint();
    }
}
