package kitnapper;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import kitnapper.model.UserRole;
import kitnapper.view.LoginFrame;
import kitnapper.view.MainFrame;
import kitnapper.view.Theme;

/**
 * Shell probe (plan §6.4, §9.4): login/shell construction, routing and the
 * Ctrl+F binding (5 structural checks), plus the folded-in FrameSmoke paint
 * checks (7) — every chrome surface paints in both themes without an
 * exception and renders more than a flat fill. Needs a display (xvfb-run).
 * Title/status text is read by reflection (preferences §4 sanctions it);
 * the view exposes no getters.
 */
public final class FrameProbe {

    private static int passed;
    private static int failed;

    private FrameProbe() { }

    public static void main(String[] args) throws Exception {
        Theme.apply(true);
        LoginFrame login = new LoginFrame();
        MainFrame borrower = new MainFrame(UserRole.BORROWER, "Student One",
            "10 items · 0 selected", key -> { }, () -> { }, () -> { });
        MainFrame manager = new MainFrame(UserRole.MANAGER, "A. Manager",
            "10 items · 0 selected", key -> { }, () -> { }, () -> { });

        check("login-construct", login.getTitle().equals("KitNapper — Sign in")
            && login.isUndecorated()
            && login.getMinimumSize().getSize().equals(new java.awt.Dimension(1024, 640))
            && login.getDefaultCloseOperation() == JFrame.EXIT_ON_CLOSE
            && login.getRootPane().getDefaultButton() != null);
        check("shell-borrower", screenTitle(borrower).equals("Catalog")
            && statusLeft(borrower).equals("10 items · 0 selected"));
        check("shell-manager", screenTitle(manager).equals("Dashboard"));
        borrower.showScreen("myloans");
        boolean routes = screenTitle(borrower).equals("My Loans");
        borrower.showScreen("dashboard");
        routes &= screenTitle(borrower).equals("Dashboard");
        borrower.showScreen("bogus-key");
        routes &= screenTitle(borrower).equals("Dashboard");
        check("showscreen-nav", routes);
        check("ctrlf-binding",
            borrower.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .get(KeyStroke.getKeyStroke("control F")) != null
            && borrower.getRootPane().getActionMap().get("focusSearch") != null);

        // Realize the frames before offscreen painting: paint(g) renders the
        // full tree only once the peer exists.
        login.setVisible(true);
        borrower.setVisible(true);
        manager.setVisible(true);

        paintCheck(login, login::refreshTheme, "paint-login-dark", true);
        paintCheck(login, login::refreshTheme, "paint-login-light", false);
        paintCheck(borrower, borrower::refreshTheme, "paint-shell-dark", true);
        paintCheck(borrower, borrower::refreshTheme, "paint-shell-light", false);
        paintCheck(manager, manager::refreshTheme, "paint-manager-dark", true);
        paintCheck(manager, manager::refreshTheme, "paint-manager-light", false);
        Theme.apply(false);
        borrower.refreshTheme();
        check("refresh-theme", borrower.getContentPane().getBackground().equals(Theme.bgApp)
            && !Theme.isDark());

        login.dispose();
        borrower.dispose();
        manager.dispose();
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** Applies the theme, repaints through the frame, and paints it offscreen. */
    private static void paintCheck(JFrame frame, Runnable refresh,
                                   String name, boolean dark) {
        Theme.apply(dark);
        refresh.run();
        BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(),
            BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        frame.paint(g);
        g.dispose();
        check(name, nonBlank(image));
    }

    /** More than a flat fill: sampled pixels must leave the corner color. */
    private static boolean nonBlank(BufferedImage image) {
        int corner = image.getRGB(0, 0);
        int different = 0;
        for (int y = 0; y < image.getHeight(); y += 2) {
            for (int x = 0; x < image.getWidth(); x += 2) {
                if (image.getRGB(x, y) != corner) {
                    different++;
                }
            }
        }
        return different > 750;
    }

    private static String screenTitle(MainFrame frame) throws Exception {
        return (String) field(field(frame, "titleBar"), "screenTitle");
    }

    private static String statusLeft(MainFrame frame) throws Exception {
        return ((JLabel) field(field(frame, "statusBar"), "left")).getText();
    }

    private static Object field(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
