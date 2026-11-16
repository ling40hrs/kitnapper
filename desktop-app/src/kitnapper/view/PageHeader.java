package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The §5 content header every screen starts with: screen title (20 px) plus a
 * one-line subtitle (12 px secondary), on the 24/32 padding grid. Screens with
 * a page-level command (Inventory's "Add item") pass it in and get it pinned to
 * the right edge, vertically centred with the title block.
 */
final class PageHeader extends JPanel {

    private static final long serialVersionUID = 1L;

    PageHeader(String title, String subtitle) {
        this(title, subtitle, null);
    }

    PageHeader(String title, String subtitle, JComponent action) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 32, 16, 32));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(UiFactory.titleLabel(title));
        text.add(Box.createVerticalStrut(4));
        text.add(UiFactory.subtitleLabel(subtitle));
        add(text, BorderLayout.WEST);
        if (action != null) {
            JPanel pinned = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            pinned.setOpaque(false);
            pinned.add(action);
            add(pinned, BorderLayout.EAST);
        }
    }
}
