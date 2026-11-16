package kitnapper.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JPanel;

import kitnapper.model.LoanStatus;
import kitnapper.model.PenaltyStatus;

/**
 * The loan and penalty pill families (§3.3 anatomy, design §3.3 dot mapping:
 * an active loan reads as Loaned, an unpaid fee as Overdue). Geometry comes
 * from {@link Badges#paintPill} so every status pill in the app is identical;
 * colours are read at paint time, so a theme switch needs only a repaint.
 */
final class StatusBadges {

    private StatusBadges() { }

    /** Badge component for the loan preview and detail panes. */
    static JComponent loanStatus(LoanStatus status) {
        return new Pill(status, null);
    }

    /** Badge component for the penalty preview and detail panes. */
    static JComponent penaltyStatus(PenaltyStatus status) {
        return new Pill(null, status);
    }

    /** Paints a loan pill with its top-left corner at ({@code x}, {@code y}). */
    static void paintLoan(Graphics2D g, int x, int y, LoanStatus status) {
        Badges.paintPill(g, x, y, loanDot(status), null, loanWord(status));
    }

    /** Paints a penalty pill with its top-left corner at ({@code x}, {@code y}). */
    static void paintPenalty(Graphics2D g, int x, int y, PenaltyStatus status) {
        Badges.paintPill(g, x, y, penaltyDot(status), null, penaltyWord(status));
    }

    /** Display word (§4: title case, no all-caps styling). */
    static String loanWord(LoanStatus status) {
        return switch (status) {
            case ACTIVE -> "Active";
            case OVERDUE -> "Overdue";
            case RETURNED -> "Returned";
        };
    }

    /** Display word for a penalty state. */
    static String penaltyWord(PenaltyStatus status) {
        return switch (status) {
            case UNPAID -> "Unpaid";
            case PAID -> "Paid";
            case WAIVED -> "Waived";
        };
    }

    /** Design §3.3: active loans reuse the Loaned dot, returns the retired one. */
    private static Color loanDot(LoanStatus status) {
        return switch (status) {
            case ACTIVE -> Theme.dotLoaned;
            case OVERDUE -> Theme.dotOverdue;
            case RETURNED -> Theme.dotDecommissioned;
        };
    }

    /** Design §3.3: UNPAID → Overdue, PAID → Available, WAIVED → Decommissioned. */
    private static Color penaltyDot(PenaltyStatus status) {
        return switch (status) {
            case UNPAID -> Theme.dotOverdue;
            case PAID -> Theme.dotAvailable;
            case WAIVED -> Theme.dotDecommissioned;
        };
    }

    /** The reusable component form; paints itself and sizes to the pill. */
    private static final class Pill extends JPanel {

        private static final long serialVersionUID = 1L;

        private final LoanStatus loan;
        private final PenaltyStatus penalty;

        Pill(LoanStatus loan, PenaltyStatus penalty) {
            this.loan = loan;
            this.penalty = penalty;
            setOpaque(false);
            setFocusable(false);
            String word = loan != null ? loanWord(loan) : penaltyWord(penalty);
            getAccessibleContext().setAccessibleName(
                (loan != null ? "Loan status " : "Penalty status ") + word);
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics words = getFontMetrics(Fonts.text(12, Font.BOLD));
            return new Dimension(Badges.pillWidth(words, getFontMetrics(Fonts.icons(12)),
                null, word()), Badges.PILL_HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            if (loan != null) {
                paintLoan(g2, 0, 0, loan);
            } else {
                paintPenalty(g2, 0, 0, penalty);
            }
            g2.dispose();
        }

        private String word() {
            return loan != null ? loanWord(loan) : penaltyWord(penalty);
        }
    }
}
