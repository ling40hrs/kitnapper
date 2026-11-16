package kitnapper.view;

import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;

import kitnapper.model.EquipmentCategory;

/**
 * Icon audit (design §10, plan §7 WP4): every Fluent/fallback pair the app
 * actually draws must resolve — {@link Fonts#glyph} answers with the fluent
 * codepoint or the fallback — and the answer must be displayable by the font
 * that paints it. The category tiles carry Unicode glyphs only, so they are
 * held to the symbol font, never the icon font. Public so the headless
 * {@code FontProbe} in {@code kitnapper} can fold these into its tally, the
 * same shape as {@code ScrollBarChecks}.
 */
public final class GlyphChecks {

    /** Fluent codepoint, §10 fallback, and the action it stands for. */
    private static final String[][] PAIRS = {
        {"\uE721", "\u2315", "search"},
        {"\uE923", "\u2750", "restore"},
        {"\uE922", "\u25A2", "maximize"},
        {"\uE921", "\u2500", "minimize"},
        {"\uE90F", "\u2692", "repair"},
        {"\uE81C", "\u21BB", "history"},
        {"\uE80F", "\u25A3", "brand"},
        {"\uE793", "\u263E", "moon"},
        {"\uE706", "\u2600", "sun"},
        {"\uE76C", "\u203A", "chevron"},
        {"\uE73E", "\u2713", "checkmark"},
        {"\uE711", "\u2715", "close"},
    };

    private static final int GLYPH_SIZE = 16;

    private GlyphChecks() { }

    /** Runs every glyph check; the map keeps the report stable. */
    public static Map<String, Boolean> results() {
        Map<String, Boolean> results = new LinkedHashMap<>();
        Font icons = Fonts.icons(GLYPH_SIZE);
        Font symbol = Fonts.symbol(GLYPH_SIZE);
        System.out.println("icon_font=" + icons.getFamily()
            + " symbol_font=" + symbol.getFamily());

        int resolved = 0;
        int paintable = 0;
        int fallbacks = 0;
        for (String[] pair : PAIRS) {
            String glyph = Fonts.glyph(pair[0], pair[1]);
            if (glyph.equals(pair[0]) || glyph.equals(pair[1])) {
                resolved++;
            }
            if (displayable(icons, glyph) || displayable(symbol, glyph)) {
                paintable++;
            }
            if (displayable(symbol, pair[1])) {
                fallbacks++;
            }
        }
        results.put("glyph-pairs-resolve", resolved == PAIRS.length);
        results.put("glyph-pairs-paint", paintable == PAIRS.length);
        results.put("glyph-fallbacks-paint", fallbacks == PAIRS.length);

        int categories = 0;
        for (EquipmentCategory category : EquipmentCategory.values()) {
            if (displayable(symbol, GradientTile.glyphFor(category))) {
                categories++;
            }
        }
        results.put("category-glyphs-paint",
            categories == EquipmentCategory.values().length);
        return results;
    }

    private static boolean displayable(Font font, String glyph) {
        return glyph != null && glyph.length() == 1 && font.canDisplay(glyph.charAt(0));
    }
}
