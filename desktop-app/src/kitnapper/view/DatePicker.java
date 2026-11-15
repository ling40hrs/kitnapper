package kitnapper.view;

import java.awt.Component;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

/**
 * The §8.7 due-date picker: a non-editable combo of {@code T … T+14} built
 * from the controller's choices, defaulting to {@code T+3}. Because only
 * valid dates exist in the model, INV-2's window is unreachable from the UI;
 * the service still rejects bad dates defensively.
 */
final class DatePicker extends JComboBox<LocalDate> {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_OFFSET = 3;

    DatePicker(List<LocalDate> dates) {
        super(new Vector<>(dates));
        setEditable(false);
        setSelectedIndex(Math.min(DEFAULT_OFFSET, getItemCount() - 1));
        UiFactory.styleCombo(this);
        setRenderer(new DateCell());
        setPreferredSize(new Dimension(0, 32));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        getAccessibleContext().setAccessibleName("Due date");
    }

    /** The selected due date; callers only read it on confirm. */
    LocalDate value() {
        return (LocalDate) getSelectedItem();
    }

    /** {@code yyyy-MM-dd} through {@link Formats}, like every other date. */
    private static final class DateCell extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setText(value instanceof LocalDate date ? Formats.date(date) : "");
            return this;
        }
    }
}
