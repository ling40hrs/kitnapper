package kitnapper.view;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;

/**
 * Token palette for the JDK's file chooser (design §6, F9 in the M6 walkthrough):
 * Metal builds its own white surfaces that ignore the app theme, so the photo
 * picker paints as a bright rectangle inside a dark shell. The chooser is modal
 * and short-lived, so it is recoloured when it opens rather than via global
 * {@code UIManager} keys that would also repaint unrelated lists and trees.
 */
final class FileChooserSkin {

    private FileChooserSkin() { }

    /** Applies the current theme to the chooser and every child it creates. */
    static void apply(JFileChooser chooser) {
        chooser.setBackground(Theme.bgSurface);
        chooser.setForeground(Theme.textPrimary);
        paint(chooser);
    }

    private static void paint(Component component) {
        if (component instanceof JList<?> list) {
            list.setBackground(Theme.bgElevated);
            list.setForeground(Theme.textPrimary);
        } else if (component instanceof JTable table) {
            table.setBackground(Theme.bgElevated);
            table.setForeground(Theme.textPrimary);
            table.setGridColor(Theme.borderSubtle);
        } else if (component instanceof JTree tree) {
            tree.setBackground(Theme.bgElevated);
            tree.setForeground(Theme.textPrimary);
        } else if (component instanceof JTextField field) {
            field.setBackground(Theme.bgControl);
            field.setForeground(Theme.textPrimary);
            field.setCaretColor(Theme.textPrimary);
        } else if (component instanceof JComboBox<?> combo) {
            combo.setBackground(Theme.bgControl);
            combo.setForeground(Theme.textPrimary);
        } else if (component instanceof JButton button) {
            button.setBackground(Theme.bgControl);
            button.setForeground(Theme.textPrimary);
        } else if (component instanceof JLabel label) {
            label.setForeground(Theme.textPrimary);
        } else if (component instanceof JPanel panel) {
            panel.setBackground(Theme.bgSurface);
        } else if (component instanceof JScrollPane scroll
                && scroll.getViewport() != null) {
            scroll.setBackground(Theme.bgSurface);
            scroll.getViewport().setBackground(Theme.bgElevated);
        }
        if (component instanceof JComponent child) {
            child.repaint();
        }
        if (component instanceof Container container) {
            paintChildren(container);
        }
    }

    private static void paintChildren(Container container) {
        for (Component child : container.getComponents()) {
            paint(child);
        }
    }
}
