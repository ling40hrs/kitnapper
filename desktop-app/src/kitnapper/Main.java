package kitnapper;

import java.awt.EventQueue;

import javax.swing.UIManager;

import kitnapper.controller.AppController;
import kitnapper.service.AppContext;
import kitnapper.service.DataStore;
import kitnapper.service.DataStoreImpl;
import kitnapper.view.Fonts;
import kitnapper.view.Theme;

/**
 * Entry point (M2): installs the cross-platform look-and-feel, resolves
 * fonts, applies the dark theme, loads (or seeds) the save file — before
 * the first frame paints, so launch needs no loading state (design
 * §13.1) — then hands off to the controller on the EDT.
 */
public final class Main {

    private Main() { }

    public static void main(String[] args) {
        EventQueue.invokeLater(Main::launch);
    }

    private static void launch() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            throw new IllegalStateException("Look-and-feel install failed", e);
        }
        Fonts.load();
        Theme.apply(true);
        DataStore store = new DataStoreImpl();
        store.load();
        AppContext context = new AppContext(store);
        new AppController(context).start();
    }
}
