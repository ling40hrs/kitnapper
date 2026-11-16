package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The §8.7 recent-activity rail: the last six derived borrow/return events,
 * newest first, each as a muted date line above its sentence, separated by a
 * 1 px hairline. The order and the event set are D28 controller work; this
 * list only renders the names and formats the frozen fee it is given.
 */
final class ActivityList extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String ROWS = "rows";
    private static final String EMPTY = "empty";

    private final CardLayout cards = new CardLayout();
    private final JPanel rows = new JPanel();

    ActivityList() {
        setLayout(cards);
        setOpaque(false);
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        add(rows, ROWS);
        add(new EmptyState(Fonts.glyph("\uE81C", "\u21BB"),
            "No lending activity yet.", null, null), EMPTY);
        cards.show(this, EMPTY);
    }

    /** Rebuilds the rail; an empty list swaps in the §8.7 empty state. */
    void setEntries(List<ActivityEntry> entries) {
        rows.removeAll();
        for (int i = 0; i < entries.size(); i++) {
            rows.add(new Row(entries.get(i), i < entries.size() - 1));
        }
        rows.revalidate();
        rows.repaint();
        cards.show(this, entries.isEmpty() ? EMPTY : ROWS);
    }

    void refreshTheme() {
        UiFactory.refresh(this);
        repaint();
    }

    /** "Student One checked out Sony A7 IV", plus the fee on a late return. */
    static String textOf(ActivityEntry entry) {
        String text = entry.borrowerName()
            + (entry.checkout() ? " checked out " : " returned ")
            + entry.itemName();
        return entry.fee() > 0 ? text + " · " + Formats.money(entry.fee()) + " fee" : text;
    }

    /** One event: muted date over the sentence, with a hairline after it. */
    private static final class Row extends JPanel {

        private static final long serialVersionUID = 1L;

        private final boolean separator;

        Row(ActivityEntry entry, boolean separator) {
            super(new BorderLayout(0, 2));
            this.separator = separator;
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            JLabel date = UiFactory.label(Formats.date(entry.date()), "muted");
            date.setFont(Fonts.mono(11, Font.PLAIN));
            JLabel text = UiFactory.label(textOf(entry), "title");
            text.setFont(Fonts.text(13, Font.PLAIN));
            add(date, BorderLayout.NORTH);
            add(text, BorderLayout.CENTER);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!separator) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Theme.borderSubtle);
            g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            g2.dispose();
        }
    }
}
