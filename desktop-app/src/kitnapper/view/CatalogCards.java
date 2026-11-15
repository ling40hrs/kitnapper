package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

import kitnapper.model.Equipment;

/**
 * The §8.4 cards layout: a 3-column grid of {@link EquipmentCard}s inside a
 * scroll pane. It holds the same selection identity as the table; switching
 * layouts only changes which component is showing, never the selected item.
 */
final class CatalogCards extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int GAP = 16;

    private final JPanel grid = new JPanel(new GridLayout(0, 3, GAP, GAP));
    private final Map<String, EquipmentCard> cards = new LinkedHashMap<>();
    private final JScrollPane scroll;
    private Consumer<Equipment> onSelect = item -> { };

    CatalogCards() {
        super(new BorderLayout());
        setOpaque(false);
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(0, 0, GAP, 0));
        JPanel holder = new WidthTracking();
        holder.setOpaque(false);
        holder.setLayout(new BorderLayout());
        holder.add(grid, BorderLayout.NORTH);
        scroll = new JScrollPane(holder, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(TableSkins.ROW_HEIGHT);
        add(scroll, BorderLayout.CENTER);
    }

    void setItems(List<Equipment> items) {
        cards.clear();
        grid.removeAll();
        for (Equipment item : items) {
            EquipmentCard card = new EquipmentCard(item, this::choose);
            cards.put(item.getId(), card);
            grid.add(card);
        }
        grid.revalidate();
        grid.repaint();
    }

    void setOnSelect(Consumer<Equipment> listener) {
        onSelect = listener == null ? item -> { } : listener;
    }

    /** Marks exactly one card selected; unknown IDs clear the highlight. */
    void selectItem(String id) {
        for (Map.Entry<String, EquipmentCard> entry : cards.entrySet()) {
            entry.getValue().setSelected(entry.getKey().equals(id));
        }
    }

    /** Number of rendered cards (probe seam; the grid is rebuilt on every query). */
    int cardCount() {
        return cards.size();
    }

    void refreshTheme() {
        scroll.getViewport().setBackground(Theme.bgApp);
        repaint();
    }

    private void choose(Equipment item) {
        onSelect.accept(item);
    }

    /**
     * Always tracks the viewport width so the 3-column grid never clips its
     * third column; height still scrolls with the content.
     */
    private static final class WidthTracking extends JPanel implements Scrollable {

        private static final long serialVersionUID = 1L;

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
            int direction) {
            return TableSkins.ROW_HEIGHT;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation,
            int direction) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
