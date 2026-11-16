package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Window;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import kitnapper.model.PenaltyOutcome;
import kitnapper.model.PenaltyStatus;

/**
 * The §8.6 Penalties page: every frozen late fee newest assessed first, the
 * penalty preview with its {@code Mark paid}/{@code Waive…} actions, and the
 * §8.9 settle dialog. Settlement stays manager-only and one-way in the service;
 * this screen only opens the dialog, toasts the result, and reloads.
 */
public final class PenaltiesScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    /** View-declared callbacks (§5.5); the controller supplies them. */
    public interface Actions {
        List<PenaltyRow> penalties();

        Optional<String> settle(PenaltyRow row, PenaltyOutcome outcome, String reason);
    }

    private static final int PAD_X = 32;
    private static final String TABLE = "table";
    private static final String EMPTY = "empty";
    private static final String EMPTY_TITLE =
        "No penalties yet. Late fees appear here when an overdue loan is returned.";

    private final Actions actions;
    private final BiConsumer<String, String> status;
    private final PenaltiesTable table = new PenaltiesTable();
    private final JScrollPane scroll = table.inScrollPane();
    private final JPanel left = new JPanel(new CardLayout());
    private final PenaltyPreviewPane preview =
        new PenaltyPreviewPane(this::markPaid, this::waive);
    private String selectedId;

    public PenaltiesScreen(Actions actions, BiConsumer<String, String> status) {
        super(new BorderLayout());
        this.actions = Objects.requireNonNull(actions, "actions");
        this.status = Objects.requireNonNull(status, "status");
        setOpaque(false);
        ScreenCatalog.Meta meta = ScreenCatalog.meta("penalties");
        add(new PageHeader(meta.title(), meta.subtitle()), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        table.setOnSelect(this::select);
        reload();
    }

    /** No omnibar commands (§8.6). */
    public JComponent commands() {
        return null;
    }

    /** Runs whenever the shell routes here. */
    public void onShown() {
        reload();
    }

    /** Re-reads every penalty and re-renders table, preview and counts. */
    public void reload() {
        List<PenaltyRow> rows = actions.penalties();
        table.setItems(rows);
        ((CardLayout) left.getLayout()).show(left, rows.isEmpty() ? EMPTY : TABLE);
        reapplySelection(rows);
        status.accept(statusLeft(rows), aggregate(rows));
    }

    /** Re-applies token colors after a Theme switch (design §8.10). */
    public void refreshTheme() {
        UiFactory.refresh(this);
        scroll.getViewport().setBackground(Theme.bgSurface);
        preview.refreshTheme();
        repaint();
    }

    PenaltiesTable table() {
        return table;
    }

    PenaltyPreviewPane preview() {
        return preview;
    }

    private void select(PenaltyRow row) {
        selectedId = row.penaltyId();
        preview.show(row, true);
    }

    private void markPaid(PenaltyRow row) {
        settle(row, PenaltyOutcome.PAID);
    }

    /** The preview's {@code Waive…} action opens the dialog on the Waive segment. */
    private void waive(PenaltyRow row) {
        settle(row, PenaltyOutcome.WAIVED);
    }

    /** Both actions open §8.9, which is the confirmation design §7 requires. */
    private void settle(PenaltyRow row, PenaltyOutcome initial) {
        if (row.status() != PenaltyStatus.UNPAID) {
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        Optional<PenaltyOutcome> outcome = SettlePenaltyDialog.show(owner, row, initial,
            (chosen, reason) -> actions.settle(row, chosen, reason));
        if (outcome.isPresent()) {
            toast(owner, row, outcome.get());
        }
        reload();
    }

    private static void toast(Window owner, PenaltyRow row, PenaltyOutcome outcome) {
        String word = outcome == PenaltyOutcome.WAIVED ? "waived" : "marked paid";
        if (owner != null) {
            Toasts.show(owner, "Penalty " + row.penaltyId() + " " + word);
        }
    }

    private void reapplySelection(List<PenaltyRow> rows) {
        PenaltyRow row = rows.stream()
            .filter(candidate -> candidate.penaltyId().equals(selectedId))
            .findFirst()
            .orElse(null);
        if (row == null) {
            selectedId = null;
            preview.clear();
        } else {
            table.selectRow(selectedId);
            select(row);
        }
    }

    /** "N penalties · M unpaid" over every penalty (§5.7). */
    private static String statusLeft(List<PenaltyRow> rows) {
        long unpaid = rows.stream()
            .filter(row -> row.status() == PenaltyStatus.UNPAID)
            .count();
        return rows.size() + " penalties · " + unpaid + " unpaid";
    }

    /** Σ UNPAID amounts, formatted once (§5.7). */
    private static String aggregate(List<PenaltyRow> rows) {
        double total = rows.stream()
            .filter(row -> row.status() == PenaltyStatus.UNPAID)
            .mapToDouble(PenaltyRow::amount)
            .sum();
        return Formats.money(total);
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, PAD_X, 16, PAD_X));
        left.setOpaque(false);
        left.add(scroll, TABLE);
        left.add(new EmptyState(Fonts.glyph("\uE721", "\u2315"), EMPTY_TITLE, null, null), EMPTY);
        body.add(left, BorderLayout.CENTER);
        body.add(preview, BorderLayout.EAST);
        return body;
    }
}
