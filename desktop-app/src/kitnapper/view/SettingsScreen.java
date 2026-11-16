package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The manager's Settings / Data page (design §5): the theme switch, the
 * "Animate loading placeholders" switch (design §13.4), the read-only save-file
 * and photo-folder locations, and the confirmed demo reset. Both switches apply
 * immediately and are session-only (D3); nothing here writes to the save file.
 */
public final class SettingsScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    /** View-declared callbacks; the controller supplies the implementation. */
    public interface Actions {
        boolean isDarkTheme();

        void setDarkTheme(boolean dark);

        String saveFilePath();

        String photoFolderPath();

        boolean animateSkeletons();

        void setAnimateSkeletons(boolean on);

        Optional<String> resetDemoData();
    }

    private final Actions actions;
    private final ToggleSwitch dark = new ToggleSwitch("Dark theme");
    private final ToggleSwitch animate = new ToggleSwitch("Animate loading placeholders");
    private final JLabel savePath = mono();
    private final JLabel photoPath = mono();
    private final JButton reset = UiFactory.destructiveButton("Reset demo data");

    public SettingsScreen(Actions actions) {
        super(new BorderLayout());
        this.actions = actions;
        setOpaque(false);
        ScreenCatalog.Meta meta = ScreenCatalog.meta("settings");
        add(new PageHeader(meta.title(), meta.subtitle()), BorderLayout.NORTH);
        add(SettingsSections.column(dark, animate, reset, savePath, photoPath),
            BorderLayout.CENTER);
        wire();
        reload();
    }

    /** Runs whenever the shell routes here; re-reads the current state. */
    public void onShown() {
        reload();
    }

    /** Re-applies token colors after a Theme switch (design §8.10). */
    public void refreshTheme() {
        UiFactory.refresh(this);
        repaint();
    }

    ToggleSwitch darkSwitch() {
        return dark;
    }

    ToggleSwitch animateSwitch() {
        return animate;
    }

    JButton resetButton() {
        return reset;
    }

    private void wire() {
        dark.setOnChange(actions::setDarkTheme);
        animate.setOnChange(actions::setAnimateSkeletons);
        reset.addActionListener(e -> confirmReset());
    }

    private void reload() {
        dark.setOn(actions.isDarkTheme());
        animate.setOn(actions.animateSkeletons());
        savePath.setText(actions.saveFilePath());
        photoPath.setText(actions.photoFolderPath());
    }

    private void confirmReset() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (!Dialogs.confirmDestructive(owner, "Reset demo data?",
                "Every loan, penalty, and uploaded photo is discarded, and the demo "
                    + "dataset is restored relative to today.",
                "Reset demo data")) {
            return;
        }
        Optional<String> failure = actions.resetDemoData();
        if (failure.isPresent()) {
            Toasts.show(owner, failure.get());
            return;
        }
        Toasts.show(owner, "Demo data reset.");
        reload();
    }

    private static JLabel mono() {
        JLabel label = new JLabel();
        label.setFont(Fonts.mono(12, Font.PLAIN));
        label.putClientProperty(UiFactory.ROLE, "title");
        label.setForeground(Theme.textPrimary);
        return label;
    }
}
