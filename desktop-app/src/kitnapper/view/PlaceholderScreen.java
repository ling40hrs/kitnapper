package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Placeholder surface for screens that are not implemented yet: a real page
 * header (title + subtitle) plus a muted empty state. No fake controls, so
 * nothing looks clickable that is not (design.md §6).
 */
final class PlaceholderScreen {

    private PlaceholderScreen() {
    }

    static JPanel create(ScreenCatalog.Meta meta) {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(24, 32, 8, 32));
        header.add(UiFactory.titleLabel(meta.title()));
        header.add(Box.createVerticalStrut(4));
        header.add(UiFactory.subtitleLabel(meta.subtitle()));
        JLabel message = UiFactory.label("No records to show yet.", "muted");
        message.setFont(Fonts.text(14, Font.PLAIN));
        message.setHorizontalAlignment(SwingConstants.CENTER);
        page.add(header, BorderLayout.NORTH);
        page.add(message, BorderLayout.CENTER);
        return page;
    }
}
