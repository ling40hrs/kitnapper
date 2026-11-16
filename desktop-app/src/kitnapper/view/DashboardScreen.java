package kitnapper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The §8.7 manager Dashboard: three KPI cards over a read-only "Overdue now"
 * table and the derived recent-activity rail. Every count, the overdue rows
 * and the D28 activity order come from {@link Actions} — the screen only lays
 * them out and writes the §5.7 status counts. Rows are not a navigation
 * surface (M4): the sidebar owns routing.
 */
public final class DashboardScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    /** View-declared callbacks (§5.5); the controller supplies them. */
    public interface Actions {
        int loansOut();

        int overdueCount();

        int unpaidCount();

        String unpaidTotal();

        List<LoanRow> overdueLoans();

        List<ActivityEntry> activity();
    }

    private static final int PAD_X = 32;
    private static final int GAP = 16;
    private static final int RAIL_WIDTH = 280;

    private final Actions actions;
    private final BiConsumer<String, String> status;
    private final KpiCard outCard = new KpiCard("Out now");
    private final KpiCard overdueCard = new KpiCard("Overdue");
    private final KpiCard unpaidCard = new KpiCard("Unpaid penalties");
    private final LoansTableSection overdueSection =
        new LoansTableSection(LoansTableModel.Mode.OVERDUE);
    private final ActivityList activity = new ActivityList();

    public DashboardScreen(Actions actions, BiConsumer<String, String> status) {
        super(new BorderLayout());
        this.actions = Objects.requireNonNull(actions, "actions");
        this.status = Objects.requireNonNull(status, "status");
        setOpaque(false);
        ScreenCatalog.Meta meta = ScreenCatalog.meta("dashboard");
        add(new PageHeader(meta.title(), meta.subtitle()), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        reload();
    }

    /** No omnibar commands on the dashboard (§8.7). */
    public JComponent commands() {
        return null;
    }

    /** Runs whenever the shell routes here. */
    public void onShown() {
        reload();
    }

    /** Re-reads every count, the overdue rows and the activity list. */
    public void reload() {
        int out = actions.loansOut();
        int overdue = actions.overdueCount();
        String unpaid = actions.unpaidTotal();
        outCard.setValue(String.valueOf(out));
        overdueCard.setValue(String.valueOf(overdue));
        unpaidCard.setValue(String.valueOf(actions.unpaidCount()));
        unpaidCard.setCaption(unpaid);
        overdueSection.setRows(actions.overdueLoans(), "Nothing is overdue.", null);
        activity.setEntries(actions.activity());
        status.accept(Formats.count(out, "loan", "loans") + " · " + overdue + " overdue",
            unpaid);
    }

    /** Re-applies token colors after a Theme switch (design §8.10). */
    public void refreshTheme() {
        UiFactory.refresh(this);
        overdueSection.refreshTheme();
        activity.refreshTheme();
    }

    LoansTableSection section() {
        return overdueSection;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, GAP));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, PAD_X, 16, PAD_X));
        body.add(kpiRow(), BorderLayout.NORTH);
        JPanel split = new JPanel(new BorderLayout(GAP, 0));
        split.setOpaque(false);
        split.add(sectionPanel("Overdue now", overdueSection), BorderLayout.CENTER);
        JPanel rail = sectionPanel("Recent activity", activity);
        rail.setPreferredSize(new Dimension(RAIL_WIDTH, 0));
        split.add(rail, BorderLayout.EAST);
        body.add(split, BorderLayout.CENTER);
        return body;
    }

    private JPanel kpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, GAP, 0));
        row.setOpaque(false);
        row.add(outCard);
        row.add(overdueCard);
        row.add(unpaidCard);
        return row;
    }

    /** A 14 px section title above its body, on the 8 px spacing grid. */
    private static JPanel sectionPanel(String title, JComponent body) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JLabel label = UiFactory.label(title, "title");
        label.setFont(Fonts.text(14, Font.BOLD));
        panel.add(label, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }
}
