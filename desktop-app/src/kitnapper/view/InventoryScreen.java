package kitnapper.view;

import java.awt.BorderLayout;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import kitnapper.model.Equipment;

/**
 * The manager's inventory page (design §5 "Inventory"): the items table, the
 * photo preview with its state actions, the omnibar text filter, and the
 * counts on the shell status bar. Every rule — what is allowed, what a
 * transition means — comes from the controller through {@link Actions}; the
 * view only lays out, formats, and forwards.
 */
public final class InventoryScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    /** View-declared callbacks; the controller supplies the implementation. */
    public interface Actions {
        List<Equipment> search(String query);

        boolean isManager();

        boolean onLoan(Equipment item);

        Optional<String> flagMaintenance(Equipment item);

        Optional<String> clearMaintenance(Equipment item);

        Optional<String> decommission(Equipment item);

        Optional<String> add(EquipmentForm form, String stagedPhoto);

        Optional<String> update(String equipmentId, EquipmentForm form, String stagedPhoto,
                                boolean removePhoto);

        PhotoStage stagePhoto(java.nio.file.Path source);

        void discardStaged(String stagedName);

        PhotoSource photos();

        void setStatus(String left, String right);
    }

    private final Actions actions;
    private final JTextField search;
    private final InventoryTable table = new InventoryTable();
    private final InventoryBody body;
    private final InventoryEditor editor;
    private final InventoryStateRunner runner;
    private String selectedId;
    private List<Equipment> results = List.of();

    public InventoryScreen(Actions actions, JTextField searchField) {
        super(new BorderLayout());
        this.actions = Objects.requireNonNull(actions, "actions");
        this.search = Objects.requireNonNull(searchField, "searchField");
        this.editor = new InventoryEditor(actions);
        this.runner = new InventoryStateRunner(actions, this, this::reload);
        InventoryPreviewPane preview = new InventoryPreviewPane(actions.photos(),
            this::edit, runner::flag, runner::clear, runner::retire);
        JButton clearSearch = UiFactory.secondaryButton("Clear search");
        clearSearch.addActionListener(e -> search.setText(""));
        EmptyState empty = new EmptyState(Fonts.glyph("\uE721", "\u2315"),
            "No items match this filter.", "Try another search, or clear it.", clearSearch);
        this.body = new InventoryBody(table, empty, preview);
        JButton add = UiFactory.primaryButton("Add item");
        add.addActionListener(e -> addItem());
        setOpaque(false);
        ScreenCatalog.Meta meta = ScreenCatalog.meta("inventory");
        add(new PageHeader(meta.title(), meta.subtitle(), add), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        wire();
        reload();
    }

    /** Runs whenever the shell routes here; keeps counts correct. */
    public void onShown() {
        reload();
    }

    /** Re-runs the query and re-renders the table, preview, and status bar. */
    public void reload() {
        results = actions.search(search.getText());
        table.setItems(results);
        if (selectedId != null) {
            table.selectItem(selectedId);
        }
        if (selected() == null) {
            selectedId = null;
        }
        body.showView(results.isEmpty());
        refreshPreview();
        setStatus();
    }

    /** Re-applies token colors after a Theme switch (design §8.10). */
    public void refreshTheme() {
        UiFactory.refresh(this);
        body.refreshTheme();
        repaint();
    }

    private void wire() {
        table.setOnSelect(item -> {
            selectedId = item.getId();
            refreshPreview();
            setStatus();
        });
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                reload();
            }

            public void removeUpdate(DocumentEvent e) {
                reload();
            }

            public void changedUpdate(DocumentEvent e) {
                reload();
            }
        });
    }

    private void refreshPreview() {
        Equipment selected = selected();
        if (selected == null) {
            body.preview().clear();
            return;
        }
        body.preview().show(selected, actions.onLoan(selected));
    }

    private Equipment selected() {
        if (selectedId == null) {
            return null;
        }
        for (Equipment item : results) {
            if (item.getId().equals(selectedId)) {
                return item;
            }
        }
        return null;
    }

    private void addItem() {
        if (editor.add(SwingUtilities.getWindowAncestor(this))) {
            reload();
        }
    }

    private void edit(Equipment item) {
        if (editor.edit(SwingUtilities.getWindowAncestor(this), item)) {
            reload();
        }
    }

    private void setStatus() {
        String count = Formats.count(results.size(), "item", "items");
        actions.setStatus(count + " · " + (selected() == null ? "0" : "1") + "/1 selected",
            Formats.money(results.stream().mapToDouble(Equipment::getReplacementValue).sum()));
    }
}
