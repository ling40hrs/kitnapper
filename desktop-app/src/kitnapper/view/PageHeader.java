package kitnapper.view;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 * The §5 content header every screen starts with: screen title (20 px) plus a
 * one-line subtitle (12 px secondary), on the 24/32 padding grid.
 */
final class PageHeader extends JPanel {

    private static final long serialVersionUID = 1L;

    PageHeader(String title, String subtitle) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(24, 32, 16, 32));
        add(UiFactory.titleLabel(title));
        add(Box.createVerticalStrut(4));
        add(UiFactory.subtitleLabel(subtitle));
    }
}
