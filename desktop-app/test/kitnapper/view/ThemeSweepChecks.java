package kitnapper.view;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Theme-sweep audit (design §8, plan §7 WP4): the switch must reach every
 * mounted screen, not just the frame chrome. For both themes the probe applies
 * the token set, runs the frame's recursive {@code refreshTheme} path, and
 * asserts every role-marked label carries the colour its role maps to — a
 * screen that forgot to re-theme leaves stale tokens behind and fails here.
 */
final class ThemeSweepChecks {

    private ThemeSweepChecks() { }

    /** Sweeps both themes over one mounted screen and restores dark. */
    static void sweep(String tag, MainFrame frame, JComponent screen) {
        sweepTo(tag + "-theme-dark", frame, screen, true);
        sweepTo(tag + "-theme-light", frame, screen, false);
        Theme.apply(true);
        frame.refreshTheme();
        UiProbeSupport.settle(frame);
    }

    private static void sweepTo(String tag, MainFrame frame, JComponent screen, boolean dark) {
        Theme.apply(dark);
        frame.refreshTheme();
        UiProbeSupport.settle(frame);
        int labelled = 0;
        int stale = 0;
        for (Component child : descendants(screen)) {
            if (!(child instanceof JLabel label)) {
                continue;
            }
            if (!(label.getClientProperty(UiFactory.ROLE) instanceof String role)) {
                continue;
            }
            labelled++;
            if (!UiFactory.colorFor(role).equals(label.getForeground())) {
                stale++;
            }
        }
        UiProbeSupport.check(tag, labelled > 0 && stale == 0);
    }

    private static List<Component> descendants(Container root) {
        List<Component> all = new ArrayList<>();
        for (Component child : root.getComponents()) {
            all.add(child);
            if (child instanceof Container container) {
                all.addAll(descendants(container));
            }
        }
        return all;
    }
}
