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

/**
 * The §8.2 280 px loan preview: {@code BG_ELEVATED} with a 1 px left border,
 * the item's category tile, title and status badge, the six loan facts, the
 * fee block (live accrual while out, frozen penalty once returned), and one
 * pinned {@code Log return} action with a hint caption beneath. Nothing
 * selected shows the muted empty state instead. My Loans reuses the pane with
 * the action hidden (M4-D5).
 */
final class LoanPreviewPane extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int WIDTH = 280;
    private static final int PAD = 16;
    private static final int MEDIA_WIDTH = WIDTH - PAD * 2 - 1;
    private static final String OUT = "\u2014";

    private final Consumer<LoanRow> onReturn;
    private final CardLayout states = new CardLayout();
    private final JPanel stack = new JPanel(states);
    private final JPanel mediaSlot = new JPanel(new BorderLayout());
    private final JPanel statusSlot = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JLabel title = new JLabel();
    private final JLabel borrowerValue = valueLabel(false);
    private final JLabel borrowedValue = valueLabel(true);
    private final JLabel dueValue = valueLabel(true);
    private final JLabel returnedValue = valueLabel(true);
    private final JLabel daysLateValue = valueLabel(true);
    private final JLabel idValue = valueLabel(true);
    private final LoanFeeBlock fee = new LoanFeeBlock();
    private final JButton returnButton = UiFactory.primaryButton("Log return");
    private final JLabel hint = UiFactory.label("", "muted");
    private LoanRow shown;

    LoanPreviewPane(Consumer<LoanRow> onReturn) {
        super(new BorderLayout());
        this.onReturn = onReturn == null ? row -> { } : onReturn;
        setOpaque(false);
        setPreferredSize(new Dimension(WIDTH, 0));
        setMinimumSize(new Dimension(WIDTH, 0));
        stack.setOpaque(false);
        statusSlot.setOpaque(false);
        stack.add(detail(), "detail");
        stack.add(new EmptyState(Fonts.glyph("\uE721", "\u2315"),
            "Select a loan to see details.", null, null), "empty");
        add(stack, BorderLayout.CENTER);
        clear();
    }

    /** Paint state for the selection, as decided by the screen. */
    void show(LoanRow row, boolean actionVisible, boolean actionEnabled, String hintText) {
        shown = row;
        mediaSlot.removeAll();
        mediaSlot.add(new GradientTile(row.category(), MEDIA_WIDTH), BorderLayout.CENTER);
        title.setText(row.itemName());
        title.setToolTipText(row.itemName());
        statusSlot.removeAll();
        statusSlot.add(StatusBadges.loanStatus(row.status()));
        borrowerValue.setText(row.borrowerName());
        borrowedValue.setText(Formats.date(row.borrowDate()));
        dueValue.setText(Formats.date(row.dueDate()));
        returnedValue.setText(row.returnDate() == null ? OUT : Formats.date(row.returnDate()));
        daysLateValue.setText(Long.toString(row.daysLate()));
        idValue.setText(row.loanId());
        fee.show(row);
        returnButton.setVisible(actionVisible);
        returnButton.setEnabled(actionEnabled);
        boolean hasHint = hintText != null && !hintText.isBlank();
        hint.setText(hasHint ? hintText : "");
        hint.setVisible(hasHint);
        mediaSlot.revalidate();
        statusSlot.revalidate();
        states.show(stack, "detail");
        revalidate();
        repaint();
    }

    /** Back to "Select a loan to see details." */
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
        info.add(row("Borrower", borrowerValue));
        info.add(row("Borrowed", borrowedValue));
        info.add(row("Due", dueValue));
        info.add(row("Returned", returnedValue));
        info.add(row("Days late", daysLateValue));
        info.add(row("ID", idValue));
        info.add(fee);
        return info;
    }

    private JPanel actionBar() {
        JPanel bar = new JPanel();
        bar.setOpaque(false);
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        returnButton.setAlignmentX(0f);
        returnButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        returnButton.addActionListener(e -> {
            if (shown != null) {
                onReturn.accept(shown);
            }
        });
        hint.setAlignmentX(0f);
        hint.setToolTipText(null);
        bar.add(returnButton);
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
