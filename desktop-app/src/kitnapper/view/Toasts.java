package kitnapper.view;

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLayeredPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * The §6/§8.8 toast: 320 px, {@code BG_ELEVATED} + border, radius 8, icon plus
 * text, bottom-right, 4 s auto-dismiss, at most three stacked. It mounts on the
 * frame's {@code POPUP_LAYER} — never the glass pane that {@link Dialogs} owns.
 * {@link ToastView} owns the §7 slide-up + fade animation.
 */
public final class Toasts {

    static final int WIDTH = 320;
    static final int HEIGHT = 48;
    private static final int MARGIN = 16;
    private static final int GAP = 8;
    private static final int MAX = 3;
    private static final int LIFETIME_MS = 4000;
    private static final String STACK = "kitnapper.toastStack";

    private Toasts() { }

    /** Shows a toast on {@code owner}; non-frame owners are ignored. */
    public static void show(Window owner, String message) {
        if (owner == null || message == null) {
            throw new IllegalArgumentException("a toast needs an owner and a message");
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> show(owner, message));
            return;
        }
        if (!(owner instanceof RootPaneContainer container) || !owner.isDisplayable()) {
            return;
        }
        JLayeredPane pane = container.getLayeredPane();
        if (pane.getWidth() <= 0) {
            return;
        }
        Stack stack = stackOf(pane);
        while (stack.toasts.size() >= MAX) {
            evict(pane, stack.toasts.remove(0));
        }
        ToastView toast = new ToastView(message);
        stack.toasts.add(toast);
        pane.add(toast, JLayeredPane.POPUP_LAYER);
        layout(pane, stack);
        toast.open();
        Timer life = new Timer(LIFETIME_MS, e -> dismiss(pane, toast));
        life.setRepeats(false);
        life.start();
        toast.putClientProperty("kitnapper.life", life);
    }

    /** The oldest toast leaves at once when the stack is full (no third timer). */
    private static void evict(JLayeredPane pane, ToastView toast) {
        stopLife(toast);
        pane.remove(toast);
    }

    private static void dismiss(JLayeredPane pane, ToastView toast) {
        stopLife(toast);
        if (toast.isClosing()) {
            return;
        }
        Stack stack = stackOf(pane);
        toast.close(() -> {
            stack.toasts.remove(toast);
            pane.remove(toast);
            layout(pane, stack);
            pane.repaint();
        });
    }

    private static void stopLife(ToastView toast) {
        Object life = toast.getClientProperty("kitnapper.life");
        if (life instanceof Timer timer) {
            timer.stop();
        }
    }

    private static void layout(JLayeredPane pane, Stack stack) {
        int bottom = pane.getHeight() - MARGIN;
        for (int i = 0; i < stack.toasts.size(); i++) {
            int y = bottom - HEIGHT - i * (HEIGHT + GAP);
            stack.toasts.get(i).setBounds(pane.getWidth() - MARGIN - WIDTH, y, WIDTH, HEIGHT);
        }
    }

    private static Stack stackOf(JLayeredPane pane) {
        Object existing = pane.getClientProperty(STACK);
        if (existing instanceof Stack stack) {
            return stack;
        }
        Stack created = new Stack();
        pane.putClientProperty(STACK, created);
        return created;
    }

    /** Per-layered-pane bookkeeping; a client property, so no static leak. */
    private static final class Stack {
        private final List<ToastView> toasts = new ArrayList<>();
    }
}
