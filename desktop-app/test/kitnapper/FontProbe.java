package kitnapper;

import kitnapper.view.Fonts;

/**
 * Font resolution probe (plan §6.4, §9.4): the text/display/mono stacks
 * resolve to installed families (a missing physical font falls back to
 * "Dialog", which must never win) and the glyph helper always answers.
 * Kept thin by design (WP0.5 note). Runs headless; prints the resolved
 * families for the baseline table.
 */
public final class FontProbe {

    private static int passed;
    private static int failed;

    private FontProbe() { }

    public static void main(String[] args) {
        Fonts.load();
        String text = Fonts.text(12, java.awt.Font.PLAIN).getFamily();
        String display = Fonts.display(20, java.awt.Font.BOLD).getFamily();
        String mono = Fonts.mono(12, java.awt.Font.PLAIN).getFamily();
        String icon = Fonts.icons(12).getFamily();
        System.out.println("text=" + text + " display=" + display
            + " mono=" + mono + " icon=" + icon);

        check("families-resolve", !"Dialog".equals(text) && !"Dialog".equals(display)
            && !"Dialog".equals(mono));
        check("glyph-fallback", !Fonts.glyph("\uE80F", "\u25A3").isEmpty());

        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
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
