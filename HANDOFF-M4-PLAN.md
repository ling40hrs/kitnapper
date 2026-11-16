# KitNapper M4 — Implementation Backbone

> Scope: milestone **M4 — Returns, Fees, Manager Views**
> Repository: `OOP/Finals/` (`github.com/ling40hrs/kitnapper`), branch `main`
> Plan baseline: `main` @ `7f7ca1d` (`docs(m3): name the M4 plan as the next work package`)
> Written: 2026-11-16 · Status: **approved, WP0 committed**
> Previous plan: `HANDOFF-M3-PLAN.md` (M3 record, complete). Scope note: `HANDOFF-M4.md`.

> **Resume here (fresh session):** **M4 is complete** — WP0–WP6, the width pass
> and two close-out fixes are pushed (see §17, final commits `cf5a838` + this
> docs change). Next milestone: **M5 — inventory, photos, reset, settings**;
> read `HANDOFF-M5.md` (scope note) and write `HANDOFF-M5-PLAN.md` before any
> code, following the M3/M4 pattern. Read this file end to end, then
> `AGENTS.md` and `docs/preferences.md`. Every WP ends with `./build.sh` +
> `./test/run-all.sh` green, a log entry in §17, and a commit under the
> documented-task rule. Final M4 state: 117 Java files / 10,788 lines; 25 probe
> files / 2,811 lines; **12/12 probes green** (`LoansUiProbe PASS=105`,
> `LoansLayoutProbe PASS=12`, `LoanProbe PASS=35`); tallest file
> `CatalogScreen` 199. Carried to M6: the penalties table clips its Assessed
> column at 1280 (no width policy), toast overlap at small sizes.

---

## 1. Objective

Replace the five remaining loan/penalty placeholders with real screens and
finish the money workflow:

- **My Loans** (borrower): Active / Overdue / Returned tabs, loans table, loan
  preview with the fee breakdown and the loan's penalty state.
- **All Loans** (manager): sortable table of every loan, preview, **return
  action**.
- **Overdue** (manager): longest overdue first, days-late column, live accrued
  fees, return action.
- **Penalties** (manager): per-loan penalty list, PAID / WAIVE settlement with a
  required reason, preview.
- **Dashboard** (manager): three KPI cards (out now, overdue, unpaid penalties),
  the overdue table, and a derived recent-activity list.

M4 is **not** an inventory milestone. Inventory, photos, settings and reset stay
placeholders until M5; chrome/motion/DPI polish stays M6.

### Milestone exit criteria

1. `ant clean jar` succeeds on JDK 17 (bundled Ant via `sh`).
2. The M3 probe suite is still green and the two new probes are green (12/12).
3. SRS §6.2 steps 7–10 pass: restart persistence, manager overdue ₱200, return
   freezes exactly one `LATE_FEE`, waive with a reason.
4. Every Java file ≤ 200 lines; no hex outside `Theme`; no `service`/`controller`
   imports under `view`; no `@SuppressWarnings`.
5. Views never compute fees or statuses; every number comes from a controller
   call (D21).
6. Docs and handoff updated in the same change as behavior.
7. Build output, runtime data, and `AGENTS.md` are never staged.

---

## 2. Confirmed decisions (this session)

| # | Decision | Consequence |
| :-- | :--- | :--- |
| M4-D1 | **Penalty settlement is a new service**, manager-only, no schema change | `PenaltyService(+Impl)` + `PenaltyMessages`; the model's `markPaid`/`waive` stay the only writers (INV-3/5) |
| M4-D2 | **`LoanRow` / `PenaltyRow` / `ActivityEntry` are view-declared records**, built by controllers | Views keep zero service/controller imports; name/date/fee resolution lives in `controller/LoanRows` |
| M4-D3 | **One `LoansTable(Model)` + `LoanPreviewPane` shared by My Loans, All Loans, Overdue** | Three screens stay ~120–150 lines; no per-screen table copies |
| M4-D4 | **Dashboard activity is derived** from the last six borrow/return events | No persistence, no schema; rule recorded as D28 |
| M4-D5 | **My Loans shows penalty state in the preview**, not a second table | Fee breakdown + one penalty row (amount, status, waive reason) |
| M4-D6 | **Return is available from Overdue as well as All Loans** | Same preview action; documented deviation in design §5, recorded as D30 |
| M4-D7 | **`LoanService.accruedFee(loanId, asOf)` overload** | The return dialog can price the fee for the chosen return date (D21 preserved) |
| M4-D8 | **`AppController` sheds `CatalogActions`** before mounting five screens | 161 → ~118, then ~168 with all mounts; cap respected |
| M4-D9 | **Segmented control keeps plain labels** (`Active / Overdue / Returned`) | `SegmentedControl` (189) is not split; counts live in the status bar |
| M4-D10 | **Loan screens take the status sink as a constructor argument** | One `LoansController` serves three screens without three status back-pointers; `CatalogScreen` keeps its M3 `Actions.setStatus` |

### Doc deviations to record in the close-out change

- design §5: return action on Overdue (D6), penalty state in the My Loans preview
  (D5), definition of "recent activity" (D4).
- SRS §3.3/§6.2: return-date picker offers `borrowDate … T` and defaults to `T`;
  the service still rejects future/pre-borrow dates (D20).
- SRS §3.4/§6.2 step 10: the waive reason is mandatory and trimmed; settlement is
  one-way (PAID/WAIVED cannot be re-settled).
- implementation plan §2.4/§2.5 inventory: 26 new files, 8 modified.
- `HANDOFF-M4.md` becomes historical at close-out; `HANDOFF-M5.md` is written.

---

## 3. Baseline and ground truth (re-proved 2026-11-16)

| Item | State |
| :--- | :--- |
| Git | `main` @ `7f7ca1dc9fc8…`, local == `origin/main`, tree clean |
| Build | `cd desktop-app && ./build.sh` → `BUILD OK`, 131 classes, zero `-Xlint:all` warnings |
| Probes | `./test/run-all.sh` → `ALL PROBES GREEN` (9/9); `CatalogUiProbe` `PASS=13` |
| Ant | `sh ../../netbeans/extide/ant/bin/ant clean jar` → `BUILD SUCCESSFUL` (M3 close-out) |
| Inventory | 85 Java files / 7,298 lines: model 18/680, service 13/802, persistence 10/918, controller 3/317, view 40/4,540, `Main` 1/41 |
| Probes | 15 files / 1,585 lines under `desktop-app/test/` |
| Tallest | `CatalogScreen` 199, `PreviewPane` 197, `SegmentedControl` 189, `CatalogTable` 183, `Theme` 175 |

### Facts that shape the plan

- **Seed dataset** (`T` = seeding date): 3 users, 10 items (7 AVAILABLE /
  3 LOANED), 3 loans — `L-001` student1 `T-2…T+5`, `L-002` student2
  `T-6…T+1`, `L-003` student1 `T-11…T-4` (overdue, ₱200). No penalties, no
  photos.
- **Loan reads already exist**: `myLoans()`, `allLoans()`, `overdueLoans()`,
  `accruedFee(loanId)`; `LoanQueries` sorts and derives status (D1/D21).
- **Return already exists**: `LoanServiceImpl.returnLoan` enforces manager-only,
  future/pre-borrow dates, freezes ≤ 1 penalty (zero fee → none), and preserves
  `UNDER_MAINTENANCE` (D15/D20).
- **Nothing consumes `AppContext.addChangeListener` yet**; M4 is the first
  multi-screen milestone and must wire reload-on-change.
- **`PreviewPane` is equipment-typed and at 197 lines** — loan previews need a
  new pane, never an extension of this file.
- **`CatalogScreen`'s constructor calls `reload()`**, which writes the shell
  status even while another screen is shown. M4 screens must not do that: first
  status write happens in `onShown()`.

---

## 4. Architecture and data flow

```
LoginFrame ── AppController ── MainFrame (chrome, sidebar, omnibar, status)
                                 │  mount(key, screen, commands, onShown)
        ┌────────────────────────┼───────────────────────────┐
        ▼                        ▼                           ▼
  CatalogScreen          MyLoansScreen / AllLoansScreen   DashboardScreen
  (M3, unchanged)         OverdueScreen / PenaltiesScreen  (KPIs + tables)
        │                        │                           │
        │ Actions                │ Actions + StatusSink      │ Actions
        ▼                        ▼                           ▼
  CatalogActions          LoansController  PenaltyController  DashboardController
        │                        │                │                │
        └────────────┬───────────┴────────┬───────┴────────────────┘
                     ▼                    ▼
             LoanService(+Impl)     PenaltyService(+Impl)
                     └────────┬───────────┘
                              ▼
                    DataStore / AppData (JSON, schemaVersion 1)

Row building:  LoanRecord/Penalty + store lookups ──controller/LoanRows──▶
               view/LoanRow · view/PenaltyRow · view/ActivityEntry
```

Rules preserved: views import `model` and `view` only; controllers import views +
services; services mutate entities and call `DataStore.save()` last; fees are
computed only by `LoanService`/`PenaltyService` (D21).

---

## 5. API contracts

### 5.1 `kitnapper.service.PenaltyService` (new)

```java
public interface PenaltyService {
    List<Penalty> allPenalties();                 // assessed date desc, id desc
    List<Penalty> penaltiesForLoan(String loanId); // empty for unknown/blank
    List<Penalty> myPenalties();                  // session borrower's loans only
    double unpaidTotal();                         // Σ amount where UNPAID
    void settle(String penaltyId, PenaltyOutcome outcome, String reason);
}
```

`settle` is manager-only (INV-3); `PAID` calls `Penalty.markPaid()`, `WAIVED`
calls `Penalty.waive(reason)` (model rejects blank); both then `save()` and
`fireChanged()`. Unknown id → `IllegalArgumentException`; already settled /
borrower session → `IllegalStateException` with a `PenaltyMessages` string.
`myPenalties()` derives the borrower scope from `LoanService.myLoans()` ids — no
new query shape.

### 5.2 `kitnapper.service.PenaltyServiceImpl` (new)

Holds `DataStore`, `Supplier<User> session`, `Runnable onChange`, and a
`LoanQueries` for the session borrower's loans. `PenaltyMessages` (new,
package-private) carries: manager-only, missing penalty, already settled, reason
required. `Penalty` accessors supply the rest. Expected size ~100 lines; if it
passes 150, split a package-private `PenaltyQueries` for reads/sorting.

### 5.3 `LoanService` overload (modified)

```java
/** Live/frozen fee as of an explicit date (return dialog preview, D7). */
double accruedFee(String loanId, LocalDate asOf);
```

`LoanServiceImpl` delegates to `LoanQueries.accruedFee(loanId, asOf, strategy)`;
the existing one-argument method passes `today()`. `LoanQueries` grows the
three-argument form (+8 lines).

### 5.4 Controllers (new)

**`controller/LoanRows`** (package-private helper, ~110): resolves a
`LoanRecord` to a `view.LoanRow` and a `Penalty` to a `view.PenaltyRow` using the
store's users/equipment, `LoanService.effectiveStatus` inputs
(`context.today()`), `daysOverdue`, and `accruedFee(loanId)`; also builds the
dashboard activity list. Single place where ids become names.

**`controller/LoansController`** (~110) implements `MyLoansScreen.Actions`,
`AllLoansScreen.Actions`, `OverdueScreen.Actions`:

```java
List<LoanRow> myLoans(LoanStatus tab);   // borrower scope, filtered by effective status
List<LoanRow> allLoans();                // borrow date desc (LoanQueries order)
List<LoanRow> overdueLoans();            // longest overdue first
List<LocalDate> returnDates(LoanRow row); // borrowDate … today, ascending
Optional<String> returnLoan(LoanRow row, LocalDate returnDate); // delegates BorrowingController
void setOnChanged(Runnable listener);
```

`returnDates` returns at least one date (today) even on clock skew; the dialog
defaults to the last entry.

**`controller/PenaltyController`** (~90) implements `PenaltiesScreen.Actions`:
`List<PenaltyRow> penalties()`, `Optional<String> settle(PenaltyRow,
PenaltyOutcome, String)`, `setOnChanged(Runnable)`. Translates
`UncheckedIOException` to the existing save-failure string and other runtime
failures to their message.

**`controller/DashboardController`** (~90) implements `DashboardScreen.Actions`:
`int loansOut()`, `int overdueCount()`, `int unpaidCount()`, `String
unpaidTotal()`, `List<LoanRow> overdueLoans()`, `List<ActivityEntry> activity()`.
Counts come from `LoanService`/`PenaltyService`; no entity math in the screen.

**`controller/CatalogActions`** (extracted from `AppController`, ~60): the M3
`CatalogScreen.Actions` anonymous class, unchanged in behavior, constructed with
`(CatalogController, BorrowingController, BiConsumer<String,String> status)`.
`AppController` keeps session/routing/theme only.

### 5.5 View-facing contracts (new records)

```java
public record LoanRow(String loanId, String itemName, EquipmentCategory category,
    String borrowerName, LocalDate borrowDate, LocalDate dueDate,
    LocalDate returnDate, // null when out
    LoanStatus status, long daysLate, double accruedFee, PenaltyRow penalty) {}

public record PenaltyRow(String penaltyId, String loanId, String itemName,
    String borrowerName, double amount, PenaltyStatus status,
    LocalDate assessedDate, String waiveReason) {} // null unless waived

public record ActivityEntry(LocalDate date, String loanId, String itemName,
    String borrowerName, boolean checkout, double fee) {}
```

`LoanRow.category` was added in WP2: §8.2 needs the item's category for the
preview tile, and views cannot look it up through a service. `LoanRows`
resolves it from the same `Equipment` lookup it already does for the name,
falling back to `OTHER` when the item is missing.

Screens declare their own `Actions`; the status sink is a constructor
`BiConsumer<String,String>` (M4-D10):

```java
public interface Actions {                       // MyLoansScreen
    List<LoanRow> myLoans(LoanStatus tab);
}
public interface Actions {                       // AllLoansScreen / OverdueScreen
    List<LoanRow> allLoans();
    List<LoanRow> overdueLoans();
    List<LocalDate> returnDates(LoanRow row);
    Optional<String> returnLoan(LoanRow row, LocalDate returnDate);
}
public interface Actions {                       // PenaltiesScreen
    List<PenaltyRow> penalties();
    Optional<String> settle(PenaltyRow row, PenaltyOutcome outcome, String reason);
}
public interface Actions {                       // DashboardScreen
    int loansOut();
    int overdueCount();
    int unpaidCount();
    String unpaidTotal();
    List<LoanRow> overdueLoans();
    List<ActivityEntry> activity();
}
```

Each screen also exposes `reload()`, `onShown()`, `refreshTheme()`. `reload()`
never writes status during construction; `onShown()` does.

### 5.6 Shared view APIs

| Type | Change | Notes |
| :--- | :--- | :--- |
| `LoansTableModel` | new, ~140 | `Mode` enum `MY`, `ALL`, `OVERDUE`; typed columns (String, `LoanStatus`, Long, Double, `LocalDate`); snapshot replace |
| `LoansTable` | new, ~150 | copies the `CatalogTable` skin (non-opaque, hover painted **before** `super.paintComponent`), `TableRowSorter` with date/money comparators, `setItems`, `setOnSelect`, `selectRow(loanId)`, `inScrollPane()` |
| `LoanPreviewPane` | new, ~190 | 280 px `BG_ELEVATED` pane: `GradientTile` media, title, status badge, borrower/borrow/due/returned/days-late rows, fee breakdown block, pinned `Return` action + hint, empty state |
| `PenaltyPreviewPane` | new, ~150 | 280 px pane: amount, loan/item/borrower, assessed date, status badge, waive reason; pinned `Mark paid` + `Waive` actions |
| `StatusBadges` | new, ~90 | `loanStatus(LoanStatus)`, `penaltyStatus(PenaltyStatus)`, plus painted forms for table cells; reads `Badges`' pill geometry via a new package-private `Badges.paintPill(...)` (+10 lines, `Badges` stays ~140) |
| `TableCells` | modified, +30 | `loanStatus()` / `penaltyStatus()` renderers delegating to `StatusBadges` |
| `DatePicker` | modified, +10 | new `(List<LocalDate>, int defaultIndex, String accessibleName)` constructor; the one-argument M3 form delegates with `+3` / `"Due date"` |
| `KpiCard` | new, ~80 | label, value (32 px mono), sub-caption, `BG_SURFACE` + border |
| `ActivityList` | new, ~90 | vertical list of `ActivityEntry` rows (glyph, text, mono date); empty state |

### 5.7 Status-bar aggregates

| Screen | Left | Right |
| :--- | :--- | :--- |
| My Loans | `N loans · M overdue` (borrower scope) | Σ live accrued of outstanding loans |
| All Loans | `N loans · M overdue` | Σ live accrued of outstanding loans |
| Overdue | `N overdue · M borrowers` | Σ accrued of the overdue rows |
| Penalties | `N penalties · M unpaid` | Σ UNPAID amounts |
| Dashboard | `N loans · M overdue` | Σ UNPAID amounts |

Money always through `Formats.money`; counts are plain strings built in the
screen (no business rule).

---

## 6. File inventory

Line estimates are sanity checks, not budgets. All paths under
`desktop-app/src/kitnapper/`.

### 6.1 New — service (3 files, ~160 lines)

| File | Contents | Est. |
| :--- | :--- | --: |
| `service/PenaltyService.java` | interface per §5.1 | 30 |
| `service/PenaltyServiceImpl.java` | manager-only settle, reads, sorting, save + notify | 100 |
| `service/PenaltyMessages.java` | user-facing settlement strings | 30 |

### 6.2 New — controller (5 files, ~440 lines)

| File | Contents | Est. |
| :--- | :--- | --: |
| `controller/LoanRows.java` | id → name/status/fee/penalty resolution, activity list | 110 |
| `controller/LoansController.java` | three loan-screen Actions + return dates | 110 |
| `controller/PenaltyController.java` | penalty rows + settle | 90 |
| `controller/DashboardController.java` | KPI counts, overdue rows, activity | 90 |
| `controller/CatalogActions.java` | M3 catalog actions extracted from `AppController` | 60 |

### 6.3 New — view (18 files, ~1,950 lines)

| File | Contents | Est. |
| :--- | :--- | --: |
| `view/LoanRow.java`, `view/PenaltyRow.java`, `view/ActivityEntry.java` | data records per §5.5 | 40 / 35 / 25 |
| `view/LoansTableModel.java` | three column sets over `LoanRow` | 140 |
| `view/LoansTable.java` | shared skinned, sortable table | 150 |
| `view/LoanPreviewPane.java` | loan details + fee breakdown + return action | 190 |
| `view/PenaltyPreviewPane.java` | penalty details + settle actions | 150 |
| `view/StatusBadges.java` | loan + penalty pills | 90 |
| `view/ReturnDialog.java` | date picker `borrowDate…T`, live fee, confirm | 120 |
| `view/SettlePenaltyDialog.java` | PAID/WAIVE choice, required reason, inline error | 110 |
| `view/MyLoansScreen.java` | tabs + table + preview | 150 |
| `view/AllLoansScreen.java` | table + preview + return | 140 |
| `view/OverdueScreen.java` | table + preview + return | 120 |
| `view/PenaltiesScreen.java` | table + preview + settle | 130 |
| `view/PenaltiesTableModel.java` | `PenaltyRow` columns | 90 |
| `view/DashboardScreen.java` | KPI row + overdue table + activity | 140 |
| `view/KpiCard.java` | stat card | 80 |
| `view/ActivityList.java` | recent events list | 90 |

### 6.4 Modified (8 files, ~+70 net)

| File | Change |
| :--- | :--- |
| `service/LoanService.java` | `accruedFee(loanId, asOf)` overload |
| `service/LoanServiceImpl.java` | delegate the overload |
| `service/LoanQueries.java` | three-argument fee query |
| `service/AppContext.java` | `penalties()` getter + construct `PenaltyServiceImpl` |
| `controller/AppController.java` | extract `CatalogActions`, mount five screens, theme-refresh list |
| `view/Badges.java` | package-private `paintPill(...)` reused by `StatusBadges` |
| `view/TableCells.java` | loan/penalty status renderers |
| `view/DatePicker.java` | range/default/label constructor |

Not modified: `MainFrame` (mount API is sufficient), `ScreenCatalog` (routes and
subtitles already exist), persistence (schema stays 1).

### 6.5 Line-cap strategy

| Risk | Mitigation (apply before exceeding 200) |
| :--- | :--- |
| `LoanPreviewPane` 190 → preview detail + action bar | extract `LoanFeeBreakdown` (~70) if it passes 185 |
| `LoansTable` 150 + sorter/renderers | extract `LoanColumnSkins` if it passes 185 |
| `MyLoansScreen` 150 + tabs + empty states | extract `MyLoansPresenter` (mirrors `CatalogPresenter`) |
| `AppController` 168 | extract `ScreenMounts` (~90) if it passes 185 |
| `Badges` 140 | no further badges in M4; M5 uses the same pill helper |
| `TableCells` 160 | renderers are one-liners; fine |

### 6.6 Totals after M4

111 Java files / ~9,900 lines (85 + 26 new; +~70 modified), 17 probe files.
New view files stay at or under 150 except the two preview panes, which follow
`PreviewPane`'s documented 190-line shape.

---

## 7. Work packages (execution order)

One WP per turn. Each ends with the WP's verification, a §17 log entry, and a
commit (documented-task rule).

### WP0 — Preflight + plan (DONE 2026-11-16)

- Re-proved `./build.sh` (131 classes) and `test/run-all.sh` (9/9).
- Recorded the probe-harness concurrency flake in the handoff gotchas.
- Wrote this plan; committed `docs(m4)`.

**Gate:** build + probes green, plan committed.

### WP1 — Data contracts, penalty service, controllers, `LoanProbe`

Files: §6.1 (3), §6.2 (5), §6.3 records (3), modified
`LoanService(+Impl)`, `LoanQueries`, `AppContext`; new
`test/kitnapper/LoanProbe.java` (+ `PenaltyChecks`/`LoanChecks` helpers if the
probe passes 200 lines).

Steps: records → `LoanRows` → `PenaltyService(+Impl)` + messages → fee overload
→ controllers → `AppContext` wiring → `LoanProbe`; add it to `run-all.sh`
(headless).

**Gate:** `./build.sh` clean, `LoanProbe` green (≥ 30 checks, counts recorded),
M3 probes still green, no view compilation (no Swing yet beyond records).

### WP2 — Shared loan UI

Files: `LoansTableModel`, `LoansTable`, `LoanPreviewPane`,
`PenaltyPreviewPane`, `StatusBadges`; modified `Badges`, `TableCells`,
`DatePicker`.

**Gate:** build clean; a temporary paint smoke run inside `LoansUiProbe`
scaffolding (row rendering, preview fields, dark/light non-blank) passes;
`PASS` count recorded; all files ≤ 200 (expected max `LoanPreviewPane` ~190).

### WP3 — My Loans, All Loans, return logging (DONE 2026-11-16)

Files: `MyLoansScreen`, `AllLoansScreen`, `ReturnDialog`; modified
`AppController` (extract `CatalogActions`, mount borrower/manager screens,
theme list). Status sink wired.

**Gate:** manual SRS step 9 path in the probe (return from All Loans freezes one
penalty, item returns AVAILABLE, Overdue empties); build + suite green; manager
and borrower navigation land on the right default screen.

**Result (2026-11-16):** gate met — `LoansUiProbe` `PASS=39 FAIL=0` including the
return from All Loans (one ₱200 UNPAID penalty, `e-007` AVAILABLE, Overdue
empty, status `3 loans · 0 overdue`); suite 10/10; AppController wiring smoked
for manager and borrower sessions. Delivered with three logged deviations: a
shared `LoansTableSection`, `controller/LoansActions`, and `feeOn` on the loan
actions. See §17.

### WP4 — Overdue, Penalties, settlement (DONE 2026-11-16)

Files: `OverdueScreen`, `PenaltiesScreen`, `PenaltiesTableModel`,
`SettlePenaltyDialog`; `AppController` mounts; return from Overdue.

**Gate:** SRS step 10 in the probe (waive with reason updates the badge; blank
reason shows inline DANGER and keeps the dialog open); suite green.

### WP5 — Dashboard and final wiring (DONE 2026-11-16)

Files: `DashboardScreen`, `KpiCard`, `ActivityList`; `AppController` mount;
`DashboardController` wiring.

**Gate:** manager default screen shows 3 out / 1 overdue / 0 unpaid on seed; the
overdue table matches `overdueLoans()`; activity newest first; suite green.

**Result (2026-11-16):** gate met — `LoansUiProbe` `PASS=101 FAIL=0` with the
new dashboard pass (seed `Out now: 3` / `Overdue: 1` / `Unpaid penalties: 0`,
table row `L-003` at ₱200.00 / 4 days late, activity `L-001` → `L-002` →
`L-003`; after the return `2 / 0 / 1`, empty overdue table, return event above
its checkout). Suite 10/10, mount smoke 8/8, `ant clean jar` green. See §17.

### WP6 — Verification, docs, handoff (DONE 2026-11-16)

Steps: complete `LoansUiProbe` (§9.3) and add it to `run-all.sh` (display);
static audits (line caps, hex, view imports, suppressions); `ant clean jar`;
full suite (12/12); SRS §6.2 steps 7–10 by hand; docs per §11; write
`HANDOFF-M5.md` and update `.opencode/HANDOFF.md`; commits + push.

**Result (2026-11-16):** gate met — suite 12/12, `LoansUiProbe` `PASS=105`
(+`all-sort-due`, +`navigation`), manual pass 33/33 through the real
`AppController`, `ant clean jar` green, audits clean. Two defects found and
fixed (settle-dialog listener order, first-paint status line). Deviations from
§9.3 recorded in §17. See §17.

**Gate:** §14 Definition of Done.

---

## 8. UI specification

All screens share: `PageHeader`, 32 px horizontal padding, 16 px gap to the
body, 280 px preview on the right, `BG_APP` background, status bar wiring on
`onShown`.

### 8.1 Shared loans table

Columns per mode (typed model; badges rendered, money right-aligned, ids mono):

| Mode | Columns |
| :--- | :--- |
| `MY` | ID, Item, Borrowed, Due, Status, Fee |
| `ALL` | ID, Item, Borrower, Borrowed, Due, Returned, Status, Fee |
| `OVERDUE` | ID, Item, Borrower, Due, Days late, Accrued fee |

40 px rows, 36 px header, sort chevrons, hover `HOVER_ROW` painted before rows,
selection `ACCENT_TINT`, single selection, no vertical gridlines. Default sort:
`ALL`/`MY` borrow date desc, `OVERDUE` days late desc (service order); clicking a
header re-sorts.

### 8.2 Loan preview pane

Empty state: "Select a loan to see details." Selected:

1. `GradientTile` for the item's category (16:9, 248 px).
2. Item title (16 px semibold) + status badge.
3. Rows: Borrower, Borrowed, Due, Returned ("—" while out), Days late,
   ID.
4. Fee block: "Accrued now" `₱X` for outstanding loans; "Frozen penalty"
   `₱X · UNPAID/PAID/WAIVED` once returned (waive reason beneath when present).
5. Pinned action: `Log return` (All Loans, Overdue) enabled only while the loan
   is outstanding; hint otherwise ("Already returned on yyyy-MM-dd.").

My Loans uses the same pane with the return action hidden and the penalty row
shown (M4-D5).

### 8.3 My Loans (borrower)

- Omnibar commands: `SegmentedControl("Active", "Overdue", "Returned")` (plain
  labels, M4-D9), default **Active**; changing tabs re-queries the controller.
- Table mode `MY`; preview to the right; empty states per tab ("No active loans.
  Browse the catalog.").
- Status bar: `N loans · M overdue` / Σ accrued of outstanding loans.

### 8.4 All Loans (manager)

- No commands beyond the omnibar search field (inert here; Ctrl+F still focuses
  it, acceptable and unchanged).
- Table mode `ALL`, sortable; preview with `Log return` (§8.8).
- Empty state only if the dataset is empty.

### 8.5 Overdue (manager)

- Table mode `OVERDUE`; longest overdue first; days-late column.
- Same preview with `Log return` (M4-D6); returning the last overdue loan swaps
  the table for "Nothing is overdue. Good." (success empty state, no action).

### 8.6 Penalties (manager)

- Table: Penalty ID, Loan, Item, Borrower, Amount, Status, Assessed.
- Preview (§5.6): amount block, loan/item/borrower, assessed date, status badge,
  waive reason; pinned `Mark paid` (primary) and `Waive…` (secondary) for
  UNPAID rows; no actions once settled.
- Empty state: "No penalties yet. Late fees appear here when an overdue loan is
  returned."

### 8.7 Dashboard (manager)

- KPI row: three `KpiCard`s — "Out now" (`loansOut`), "Overdue"
  (`overdueCount`), "Unpaid penalties" (`unpaidCount` + `unpaidTotal` caption).
- Layout: the cards form a 96 px-tall row on the 16 px grid; below it the
  overdue table fills the left and a fixed 280 px "Recent activity" rail sits
  on the right (the preview-pane width). Activity rows are two-line — muted
  `yyyy-MM-dd` above the sentence, 1 px `BORDER_SUBTLE` hairline between rows.
- Overdue table (mode `OVERDUE`, no preview) titled "Overdue now"; clicking a
  row navigates to Overdue? **No** — M4 keeps it read-only; navigation is the
  sidebar (no new routing surface).
- Activity list: last six events, newest first — each loan contributes a
  checkout at `borrowDate` and, once returned, a return at `returnDate`; ties
  break by loan id desc; text "Student One checked out Sony A7 IV" /
  "… returned Rode Wireless PRO kit · ₱200.00 fee" (the fee suffix appears only
  when the return froze a fee).
- Empty dashboard state: all three counters zero → the activity list shows "No
  lending activity yet."

### 8.8 Return dialog (480 px)

Body: item + borrower + borrowed/due + status badge + days late; a `DatePicker`
over `borrowDate … today` (default today, label "Return date"); a live line
"Fee to freeze: ₱X" recomputed on selection through
`LoanService.accruedFee(loanId, date)` (never in the view); note "No fee is
frozen when the return is on time." Footer `Cancel` / `Log return` (primary).
On confirm: service call; a rejection keeps the dialog open with the DANGER line
(blank reason equivalent does not apply); success toasts
"Returned <item> · fee ₱X" (or "· no fee") and reloads every mounted screen.
Return is not destructive, so no scrim confirmation beyond this dialog.

### 8.9 Settle penalty dialog (480 px)

Body: amount, loan/item/borrower, assessed date, current status; a
`SegmentedControl("Mark paid", "Waive")`; a reason field enabled only for Waive
(required; trimmed; inline DANGER when blank). Footer `Cancel` / `Confirm`
(primary; destructive styling when Waive). The dialog itself is the
confirmation required by design §7. Success toasts "Penalty p-001 marked paid" /
"Penalty p-001 waived".

### 8.10 Toasts and theme

- Success toasts only; failures stay inline in dialogs (M3 pattern).
- `refreshTheme()` on every screen re-applies `UiFactory.refresh(this)`, table
  scroll backgrounds, preview panes, and `KpiCard`/`ActivityList` painters;
  `AppController` keeps a `List<Runnable> refreshers` and runs them all.

---

## 9. Probe plan

Probes live under `desktop-app/test/` and run through `test/run-all.sh`.
Plain `main` classes with `check(name, ok)` counters; no framework. Always run
the suite **serially** — the harness compiles into `test/out` and is not
concurrency-safe.

### 9.1 Harness updates

- Add `LoanProbe` (headless) and `LoansUiProbe` (display) to `run-all.sh`.
- `LoansUiProbe` goes in package `kitnapper.view` (package-private screens) and
  reuses `UiProbeSupport`; it must not depend on `/tmp` screenshots.
- Each probe prints `PASS=n FAIL=0` on success; the runner asserts that shape.

### 9.2 `LoanProbe` — returns, settlement, persistence, controller rows

`AppContext(DataStoreImpl(tempDir), () -> T)` with `T = 2026-11-14`.

Seed and reads:

1. `allLoans().size() == 3`; `myLoans()` student1 = 2, student2 = 1.
2. `overdueLoans()` = `[L-003]`, `accruedFee(L-003) == 200`, `daysOverdue == 4`.
3. `allPenalties()` empty; `unpaidTotal() == 0`.

Returns:

4. Manager returns `L-003` at `T` → status RETURNED, `e-007` AVAILABLE, exactly
   one penalty `p-001` of ₱200 UNPAID.
5. Second return → rejected (`ALREADY_RETURNED`), penalty count unchanged.
6. Return `L-001` at `T+1` → rejected (future), loan still ACTIVE.
7. Return at `borrowDate - 1` → rejected.
8. Zero fee: student2 returns `L-002` at `T` (due `T+1`) → no penalty.
9. D15: flag `e-001` UNDER_MAINTENANCE while `L-001` is outstanding, return at
   `T` → loan RETURNED, item stays UNDER_MAINTENANCE.
10. Frozen fee: after return, `accruedFee(L-003)` is still ₱200.
11. Borrower-session return → rejected (`MANAGER_ONLY`).

Settlement:

12. Manager marks `p-001` PAID → status PAID; `unpaidTotal() == 0`.
13. Re-settle PAID → rejected; status stays PAID.
14. Waive with blank reason → rejected; status stays UNPAID.
15. Waive with "First offense" → WAIVED and reason preserved.
16. Borrower settle → rejected (`MANAGER_ONLY`).
17. Settle unknown id → rejected (`PENALTY_MISSING`).
18. Save + fresh `DataStoreImpl(dir).load()` → loan RETURNED, penalty WAIVED with
    reason, `e-007` AVAILABLE.

Fee overload and queries:

19. `accruedFee(L-003, T-1) == 150`; `(L-003, T) == 200`; `(L-002, T) == 0`.
20. `penaltiesForLoan(L-003)` = 1 row; unknown loan id → empty.
21. `myPenalties()` = 0 before the return, 1 after (student1).

Controllers:

22. `LoansController.myLoans(OVERDUE)` student1 = 1, `(ACTIVE)` = 1,
    `(RETURNED)` = 0; rows carry resolved item/borrower names and ₱200.
23. `returnDates(row)` first = borrow date, last = `T`, size 12 for `L-003`.
24. `returnLoan` returns empty on success and a non-empty message on a repeat.
25. `PenaltyController.penalties()` builds a row with names + UNPAID; settle
    failure returns the message, not an exception.
26. `DashboardController`: seed = 3 out / 1 overdue / 0 unpaid; after return =
    2 out / 0 overdue / 1 unpaid.

Expected: `PASS≈26`, `FAIL=0` (record the real number in §17).

### 9.3 `LoansUiProbe` — rendering and flows (display)

1. Mount My Loans in `MainFrame`; Active tab 1 row, Overdue 1, Returned 0.
2. Status left contains "2 loans"; right `₱200.00`.
3. Select `L-003` → preview item/borrower, days late 4, accrued ₱200.00, badge
   OVERDUE.
4. All Loans: 3 rows; header click on Due sorts `L-003` first.
5. Return flow: open dialog, confirm today → toast, table reload, Overdue 0
   rows, Penalties 1 row, penalty preview ₱200 UNPAID.
6. Penalties: waive with a reason → badge WAIVED; table reload.
7. Blank reason → inline DANGER, dialog stays open, penalty still UNPAID.
8. Dashboard: 2 out / 0 overdue / 0 unpaid after the return; overdue table empty
   state; activity newest first (return above checkout).
9. Paint dark and light at 1280×800; assert non-background pixel counts above a
   threshold (screens are not blank) and no exception.
10. Navigation: manager sidebar 6 keys, borrower 2; defaults dashboard/catalog.

Expected: `PASS≈10`, `FAIL=0` (record exact counts).

### 9.4 Expected baseline after M4

| Probe | Expected |
| :--- | :--- |
| `DataProbe` | `PASS=26` (unchanged) |
| `CatalogProbe` | `PASS=28` (unchanged) |
| `LoanProbe` | `PASS=35` (WP1) |
| `SkinProbe` | `PASS=4`; `light_pixels=260` |
| `EdgeProbe` | `PASS=4` |
| `SizeProbe` | `PASS=3` |
| `FontProbe` | `PASS=2` |
| `FrameProbe` | `PASS=12` |
| `CloseProbe` | `PASS=1` + `EXITED_OK` |
| `CatalogUiProbe` | `PASS=13` |
| `LoansUiProbe` | `PASS=105` (WP6, registered) |

---

## 10. Manual verification

### 10.1 SRS §6.2 (M4 slice)

1. Log in as `student1` → Catalog; open **My Loans**: L-001 active, L-003
   overdue, no returned rows (FR-05).
2. Log in as manager → **Dashboard** shows 3 out, 1 overdue, 0 unpaid, with
   `L-003` first (₱200.00, 4 days late) (FR-08, NFR-05).
3. **All Loans** lists all three loans, newest first (FR-09).
4. Log the return of `L-003` with return date `T` → row becomes RETURNED,
   Dashboard overdue empty, **Penalties** gains exactly one ₱200.00 UNPAID fee,
   `e-007` AVAILABLE (FR-06/07, D15/D20).
5. **Waive** `p-001` with a reason → status updates immediately, reason visible
   in the preview (FR-11, NFR-02).
6. Close and relaunch, log in as manager → the return and the waiver persist
   (FR-12, NFR-07).
7. Return a due-today loan (`L-002`) → no penalty appears (FR-07 acceptance).
8. Check out, flag maintenance, return → item stays UNDER_MAINTENANCE (D15).

### 10.2 Visual and behavior checks

- 1280×800 and 1024×640: tables readable, preview 280 px, no clipped action
  labels; note any clipping as M6 (do not chase it in M4).
- Theme toggle repaints all five screens (tables, previews, KPI cards).
- Keyboard: tabs/table reachable by Tab; dialog Enter confirms, Esc cancels.
- No `AWTTreeLock` deadlocks (all UI work on the EDT).
- Toast text names the item/penalty and the amount where applicable.

### 10.3 Notes

- The probe clock is pinned; the app uses the real date (D4). A manual run the
  day after seeding shows 5 days late — values shift, behavior does not.
- The 1024×640 preview clipping and toast overlap remain M6 items (design §9).

---

## 11. Documentation updates (same change as behavior)

| File | Update |
| :--- | :--- |
| `docs/implementation-plan.md` | revision **1.9.0**: M4 row marked delivered with its files; §2.4/§2.5 inventory; decisions **D28–D31**; totals |
| `docs/SRS.md` | 3.1.3: return-date picker bounds + default, waive reason required, My Loans penalty display; §6.2 steps 8–10 wording; revision row |
| `docs/design.md` | §5: return from Overdue, penalty state in My Loans preview, dashboard activity definition; §6 table/preview notes if needed |
| `docs/decisions.md` | §9: M4 entry — what shipped, probe numbers, fixes made during the pass |
| `README.md` | documentation table (M4 plan → record, `HANDOFF-M5.md` next), status paragraph, probe list (11) |
| `HANDOFF-M4.md` | header note: superseded as the contract by this plan; historical after close-out |
| `HANDOFF-M5.md` | new: M5 scope (inventory, photos, reset, settings), reuse map, gotchas, commands |
| `.opencode/HANDOFF.md` | local: new `NEXT`, `STATE`, `FILES`, gotchas (untracked) |

New decisions to record:

- **D28** — Dashboard "recent activity" = last six borrow/return events derived
  from loans (date desc, loan id desc); no persistence.
- **D29** — Loan/penalty rows are view records built from a single
  `controller/LoanRows` helper; controllers resolve names, views format.
- **D30** — Return is reachable from All Loans and Overdue (NFR-01) — deviation
  from design §5's single mention.
- **D31** — Penalty settlement is one-way, manager-only, reason-mandatory for
  waive; `PenaltyService` is the only caller of `markPaid`/`waive`.

---

## 12. Git plan

Documented tasks commit and push without asking (`preferences.md` §3); one
logical change per commit; both trailers and SSH signing on every commit.

| # | Commit | Content |
| :-- | :--- | :--- |
| 1 | `docs(m4): add the M4 implementation plan` | this file + README/handoff pointers (WP0) |
| 2 | `feat(m4): add penalty settlement and loan row contracts` | WP1 service + controllers + records + `AppContext`/fee overload |
| 3 | `test(m4): probe return and settlement rules` | `LoanProbe` + runner entry (WP1) |
| 4 | `feat(m4): add the shared loan table and preview panes` | WP2 |
| 5 | `feat(m4): add My Loans and All Loans with return logging` | WP3 + `CatalogActions` extraction |
| 6 | `feat(m4): add Overdue and Penalties with settlement` | WP4 |
| 7 | `feat(m4): add the manager Dashboard` | WP5 |
| 8 | `test(m4): add the loan screens UI probe` | WP6 probe |
| 9 | `docs(m4): record M4 completion and hand off M5` | WP6 docs + `HANDOFF-M5.md` |

Push after each commit, then report local SHA, remote SHA, and `verified`.

---

## 13. Risks and mitigations

| Risk | Likelihood | Mitigation |
| :--- | :--- | :--- |
| Preview pane or table passes 200 lines | High | Split points named in §6.5; check size before each edit |
| `AppController` exceeds the cap while mounting | Medium | Extract `CatalogActions` first (WP3); `ScreenMounts` if still tight |
| Change listeners cause reload storms / stale selections | Medium | Small dataset; screens keep selection by id and re-apply; one listener per controller, not per screen |
| Return dialog shows a fee that differs from the frozen one | Low | Fee comes from the same `LoanService.accruedFee(loanId, asOf)` the service uses |
| Probe flake from parallel runs | Medium | Documented: run `run-all.sh` serially; no build in parallel |
| `LoansTable` duplicates `CatalogTable` skin | Accepted | M6 follow-up to generalise; M4 favors a working table over a risky refactor |
| Dashboard activity surprises (event definition) | Low | D28 fixes the rule; the probe asserts ordering |
| Status bar written during construction | Low | `reload()` is silent; `onShown()` writes |

---

## 14. Definition of Done

1. `cd desktop-app && ./build.sh clean && ./build.sh` → `BUILD OK`, zero warnings.
2. `sh ../../netbeans/extide/ant/bin/ant clean jar` → `BUILD SUCCESSFUL`.
3. `./test/run-all.sh` → **12/12 green**, counts match §9.4.
4. SRS §6.2 steps 7–10 pass by hand; §10.1 extras pass.
5. All Java files ≤ 200 lines (`find … | xargs wc -l` max reported).
6. No hex outside `Theme` (transparent-window exceptions aside); no
   `service`/`controller` imports under `view`; no `@SuppressWarnings`.
7. No entity mutator called from a view or controller outside the services.
8. Docs §11 updated in the same change; `HANDOFF-M5.md` written.
9. Commits signed, trailers present, remote SHA verified, GitHub `verified`.
10. Build output, runtime files, `AGENTS.md`, `.opencode/` never staged.

---

## 15. Out of scope (do not do in M4)

- Inventory CRUD, photo import, `ImageStore`, `PhotoTile`, skeletons, reset demo
  data, Settings (M5 — D27).
- Thin scrollbars, Fluent motion, DPI/contrast passes, 1024×640 preview fixes
  (M6).
- Any schema change; `schemaVersion` stays 1.
- Loan cancellation, due-date extension, borrower-side settlement.
- Rewriting M3's catalog behavior beyond extracting `CatalogActions`.
- Generalising `CatalogTable` into a shared superclass (M6).

---

## 16. Follow-ups after M4

- Generalise the table skin (`CatalogTable`/`LoansTable` duplication).
- Preview clipping and toast overlap at small sizes (M6).
- Overdue row → Overdue screen navigation from the Dashboard (nice-to-have).
- Borrower dashboard parity (not in scope; borrowers keep Catalog + My Loans).

---

## 17. Execution log

### WP0 — Plan authored and preflight re-proved (2026-11-16)

Starting HEAD `7f7ca1dc9fc8…`; tree clean; local == `origin/main`.

Commands and results:

```bash
cd desktop-app && ./build.sh          # BUILD OK -> build/classes (131 classes)
./test/run-all.sh                     # ALL PROBES GREEN (9/9)
```

Facts recorded: 85 Java files / 7,298 lines (model 18/680, service 13/802,
persistence 10/918, controller 3/317, view 40/4,540, Main 1/41); probes 15 files
/ 1,585 lines; max file `CatalogScreen` 199.

Observed once: a probe run started concurrently with another shell command in
`desktop-app/` failed with missing probe classes (`NoClassDefFoundError` on
`SizeProbe$1`, `FrameProbe` not found); a serial re-run was green. Recorded as a
gotcha: `test/run-all.sh` wipes and recompiles `test/out`, so never run it
concurrently with a build.

Decisions confirmed with the maintainer: dashboard activity derived (M4-D4),
penalty state in the My Loans preview (M4-D5), return from Overdue (M4-D6), one
commit per work package (git plan §12).

Plan committed as `docs(m4): add the M4 implementation plan` (SHA in the
handoff note).

### WP1 — Data contracts, penalty service, controllers, `LoanProbe` (2026-11-16)

Starting HEAD `909cb1f` (the plan commit); tree clean; local == `origin/main`.

Commands and results:

```bash
cd desktop-app && ./build.sh      # BUILD OK -> build/classes (141 classes)
./test/run-all.sh                 # ALL PROBES GREEN (10/10)
# LoanProbe alone: PASS=35 FAIL=0 (headless)
```

Delivered (plan §5–§7 WP1): view records `LoanRow` / `PenaltyRow` /
`ActivityEntry`; `PenaltyService` / `PenaltyServiceImpl` / `PenaltyMessages`
(manager-only, one-way, reason-mandatory settle; reads reuse the
`LoanQueries` shapes, no new query); fee overload `accruedFee(loanId, asOf)`
through `LoanService` / `LoanServiceImpl` / `LoanQueries`; controllers
`LoanRows` / `LoansController` / `PenaltyController` / `DashboardController`
(rows + reload-on-change listeners, no screen Actions yet — attached in
WP3–WP5); `AppContext.penalties()`. `LoanProbe` (+ `LoanChecks`,
`DashboardChecks`, `PenaltyChecks`, `ProbeSupport.signIn`) wired into
`run-all.sh` headless: 35 named checks, `PASS=35 FAIL=0`.

Realities vs plan §9.2 (counts recorded, ~26 → 35):

- The blank-reason, mark-paid and waive-success checks each need an UNPAID
  penalty, so one frozen fee cannot cover all three. The probe advances the
  pinned clock once (T → T+3) and returns L-002 to freeze a second fee
  (p-002, ₱100); p-001 is then marked PAID and p-002 waived with
  "First offense". No checkout was needed.
- Dashboard states hit: seed 3/1/0 `₱0.00`; after L-003's return 2/0/1
  `₱200.00` (plan check 26); after L-001's zero-fee return (D15) 1/0/1.
- Same-day return ties in the activity list: loan id desc puts L-003 above
  L-001; a documented final tiebreak puts a loan's return above its checkout
  on the same day (`LoanRows.activity()`).

Audits: no file over 200 (max `CatalogScreen` 199; largest new src file
`LoanServiceImpl` 152 after the overload; largest new test file
`LoanChecks` 154); no hex outside `Theme` (two known javadoc `{@link #...}`
regex false positives in `LoanRecord` / `AppContext`); no
`service`/`controller` imports under `view`; no `@SuppressWarnings`.
Inventory: 95 Java files / 7,916 lines (model 18, service 16,
persistence 10, controller 7, view 43, `Main` 1); 19 probe files /
2,002 lines.

Decisions taken during the pass (promote with D28–D31 at WP6): the
`PenaltyMessages` set (manager-only / missing / already settled / reason
required), and null waive reason normalized to the blank-reason rejection
(`IllegalArgumentException`, not the model's NPE).

Committed: `feat(m4): add penalty settlement and loan row contracts`,
`test(m4): probe return and settlement rules`.

### WP2 — Shared loan UI (2026-11-16)

Starting HEAD `bc9ff3c` (WP1 close-out); tree clean; local == `origin/main`.

Commands and results:

```bash
cd desktop-app && ./build.sh      # BUILD OK -> build/classes (158 classes)
./test/run-all.sh                 # ALL PROBES GREEN (10/10)
xvfb-run -a java -cp "build/classes:test/out" kitnapper.view.LoansUiProbe
                                  # PASS=33 FAIL=0
```

Delivered (plan §5.6, §8.1–§8.2): `LoansTableModel` (three column sets, typed
values, per-column renderer kind and width), `LoansTable` (the `CatalogTable`
skin, hover band, typed sorter, silent selection, selection kept by loan id),
`StatusBadges` (loan/penalty pills built on the extracted
`Badges.paintPill` / `pillWidth`), `LoanPreviewPane` + `LoanFeeBlock` (the pane
passed 200 lines on the first draft, so §6.5's split applied), and
`PenaltyPreviewPane`; `TableCells` gained one pill cell with equipment, loan
and penalty painters plus the date renderer; `DatePicker` gained the
range/default/label constructor, with the one-argument form delegating to
`+3` / "Due date".

Contract change confirmed with the maintainer before coding: `LoanRow` gained
`EquipmentCategory category` because §8.2 needs the item's category for the
preview tile and views cannot look it up. §5.5 updated above; `LoanRows`
resolves it from the same `Equipment` lookup it already does for the name and
falls back to `OTHER`.

WP2 smoke (`test/kitnapper/view/LoansUiProbe`, not yet in `run-all.sh` — WP6
registers it): seed at `T=2026-11-14`; ALL table 3 rows / 8 columns; L-003
status cell OVERDUE and fee cell `₱200.00`; preview item, borrower, 4 days
late, `₱200.00`, OVERDUE badge, visible `Log return`; returning L-003 flips it
to RETURNED with the frozen `₱200.00 · Unpaid` line and hint; penalty pane
shows `L-003`, amount, UNPAID badge and both actions; waiving with "First
offense" shows the WAIVED badge and reason and hides the actions; dark and
light paints clear the non-blank floor.

Decisions taken (log only; promote with the M4 docs at WP6):

- Loan badge dots follow design §3.3's analogues: ACTIVE → Loaned, OVERDUE →
  Overdue, RETURNED → Decommissioned; words Active / Overdue / Returned.
- No initial sort key: rows arrive in service order and the chevron appears on
  the first header click (§8.1 "(service order)").
- Penalty preview empty state: "Select a penalty to see details."
- The preview's fee caption reads "Live until the loan is returned." rather
  than naming the ₱50 rate, so the UI does not restate a service rule.

Audits: no file over 200 (max `CatalogScreen` 199; new maximums
`PenaltyPreviewPane` 195, `LoanPreviewPane` 194, probe 191); no hex outside
`Theme`; no `service`/`controller` imports under `view`; no
`@SuppressWarnings`. Inventory: 101 Java files / 8,891 lines (view 49);
20 probe files / 2,193 lines.

Committed: `feat(m4): add the shared loan table and preview panes`.

### WP3 — My Loans, All Loans, return logging (2026-11-16)

Starting HEAD `66e2635` (WP2 close-out); tree clean; local == `origin/main`.

Commands and results:

```bash
cd desktop-app && ./build.sh      # BUILD OK -> build/classes (165 classes)
./test/run-all.sh                 # ALL PROBES GREEN (10/10)
xvfb-run -a java -cp "build/classes:test/out" kitnapper.view.LoansUiProbe
                                  # PASS=39 FAIL=0
```

Delivered: `view/MyLoansScreen` (Active/Overdue/Returned `SegmentedControl`,
MY table, preview with the return action hidden, per-tab empty states, borrower
scope counts), `view/AllLoansScreen` (ALL table, preview with `Log return`,
§8.8 flow, empty state for an empty dataset), `view/ReturnDialog` (borrow date …
today, default today, live "Fee to freeze" through the controller, inline
DANGER on rejection), `view/LoansTableSection` (shared table + empty-state body),
`controller/CatalogActions` (the M3 anonymous actions, moved verbatim),
`controller/LoansActions`, `controller/AppController` (mounts the role's loan
screen, keeps a `refreshers` list), `LoansController.feeOn`.

Probe split per the WP2 note: `LoansUiChecks` holds the check bodies and
`LoansUiProbe` seeds/sessions/orchestrates. The manager pass now proves the WP3
gate end to end: select `L-003`, return at `T` through the dialog's own delegate
(`feeOn` = ₱200 today, ₱150 at `T-1`), row flips to RETURNED, hint "Already
returned on 2026-11-14.", action disabled, `overdueLoans()` empty, status
`3 loans · 0 overdue` / `₱0.00`, one ₱200 UNPAID penalty, `e-007` AVAILABLE.

Deviations confirmed with the maintainer before coding:

- `view/LoansTableSection` — the table + empty-state body is needed three times
  (MY, ALL, OVERDUE); §6.5 only sanctions splits. Overdue reuses it in WP4.
- `controller/LoansActions` — one adapter implements `MyLoansScreen.Actions` and
  `AllLoansScreen.Actions`, so `AppController` stays free of anonymous classes;
  WP4 adds `OverdueScreen.Actions` to it.
- `LoansController.feeOn(row, date)` — §8.8 needs a per-date fee the §5.5
  contracts did not expose; `ReturnDialog` takes it as a narrow lambda, matching
  `CheckoutDialog`.
- The status sink stays the plain `frame::setStatus` (maintainer's call), bound
  to the session's frame and guarded by `isDisplayable()` so screens from an
  earlier session cannot write into a new one. Last-writer-wins remains a known
  limitation for WP6/M6.

Docs in this change: SRS FR-06 acceptance plus revision **3.1.3** (front-matter
version also corrected — it still read 3.1.1 while the appendix had 3.1.2).
§11's SRS return-picker item is therefore done; WP6 covers the remaining
documentation passes.

AppController has no probe coverage, so the mount path was smoked ad hoc (temp
save, manager login → All Loans + catalog mounted, sign-out, borrower login →
My Loans): all checks OK. Not committed as a probe — WP6 owns `LoansUiProbe` in
the suite.

Audits: no file over 200 (max `CatalogScreen` 199, `PreviewPane` 197,
`PenaltyPreviewPane` 195, `LoanPreviewPane` 194); no hex outside `Theme`; no
`service`/`controller` imports under `view`; no `@SuppressWarnings`. Inventory:
107 Java files / 9,543 lines; 21 probe files / 2,281 lines.

Committed: `feat(m4): add My Loans and All Loans with return logging`.

### WP4 — Overdue, Penalties, settlement (2026-11-16)

Starting HEAD `3f018f8` (WP3 close-out); tree clean; local == `origin/main`.

Commands and results:

```bash
cd desktop-app && ./build.sh                    # BUILD OK -> build/classes (179 classes)
./test/run-all.sh                               # ALL PROBES GREEN (10/10)
xvfb-run -a java -cp "build/classes:test/out" kitnapper.view.LoansUiProbe
                                                # PASS=73 FAIL=0 (was 39 at WP3)
sh ../../netbeans/extide/ant/bin/ant clean jar   # BUILD SUCCESSFUL
```

Delivered: `view/OverdueScreen` (OVERDUE table, shared preview with `Log
return`, `N overdue · M borrowers` / Σ accrued, success empty state "Nothing is
overdue. Good." on the last return), `view/PenaltiesScreen` (penalties table,
`PenaltyPreviewPane`, settle flow, `N penalties · M unpaid` / Σ UNPAID),
`view/PenaltiesTableModel` + `view/PenaltiesTable` (mirrors the loans pair),
`view/SettlePenaltyDialog` (§8.9 body, `Mark paid`/`Waive` segmented control,
reason enabled only for Waive, confirm primary vs destructive), `controller/
LoansActions` now also implements `OverdueScreen.Actions`, `controller/
PenaltyController` implements `PenaltiesScreen.Actions`, `controller/
AppController` mounts both for managers and reloads All Loans + Overdue on one
change, `view/DialogSkin` + `view/Dialogs` take a caller-built confirm button,
`view/FlatButton.setRole` restyles it live.

Probes: new `test/.../OverdueUiChecks` and `test/.../PenaltyUiChecks`
(`LoansUiChecks` was at 195/200); the WP3 `penaltyPane` check moved into
`PenaltyUiChecks` against the real screen. `LoansUiProbe` now seeds a separate
store per pass — the Overdue pass returns `L-003` on purpose, so sharing one
store contradicted the All Loans asserts (the first run failed 5 checks before
the split). The WP4 gate is proved on the controller path: `settle(row, WAIVED,
"   ")` returns the reason-required message — the exact string `DialogSkin`
renders inline while staying open — and the penalty is still UNPAID; the §8.9
body is painted separately to prove it renders the row, with the reason field
disabled.

Deviations confirmed with the maintainer before coding:

- `view/PenaltiesTable` — §6.3 listed only the model, but §8.6 needs the same
  skinned/sortable/hover table as loans and hover is only paintable from a
  `JTable` subclass; `PenaltiesScreen` would otherwise pass its 130-line shape.
- `FlatButton.setRole` + the `DialogSkin`/`Dialogs` confirm-button overload —
  §8.9 wants Confirm primary for Mark paid and destructive for Waive, which the
  one-shot footer could not express (+12 lines across three files, all ≤ 200).
- `OverdueScreen` — own file per §6.3, built on `LoansTableSection(OVERDUE)` and
  `LoanPreviewPane` rather than a shared base class (refactoring WP3 was not in
  the plan).

Audits: no file over 200 (max `CatalogScreen` 199, `PreviewPane` 197,
`PenaltyPreviewPane` 195, `LoanPreviewPane` 194, `AppController` 183); no hex
outside `Theme`; no `service`/`controller` imports under `view`; no
`@SuppressWarnings`. Inventory: 112 Java files / 10,374 lines; 23 probe files /
2,498 lines.

AppController still has no probe coverage; the mount path was smoked ad hoc
again (manager login mounts Overdue + Penalties + All Loans + Catalog, borrower
login mounts My Loans + Catalog): 6/6 OK. WP6 owns `LoansUiProbe` in the suite.

Committed: `feat(m4): add Overdue and Penalties with settlement`.

### WP5 — Dashboard and final wiring (2026-11-16)

Starting HEAD `929e164` (WP4 close-out); tree clean; local == `origin/main`.

Commands and results:

```bash
cd desktop-app && ./build.sh                    # BUILD OK -> build/classes (185 classes)
./test/run-all.sh                               # ALL PROBES GREEN (10/10)
xvfb-run -a java -cp "build/classes:test/out" kitnapper.view.LoansUiProbe
                                                # PASS=101 FAIL=0 (was 73 at WP4)
sh ../../netbeans/extide/ant/bin/ant clean jar   # BUILD SUCCESSFUL
```

Delivered: `view/KpiCard` (the shared 8 px surface with a 12 px secondary
label, a 28 px semibold number and an optional muted caption; the accessible
name carries label and value for the probe), `view/ActivityList` (two-line
rows — muted date above the sentence, hairline separators, `EmptyState`
"No lending activity yet."), `view/DashboardScreen` (96 px KPI row, read-only
`LoansTableSection(OVERDUE)` titled "Overdue now", 280 px activity rail,
`N loans · M overdue` / Σ UNPAID), `controller/DashboardController` implements
`DashboardScreen.Actions`, `controller/ScreenMounts` (the §6.5 extraction:
catalog + role screens + `refreshers`), `controller/AppController` reduced to
session, shell, routing and theme (183 → 113 lines).

Layout confirmed with the maintainer before coding: KPI row on top, overdue
table left, activity rail right at the preview-pane width; activity rows are
two-line. `RoundedPanel` lost `final` so `KpiCard` reuses the card surface
instead of duplicating the paint (2-line diff, no behavior change).

Probes: new `test/.../DashboardUiChecks` (28 checks) as its own seeded pass —
this pass returns `L-003`, so it cannot share a store with the seed asserts
(fourth per-pass store in `LoansUiProbe`). It asserts the KPI accessible names,
the caption, the table against `controller.overdueLoans()` (id by id), the
activity sentences in tree order, the §5.7 status strings, and dark/light
non-blank paints.

AppController still has no probe coverage; the `ScreenMounts` extraction was
smoked ad hoc under xvfb by reflecting `openMain`/`signOut` (`/tmp/opencode/
MountSmoke2.java`, not committed): manager lands on the Dashboard with the KPI
row and 4 manager screens mounted, sign-out works, borrower lands on the
Catalog with My Loans mounted and no dashboard: 8/8 OK. WP6 owns
`LoansUiProbe` in the suite.

Audits: no file over 200 (max `CatalogScreen` 199; new maximums `DashboardScreen`
135, `ScreenMounts` 116, `ActivityList` 105); no hex outside `Theme`; no
`service`/`controller` imports under `view`; no `@SuppressWarnings`.
Inventory: 116 Java files / 10,722 lines; 24 probe files / 2,669 lines.

Committed: `feat(m4): add the manager Dashboard`.

### Verification pass — loans-table column clipping (2026-11-16)

Starting HEAD `bfa24cd`; tree clean; local == `origin/main`.

Found by an independent headless run of the real entry point at 1280×800: the
All Loans table (eight columns, 876 px preferred) was auto-shrunk into the
~670 px the fixed 280 px preview leaves, clipping dates (`2026-1…`), the
`OVERDUE` badge (`Overdu`) and the fee (`₱20…`); the Dashboard overdue table
clipped its Due column. My Loans and Overdue fitted that width.

Fix: `view/LoansWidthPolicy` (new, 52 lines) — the table stays
`AUTO_RESIZE_OFF` while its columns do not fit (so it scrolls horizontally) and
switches to `AUTO_RESIZE_SUBSEQUENT_COLUMNS` once the viewport can hold them,
restoring the §8.1 widths when it narrows again. `LoansTableModel` MY and
OVERDUE widths trimmed to 672/670 px so those screens stretch rather than
scroll at 1280 and 1024 stays readable.

Commands and results:

```bash
cd desktop-app && ./build.sh       # BUILD OK -> build/classes (187 classes)
./test/run-all.sh                  # ALL PROBES GREEN (11/11)
```

Probes: `test/kitnapper/view/LoansLayoutProbe` (12 checks) mounts each mode at
678 px and 1020 px and asserts no column is narrower than its widest cell, plus
the resize-mode contract. Negative control: restoring the old always-stretch
policy fails `all-narrow-mode` and `all-narrow-no-clipping`, so the probe guards
the regression.

Independent re-check (`/tmp/opencode`, not committed): 37/37 launch-and-route
checks and 44/44 data-boundary checks against this tree. All Loans now measures
76/180/130/100/100/100/100/90 at 1280 with every date, badge and amount legible,
and stretches to fill at 1600.

Audits: no file over 200 (`LoansTable` 178, `LoansWidthPolicy` 52); no hex
outside `Theme`; no `service`/`controller` imports under `view`; no
`@SuppressWarnings`.

### WP6 — Verification, docs, handoff (2026-11-16)

**Status:** complete. Starting HEAD `2e54c42` (tree clean, local = remote).
M4 closes here.

**§9.3 completion.** `LoansUiProbe` is registered in `run-all.sh` as the
twelfth probe (display mode). Two gaps in the original §9.3 list were closed in
`LoansUiChecks`:

- `all-sort-due` (§9.3.4) — toggling the Due header sorts earliest first, so
  `L-003` (due `T-4`) leads the All Loans table.
- `navigation` (§9.3.10) — `ScreenCatalog.navKeys` returns the six manager
  routes and the two borrower routes, and `defaultKey` resolves to
  dashboard/catalog. It lives in `kitnapper.view` because `ScreenCatalog` is
  package-private.

Counts: `LoansUiProbe` `PASS=105 FAIL=0` (was 101; +4 checks).

**Deviations recorded, not coded** (§9.3 items 1, 5, 9): the probe mounts each
screen in a bare `JFrame` rather than `MainFrame` (`FrameProbe` owns the shell),
the modal return/settle dialogs and their toasts are driven by the manual
harness instead of the probe (a modal `JDialog` blocks the EDT under xvfb —
§17 WP5), and the probe frames are 1180×700. §9.3 item 5's "Penalties gains one
row / preview ₱200 UNPAID" is proven by the penalties pass, which shares the
store the All Loans pass returns `L-003` on.

Commands and results:

```bash
cd desktop-app && ./build.sh clean && ./build.sh   # BUILD OK -> 187 classes, zero warnings
./test/run-all.sh                                  # ALL PROBES GREEN (12/12)
sh ../../netbeans/extide/ant/bin/ant clean jar     # BUILD SUCCESSFUL
```

| Probe | Result |
| :--- | :--- |
| `DataProbe` / `CatalogProbe` / `LoanProbe` | `PASS=26` / `28` / `35` |
| `SkinProbe` / `FontProbe` / `EdgeProbe` / `SizeProbe` | `PASS=4` (`light_pixels=260`) / `2` / `4` / `3` |
| `FrameProbe` / `CloseProbe` | `PASS=12` / `PASS=1` + `EXITED_OK` |
| `CatalogUiProbe` / `LoansLayoutProbe` | `PASS=13` / `PASS=12` |
| `LoansUiProbe` | **`PASS=105`** (registered at WP6) |

Audits: no file over 200 (`CatalogScreen` 199, `PreviewPane` 197,
`PenaltyPreviewPane` 195, `LoanPreviewPane` 194, `SegmentedControl` 189, new
`LoansUiChecks` 176); no hex outside `Theme`; no `service`/`controller` imports
under `view`; no `@SuppressWarnings`. Inventory: 117 Java files / 10,788 lines;
25 probe files / 2,811 lines.

**SRS §6.2 steps 7–10 by hand.** A throwaway harness
(`/tmp/opencode/m4-manual/ManualPass.java`, not committed) drove the real
`AppController`, `MainFrame` and the real modal dialogs on a temp-dir seed
anchored at today, capturing eight 1280×800 screenshots and **33/33 checks**:

- step 7 — student checkout, close, relaunch, log in: My Loans shows
  `3 loans · 1 overdue` with the new loan on the Active tab.
- step 8 — manager login lands on the Dashboard: `Out now: 4`, `Overdue: 1`,
  `Unpaid penalties: 0`, `L-003` first at ₱200.00 / 4 days late; All Loans 4 rows.
- step 9 — `Log return` on `L-003` through the dialog (date defaults to `T`):
  row RETURNED, exactly one ₱200.00 penalty, `e-007` AVAILABLE, overdue list
  empty, toast shown.
- step 10 — `Waive…` with a reason: badge WAIVED, reason in the preview,
  `Unpaid penalties: 0`.
- extras — the return and the waiver survive a relaunch; an early return of
  `L-002` creates no penalty; a loaned item flagged `UNDER_MAINTENANCE` stays
  flagged after its return (D15).

**Two defects found and fixed in this package:**

1. `SettlePenaltyDialog.show` styled the segments before registering the change
   listener and then set the initial index, so opening it from the preview's
   `Waive…` action arrived with Waive selected but the reason field disabled and
   the confirm button still primary (§8.9 wants a mandatory reason and a
   destructive confirm). Fixed by registering the listener first (`fix(view)`).
2. `AppController.openMain` routed to the default screen before showing the
   frame, and the status sink ignores frames without a peer, so the first
   `onShown` status was dropped: a manager's status bar read
   `10 items · 0 selected` until the next navigation. Fixed by showing the
   frame before routing (`fix(m4)`). Borrowers were masked because the catalog's
   own text matches the pre-mount placeholder.

**Carried to M6** (recorded per §10.2, not fixed in M4): the penalties table
clips its Assessed column at 1280 because `PenaltiesTable` has no
`LoansWidthPolicy` equivalent; the visible toast overlaps the preview action bar.

**Docs in this change:** `docs/SRS.md` §6.2 steps 8–10 wording + revision
**3.1.5**; `docs/design.md` §5 screen inventory + the M4 loan-screen notes;
`docs/decisions.md` §9 M4 entry with the real numbers and both fixes;
`docs/implementation-plan.md` 1.9.0 (D28–D31, M4 file inventory, 2,811 probe
lines); `README.md` (12 probes, M4 status); `HANDOFF-M5.md` (active scope note);
`HANDOFF-M4.md` (historical). Commits: `f18fc4a` (probe), `f69303f` +
`cf5a838` (fixes), the docs commit for this entry.
