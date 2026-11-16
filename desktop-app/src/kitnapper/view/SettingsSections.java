package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The Settings / Data page's four sections (design §5): appearance, loading,
 * storage paths, and demo data. Extracted from {@link SettingsScreen} at M5
 * WP5 to keep that class under the line cap; it is layout only and owns no
 * state.
 */
final class SettingsSections {

    private static final int PAD_X = 32;

    private SettingsSections() {
    }

    /** Builds the whole column around the screen's controls. */
    static JPanel column(ToggleSwitch dark, ToggleSwitch animate, JButton reset,
                         JLabel savePath, JLabel photoPath) {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(0, PAD_X, 24, PAD_X));
        column.add(toggleSection("Appearance", "Dark theme", dark,
            "Switches the whole app between the dark and light token sets."));
        column.add(toggleSection("Loading", "Animate loading placeholders", animate,
            "Turns off the pulse and the crossfade on photo loading placeholders."));
        column.add(storageSection(savePath, photoPath));
        column.add(resetSection(reset));
        column.add(Box.createVerticalGlue());
        return column;
    }

    private static JPanel toggleSection(String heading, String name, ToggleSwitch control,
                                        String caption) {
        JPanel section = section(heading);
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(0f);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel text = new JLabel(name);
        text.setFont(Fonts.text(14, Font.PLAIN));
        text.putClientProperty(UiFactory.ROLE, "title");
        text.setForeground(Theme.textPrimary);
        row.add(text, BorderLayout.WEST);
        row.add(control, BorderLayout.EAST);
        section.add(row);
        section.add(Box.createVerticalStrut(4));
        section.add(caption(caption));
        return section;
    }

    private static JPanel storageSection(JLabel savePath, JLabel photoPath) {
        JPanel section = section("Storage");
        section.add(storageRow("Save file", savePath));
        section.add(storageRow("Photos", photoPath));
        section.add(caption("Local only — never synced, never committed."));
        return section;
    }

    private static JPanel resetSection(JButton reset) {
        JPanel section = section("Demo data");
        section.add(caption("Restores the PRD §9 dataset and empties the photo folder."));
        section.add(Box.createVerticalStrut(8));
        reset.setAlignmentX(0f);
        section.add(reset);
        return section;
    }

    private static JPanel storageRow(String name, JLabel value) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(0f);
        JLabel label = UiFactory.label(name, "secondary");
        label.setAlignmentX(0f);
        value.setAlignmentX(0f);
        row.add(label);
        row.add(Box.createVerticalStrut(2));
        row.add(value);
        row.add(Box.createVerticalStrut(10));
        return row;
    }

    private static JPanel section(String heading) {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(0f);
        section.setBorder(BorderFactory.createEmptyBorder(8, 0, 20, 0));
        JLabel title = new JLabel(heading);
        title.setFont(Fonts.text(14, Font.BOLD));
        title.putClientProperty(UiFactory.ROLE, "title");
        title.setForeground(Theme.textPrimary);
        title.setAlignmentX(0f);
        section.add(title);
        section.add(Box.createVerticalStrut(8));
        return section;
    }

    private static JLabel caption(String text) {
        JLabel label = UiFactory.label(text, "muted");
        label.setAlignmentX(0f);
        return label;
    }
}
