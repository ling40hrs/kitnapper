package kitnapper.view;

import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The §6 empty state: muted glyph, a 14 px explanation, an optional 12 px
 * secondary line, and the optional action that fixes it — centred in whatever
 * space it is given. The catalog uses it for no results; the preview pane for
 * nothing selected.
 */
final class EmptyState extends JPanel {

    private static final long serialVersionUID = 1L;

    EmptyState(String glyph, String message, String detail, JButton action) {
        super(new GridBagLayout());
        setOpaque(false);
        getAccessibleContext().setAccessibleName(message);
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel icon = new JLabel(glyph);
        icon.setFont(Fonts.icons(28));
        icon.setForeground(Theme.textMuted);
        icon.setAlignmentX(CENTER_ALIGNMENT);
        JLabel text = new JLabel(message);
        text.setFont(Fonts.text(14, java.awt.Font.PLAIN));
        text.putClientProperty(UiFactory.ROLE, "title");
        text.setForeground(Theme.textPrimary);
        text.setAlignmentX(CENTER_ALIGNMENT);
        box.add(icon);
        box.add(Box.createVerticalStrut(8));
        box.add(text);
        if (detail != null) {
            JLabel line = UiFactory.label(detail, "secondary");
            line.setAlignmentX(CENTER_ALIGNMENT);
            box.add(Box.createVerticalStrut(4));
            box.add(line);
        }
        if (action != null) {
            action.setAlignmentX(CENTER_ALIGNMENT);
            box.add(Box.createVerticalStrut(12));
            box.add(action);
        }
        add(box);
    }
}
