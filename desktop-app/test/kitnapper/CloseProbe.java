package kitnapper;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import kitnapper.model.UserRole;
import kitnapper.view.LoginFrame;
import kitnapper.view.MainFrame;

/**
 * Exit probe (plan §6.4, §9.4): both chrome frames default to
 * EXIT_ON_CLOSE, and a WINDOW_CLOSING dispatch on an EXIT_ON_CLOSE frame
 * fires the listener hook, prints EXITED_OK, and ends the JVM with 0 —
 * so this probe intentionally never prints its own PASS line. Needs a
 * display (xvfb-run); run-all.sh expects EXITED_OK instead.
 */
public final class CloseProbe {

    private static int passed;
    private static int failed;

    private CloseProbe() { }

    public static void main(String[] args) throws Exception {
        LoginFrame login = new LoginFrame();
        MainFrame shell = new MainFrame(UserRole.BORROWER, "Student One", "probe",
            key -> { }, () -> { }, () -> { });
        check("exit-on-close-set",
            login.getDefaultCloseOperation() == JFrame.EXIT_ON_CLOSE
            && shell.getDefaultCloseOperation() == JFrame.EXIT_ON_CLOSE);
        login.dispose();
        shell.dispose();
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) {
            System.exit(1);
        }

        JFrame victim = new JFrame("CloseProbe victim");
        victim.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        victim.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("EXITED_OK");
                System.out.flush();
            }
        });
        victim.setVisible(true);
        victim.dispatchEvent(new WindowEvent(victim, WindowEvent.WINDOW_CLOSING));

        // Reached only if the EXIT_ON_CLOSE hook did not end the JVM.
        System.out.println("PASS=" + passed + " FAIL=" + (failed + 1)
            + " EXIT_HOOK_MISSING");
        System.exit(1);
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
