# KitNapper M3 — Implementation Backbone

> Scope: milestone **M3 — Catalog + Checkout**
> Repository: `/Users/student/kitnapper`
> Baseline: `main` @ `745ae28` (`fix(ui): repair M2 field, resize, and exit bugs`);
> tree clean except untracked `HANDOFF-SESSION-2026-11-14.md`
> Written: 2026-11-14
> Status: approved plan; **no code written yet** (Plan mode)

---

## 1. Objective

Ship the first real feature milestone on top of the M2 shell:

- A working catalog for borrowers and a **read-only catalog for managers**.
- Search by name/serial, category filter, status filter, table + preview
  (default), and a cards layout toggle.
- Instant checkout with the documented limits, availability lock, persistence,
  and user-facing validation.
- The shared view infrastructure M4/M5 will reuse: badges, tables, preview pane,
  skeleton gate, photo tile, toasts, dialog skin.

M3 is **not** a loans milestone. My Loans, All Loans, Overdue, Penalties,
Inventory, and Settings remain placeholders until M4/M5.

### Milestone exit criteria

1. `ant clean jar` succeeds on JDK 17.
2. Existing M2 probe suite is still green.
3. New M3 probes prove the business rules and the rendered catalog.
4. SRS §6.2 steps 3–6 pass manually (step 6 via probe, see §10.3).
5. All Java files ≤ 200 lines; no hex outside `Theme`; no business logic in views.
6. Docs and handoff updated in the same change.
7. Build artifacts, runtime files, and `AGENTS.md` are never staged.

---

## 2. Confirmed decisions (this session)

| # | Decision | Consequence |
| :-- | :--- | :--- |
| M3-D1 | **Catalog + checkout only** | Loan *services* are completed and probed now; loan *screens* stay M4 placeholders |
| M3-D2 | **Managers get a read-only Catalog nav entry** | Manager nav becomes Dashboard, Catalog, All Loans, Overdue, Penalties, Inventory; checkout hidden for managers (INV-3) |
| M3-D3 | **Due date is a non-editable picker `T … T+14`** | Invalid dates are unreachable in the UI; SRS §6.2 step 6 is proven by a service/controller probe and documented |
| M3-D4 | **Table + preview is the default catalog view** | Cards view is opt-in via the segmented toggle; selection is shared |
| M3-D5 | **No schema change** | `schemaVersion` stays 1; only existing entities/lists are mutated |
| M3-D6 | **Service-first build order** | Business rules and probes land before any Swing screen |

### Doc deviations to record in this change

- Manager sidebar gains Catalog (design.md §5).
- M3 inventory grows by the files listed in §6 (screen split, filters, table,
  cards, DatePicker, Ids, DialogSkin).
- SRS §6.2 step 6 verification path changes to `CatalogProbe` (picker prevents
  entering a 15-day date).
- `HANDOFF-SESSION-2026-11-14.md` is untracked; decide whether to commit it with
  the docs change (recommended) or leave it local.

---

## 3. Baseline and ground truth

| Item | State |
| :--- | :--- |
| Build | `ant clean jar` green per `HANDOFF-SESSION-2026-11-14.md`; **re-prove first** |
| Probes | `sh /Users/student/kitnapper-probes/run-all.sh` → `PASS=26`, `light_pixels=175`, `4/4`, `5/5`, `text=Inter`, `EXITED_OK` |
| Model | 18 classes; INV-1/2/4/5 enforced; `LoanRecord.effectiveStatus` derives OVERDUE |
| Persistence | 9 classes; seed fallback D18; `DataStore.save()` atomic tmp+move |
| Services | `AppContext`, `AuthService(+Impl)`, `DataStore(+Impl)`, `LoanService` interface only |
| Shell | `MainFrame` placeholders, custom chrome, sidebar/omnibar/status bar, theme toggle |
| Missing for M3 | `CatalogService(+Impl)`, `LoanServiceImpl`, `Ids`, catalog controllers, all catalog/badge/preview/dialog UI |

### Known constraints that shape the plan

- **200-line cap**: `MainFrame` is already 182 lines and `Dialogs` 128 — split
  before extending.
- **All hex in `Theme`**; components read public token fields at paint time.
- **No third-party libraries**, no test framework; verification is probes + manual.
- **EDT discipline**: all UI mutations and service calls happen on the EDT; only
  image decode/query work may go off-EDT.
- **Theme refresh**: `Theme.addThemeListener` exists but nothing uses it; tables,
  combos, and custom paint need explicit repaint/recolour.

---

## 4. Architecture and data flow

```
LoginFrame ── AppController ── MainFrame (chrome, sidebar, omnibar, status)
                                  │  mount(key, screen, commands, onShown)
                                  ▼
                            CatalogScreen  ── CheckoutDialog
                              │      │
                    Actions  │      │  Toasts / PhotoTile / PreviewPane
                              ▼      ▼
                    CatalogController  BorrowingController
                              │              │
                              ▼              ▼
                    CatalogService      LoanService
                              └──────┬───────┘
                                     ▼
                                  DataStore ── kitnapper-data.json
```

Rules:

- Views never import `controller` or `service`; they receive view-declared
  `Actions`/callbacks (same pattern as `LoginFrame.setOnSignIn` and
  `Sidebar.Callbacks`).
- Controllers may import services and views; they own session/state routing.
- Services are the only callers of entity mutators and `DataStore.save()`.
- `AppContext` owns the service singletons, the session user, the fee strategy,
  and the change-listener list.

---

## 5. API contracts

### 5.1 `kitnapper.service.AppContext` (modified)

```java
public AppContext(DataStore store);                       // LocalDate::now
public AppContext(DataStore store, Supplier<LocalDate> today); // probe clock

public CatalogService catalog();
public LoanService loans();
public LocalDate today();

void fireChanged();        // package-private; services call it after save()
```

- Constructs `CatalogServiceImpl(store)` and
  `LoanServiceImpl(store, lateFeeStrategy, this::currentUser, today, this::fireChanged)`.
- `setCurrentUser` keeps firing; `fireChanged` stays idempotent for zero listeners.
- No view imports.

### 5.2 `kitnapper.service.CatalogService` / `CatalogServiceImpl`

```java
public interface CatalogService {
    List<Equipment> search(String query, EquipmentCategory category, EquipmentStatus status);
    List<Equipment> all(); // search(null, null, null)
}
```

- Trim query; blank/null → no text filter.
- Case-insensitive `Locale.ROOT` match on **name or serial** (REQ-001).
- Null category/status → no filter; filters compose with the query.
- Returns a fresh mutable-safe list sorted by name (case-insensitive), then id.
- Never touches the live list.

### 5.3 `kitnapper.service.LoanServiceImpl` (new, implements existing `LoanService`)

```java
public LoanServiceImpl(DataStore store,
                       FeeStrategy feeStrategy,
                       Supplier<User> session,
                       Supplier<LocalDate> today,
                       Runnable onChange);
```

`checkout(equipmentId, dueDate)`:

1. `today = todaySupplier.get()` once.
2. Session must be a `Borrower` → else `IllegalStateException("Only borrowers can check out gear.")`.
3. Equipment lookup by id → missing: `IllegalArgumentException("That item is no longer in the catalog.")`.
4. `!isBorrowable()` → `IllegalStateException("This item is not available to borrow.")` (INV-1).
5. Count the borrower's loans whose `effectiveStatus(today) != RETURNED`; `>= 3`
   → `IllegalStateException("You already have 3 active loans. Return one first.")` (INV-6).
6. `LoanRecord.checkout(Ids.nextLoanId(...), userId, equipmentId, today, dueDate)`;
   wrap model `IllegalArgumentException` as
   `"Due date must be between " + today + " and " + today.plusDays(14) + "."` (INV-2).
7. `equipment.markLoaned()`, add loan, `store.save()`, `onChange.run()`.

`returnLoan(loanId, returnDate)` (service now, UI M4):

1. Session must be `Manager` → else `IllegalStateException("Only a manager can log returns.")` (INV-3).
2. Loan lookup → missing IAE; already `RETURNED` → ISE.
3. `returnDate.isAfter(today)` → `IllegalArgumentException("Return date cannot be in the future.")` (D20).
4. `fee = loan.accruedFee(returnDate, feeStrategy)`; `loan.completeReturn(returnDate)`.
5. `fee > 0` → add exactly one `Penalty(Ids.nextPenaltyId(...), loanId, LATE_FEE, fee, returnDate)` (FR-07, D20).
6. Find linked equipment; if its status is `LOANED`, `markAvailable()`; if
   `UNDER_MAINTENANCE`, leave it (D15). Missing equipment → ISE.
7. `store.save()`, `onChange.run()`.

Reads (sorted, deterministic):

- `myLoans()` → session user id; borrow date desc, then loan id.
- `allLoans()` → all; borrow date desc, then loan id.
- `overdueLoans()` → `isOverdue(today)`; days overdue desc, then due date, then id.
- `accruedFee(loanId)` → `loan.accruedFee(today, feeStrategy)`; missing → IAE.

If the class exceeds ~190 lines, extract `LoanQueries.java` (read helpers) or
`LoanMessages.java` (strings) — do not breach the cap.

### 5.4 `kitnapper.service.Ids` (new)

```java
public static String nextLoanId(List<LoanRecord> loans);       // L-004 after L-003
public static String nextPenaltyId(List<Penalty> penalties);   // p-001
public static String nextEquipmentId(List<Equipment> equipment); // e-011 (M5)
```

- Scan numeric suffixes after the prefix, take max + 1, zero-pad to 3.
- Ignore non-numeric/blank suffixes; deterministic after reset (D22).

### 5.5 Controllers

```java
public final class CatalogController {
    public CatalogController(AppContext context); // subscribes to app changes
    public void setQuery(String query);
    public void setCategory(EquipmentCategory category); // null = any
    public void setStatus(EquipmentStatus status);       // null = any
    public void clearFilters();
    public List<Equipment> results();
    public boolean isManager();
    public boolean canCheckout(Equipment item);          // borrower + borrowable
    public String checkoutHint(Equipment item);          // reason / manager note
    public void setOnChanged(Runnable listener);         // screen.reload
}

public final class BorrowingController {
    public BorrowingController(AppContext context);
    public List<LocalDate> dueDateChoices();             // today .. today+14
    public Optional<String> checkout(Equipment item, LocalDate dueDate);
    public Optional<String> returnLoan(String loanId, LocalDate returnDate); // M4 UI
}
```

- `BorrowingController.checkout` catches `RuntimeException`; `UncheckedIOException`
  → "Could not save the change. Check the app folder and retry."; otherwise use
  the service's user-facing message; null/blank → "Could not complete checkout."
- `CatalogController` notifies its listener on every `AppContext` change (NFR-02).
- Controllers never touch Swing components.

### 5.6 View-facing shell API (`MainFrame`, `Omnibar`, `StatusBar`)

```java
// MainFrame (constructor unchanged — probes keep working)
public void mount(String key, JComponent screen, JComponent commands, Runnable onShown);
public void setStatus(String left, String right);
public JTextField searchField();
public void showScreen(String key); // existing

// Omnibar
public void setCommands(JComponent commands); // null clears; commandSlot() kept
```

- `mount` replaces the placeholder in the `CardLayout` and records commands/onShown.
- `showScreen` sets title/crumb, sidebar selection, mounts that screen's commands
  (or clears), clears status-right, then runs `onShown`.
- `AppController.navigate` sets a neutral status-left before switching screens;
  `CatalogScreen.onShown` overrides it.

### 5.7 `CatalogScreen.Actions` (view-declared, controller-supplied)

```java
interface Actions {
    List<Equipment> search(String query, EquipmentCategory category, EquipmentStatus status);
    boolean isManager();
    boolean canCheckout(Equipment item);
    String checkoutHint(Equipment item);
    List<LocalDate> dueDates();
    Optional<String> checkout(Equipment item, LocalDate dueDate);
    void setStatus(String left, String right);
}
```

### 5.8 Shared view APIs

```java
// Dialogs (facade; DialogSkin does the painting)
public static boolean showForm(Window owner, String title, JComponent body,
    String confirmLabel, boolean destructive, Supplier<Optional<String>> onConfirm);

// CheckoutDialog
public static boolean show(Window owner, Equipment item, List<LocalDate> dueDates,
    Function<LocalDate, Optional<String>> checkout); // true = checked out

// SkeletonGate (package-private timings constructor for probes)
public interface Work<T> { T run() throws Exception; }
public static <T> void run(SkeletonPanel panel, String loadingName,
    Work<T> work, Consumer<T> success, Consumer<Throwable> failure);

// Toasts
public static void show(Window owner, String message);

// Badges
public static JComponent equipmentStatus(EquipmentStatus status);
public static void paint(Graphics2D g, int x, int y, EquipmentStatus status);
```

---

## 6. File inventory

### 6.1 New — service/controller (6 files)

| File | Package | Purpose | Est. |
| :--- | :--- | :--- | --: |
| `CatalogService.java` | `kitnapper.service` | Search/filter contract | 25 |
| `CatalogServiceImpl.java` | `kitnapper.service` | Query + sort | 70 |
| `LoanServiceImpl.java` | `kitnapper.service` | Checkout, return, reads, fees | 180 |
| `Ids.java` | `kitnapper.service` | Sequential `L-`/`p-`/`e-` IDs | 50 |
| `CatalogController.java` | `kitnapper.controller` | Query state + role/action hints | 90 |
| `BorrowingController.java` | `kitnapper.controller` | Due-date choices, checkout errors | 70 |

### 6.2 New — view (20 files)

| File | Purpose | Est. |
| :--- | :--- | --: |
| `ScreenCatalog.java` | Extracted `SCREENS` metadata, nav keys, titles | 80 |
| `PlaceholderScreen.java` | Extracted M2 placeholder panel | 45 |
| `Badges.java` | 20 px pill + dot + word; wrench on maintenance | 120 |
| `SegmentedControl.java` | Table/Cards (and later My Loans tabs) | 90 |
| `GradientTile.java` | The one permitted gradient fallback | 90 |
| `SkeletonPanel.java` | Flat blocks + pulse, MEDIA/CARD/PREVIEW/TABLE | 120 |
| `SkeletonGate.java` | 150/300/167 ms, watchdog, accessibility | 140 |
| `PhotoTile.java` | Off-EDT decode, cache, gradient fallback | 130 |
| `PreviewPane.java` | 280 px detail pane + pinned action | 160 |
| `EquipmentCard.java` | Cards-view tile | 110 |
| `CatalogTableModel.java` | 5 typed columns | 90 |
| `TableSkins.java` | Row/header/renderers reused by M4 | 120 |
| `CatalogTable.java` | JTable + scroll + selection + sorter | 120 |
| `CatalogCards.java` | Card grid + selection | 90 |
| `CatalogFilters.java` | Category/status combos + layout toggle | 110 |
| `CatalogScreen.java` | Page layout, query, selection, status | 160 |
| `DatePicker.java` | Non-editable `T..T+14` combo | 70 |
| `CheckoutDialog.java` | Item summary + picker + inline error | 120 |
| `DialogSkin.java` | Shared scrim/card/footer/error shell | 130 |
| `Toasts.java` | POPUP_LAYER stack, 4 s, max 3 | 100 |

### 6.3 Modified

| File | Change | Est. |
| :--- | :--- | --: |
| `service/AppContext.java` | Add catalog/loans/today/notify | 95 |
| `controller/AppController.java` | Build/mount catalog, routing, theme | 135 |
| `view/MainFrame.java` | Extract metadata/placeholder; add mount/status/commands | 150 |
| `view/Omnibar.java` | `setCommands`, keep search/slot APIs | 125 |
| `view/Theme.java` | UIManager table/combo/scroll defaults | 185 |
| `view/UiFactory.java` | `comboBox`, refresh tables/combos/header | 125 |
| `view/Dialogs.java` | Facade over `DialogSkin`; keep old methods | 85 |

### 6.4 External probe harness (not committed)

| File | Purpose |
| :--- | :--- |
| `probe/CatalogProbe.java` | Service + controller rules, persistence |
| `probe/CatalogUiProbe.java` | Headless catalog/table/preview/render |
| `kitnapper/view/SkeletonProbe.java` | Timing, failure, watchdog, reduced motion |
| `kitnapper/view/PhotoTileProbe.java` | Temp image decode + fallback + cache |
| `run-all.sh` | Add compile + run sections and expected outputs |

### 6.5 Line-cap strategy

- Split `MainFrame` and `Dialogs` **before** adding features.
- Split `CatalogScreen` into `CatalogFilters` + `CatalogTable` + `CatalogCards`
  from the start.
- If `LoanServiceImpl` > 190 lines, extract `LoanQueries`.
- Audit at the end:
  `find desktop-app/src -name '*.java' | xargs wc -l | awk '$1 > 200'`
  must print nothing.

---

## 7. Work packages (execution order)

### WP0 — Preflight (read-only)

- [ ] `git status --short`, `git log -3 --oneline`; record HEAD.
- [ ] Run `sh /Users/student/kitnapper-probes/run-all.sh`; save the output.
- [ ] Confirm `ant clean jar` is green and JDK 17/Ant paths work.

**Exit:** baseline probes green; any pre-existing failure is reported before code.
**No git writes.**

### WP1 — Services + controllers + business probes

- [ ] `Ids.java` (loan, penalty, equipment).
- [ ] `CatalogService` + `CatalogServiceImpl`.
- [ ] `LoanServiceImpl` with the §5.3 rules and friendly messages.
- [ ] `AppContext`: construct both services, injectable `today`, `fireChanged`.
- [ ] `CatalogController` (state + change subscription) and `BorrowingController`.
- [ ] `CatalogProbe` covering §9.2; compile and run against `build/classes`.

**Exit:** `CatalogProbe` green; `DataProbe` still `PASS=26 FAIL=0`; no Swing
changes yet. This is the point where the risky rules are proven.

### WP2 — Shell split and mounting API

- [ ] Extract `ScreenCatalog` (metadata, `navKeys`, `defaultKey`, `titles`).
- [ ] Extract `PlaceholderScreen.create(meta)`.
- [ ] `MainFrame`: `mount`, `setStatus`, `searchField`, onShown/commands maps;
      manager `navKeys` = `dashboard, catalog, allloans, overdue, penalties, inventory`.
- [ ] `Omnibar.setCommands` (null clears).
- [ ] Keep the existing `MainFrame` constructor signature for `FrameProbe`.

**Exit:** build green; `FrameProbe` 5/5, `SizeProbe` sane; mounting is exercised by `CatalogUiProbe` in WP4.

### WP3 — Shared view infrastructure

- [ ] `Badges` (equipment statuses now; penalty mapping optional).
- [ ] `SegmentedControl`.
- [ ] `GradientTile`.
- [ ] `SkeletonPanel` + `SkeletonGate` (timings injectable).
- [ ] `PhotoTile` (default `kitnapper-images/`, package-private `Path` ctor).
- [ ] `TableSkins`.
- [ ] `Toasts`.
- [ ] `DialogSkin` + refactor `Dialogs` to a facade; keep `showMessage` and
      `confirmDestructive` behaviour.

**Exit:** components paint in dark/light; `SkeletonProbe` and
`PhotoTileProbe` green; existing frame paints unaffected.

### WP4 — Catalog screen

- [ ] `PreviewPane`, `EquipmentCard`, `CatalogTableModel`, `CatalogTable`,
      `CatalogCards`, `CatalogFilters`, `CatalogScreen`.
- [ ] `AppController.openMain`: construct controllers/screen, implement
      `Actions`, `mount("catalog", ...)`, subscribe `CatalogController.setOnChanged`
      to `screen.reload`, refresh theme.
- [ ] Search field, filters, table/cards toggle, selection, empty state, status bar.

**Exit:** `CatalogUiProbe` green; offscreen 1280×800 paint in both themes;
10 rows initially, 7 AVAILABLE / 3 LOANED, total ₱872,000.00.

### WP5 — Checkout

- [ ] `DatePicker` (`T..T+14`, default `T+3`).
- [ ] `CheckoutDialog` via `Dialogs.showForm`; inline DANGER error stays open.
- [ ] Preview checkout action enabled only for borrower + AVAILABLE.
- [ ] Success → toast + refresh + status update; failure → inline dialog error.
- [ ] Manager catalog: no checkout button, hint text.

**Exit:** checkout persists (reload sees it); item becomes LOANED; selection
updates; 4-click budget holds (select → Check out → confirm).

### WP6 — Verification, docs, handoff

- [ ] `ant clean jar`; full probe suite (old + new).
- [ ] Manual SRS §6.2 steps 1–6 + manager/theme/cards/empty/Ctrl+F/restart.
- [ ] Line-cap, hex, and “no service imports in view” audits.
- [ ] Docs: README, decisions §5/§9, implementation-plan (M3 row, inventory,
      D24–D26, revision 1.5.0), design §5 sidebar, SRS §6.2 note, `HANDOFF-M4.md`.
- [ ] Decide on committing the untracked session handoff.
- [ ] Ask for approval before staging/committing/pushing; verify signed +
      remote `verified: true` after push.

**Exit:** Definition of Done (§14) fully checked.

---

## 8. UI specification

### 8.1 Catalog page

```
page header (title 20 + subtitle 12, 24/32 padding)
┌──────────────────────────────────────────────┬──────────────┐
│ table view (default) or cards view           │ PreviewPane  │
│ JScrollPane                                  │ 280 px       │
└──────────────────────────────────────────────┴──────────────┘
status bar: "N items · 0/1 selected"           right: ₱ total
```

- Page background `bgApp`; content padded 16–24 px.
- Table and cards share selection state (`selectedId`); switching views keeps it.
- Empty results replace the left pane with the empty state (not an empty table).
- Manager and borrower share the screen; only the preview action differs.

### 8.2 Details table (default)

- Columns: Name, Category, Status, Serial, Replacement value.
- 40 px rows, 36 px header, 1 px `BORDER_SUBTLE` horizontal separators, no
  vertical gridlines, single selection, `ACCENT_TINT` selection, hover row.
- Status cell painted by `Badges` (dot + word + wrench on maintenance).
- Money right-aligned, monospace, `Formats.money`.
- IDs/serials monospace.
- Click-to-sort on all columns via `TableRowSorter`; typed model values
  (`String`, `EquipmentCategory`, `EquipmentStatus`, `Double`).
- No manual editing; no column reordering requirement.

### 8.3 Preview pane

- Fixed 280 px, `BG_ELEVATED`, 1 px left border.
- 16:9 media at the top: `PhotoTile` (falls back to `GradientTile`).
- Title 16 px semibold; metadata rows (12 px secondary label / 14 px value):
  Status badge, Category, Serial (mono), Replacement value (mono), ID (mono).
- Primary action pinned at the bottom:
  - borrower + AVAILABLE: primary **Check out**.
  - borrower + unavailable: disabled button + `checkoutHint` caption.
  - manager: no button; caption "Manager view — checkout is borrower-only."
- Empty state: muted glyph + "Select an item to see details."

### 8.4 Cards view

- 8 px radius `BG_SURFACE` cards, 1 px `BORDER_SUBTLE`, no shadow/gradient.
- 16:9 media, name 14 semibold, category 12 secondary, status badge, value mono.
- Hover raises the border to `BORDER_STRONG`; selected uses `ACCENT` 1 px +
  `ACCENT_TINT` tint.
- Grid `0×3` inside a scroll pane; card click selects and updates the preview.
- Toggle is session-only (D3); default is table.

### 8.5 Omnibar commands and filters

- `CatalogScreen.commands()` returns a compact row mounted in the omnibar center:
  - Category combo: “Any category” + 6 categories.
  - Status combo: “Any status” + 4 equipment statuses.
  - `SegmentedControl`: Table | Cards.
- Search lives in the omnibar search field (240→320 px on focus, Ctrl+F).
  `CatalogScreen` attaches a `DocumentListener`; filtering is on keystroke.
- No “+ New” command in M3 (inventory is M5).
- Leaving Catalog clears the command slot; returning restores it.

### 8.6 Empty state

- Centered: muted glyph, 14 px “No gear matches this filter.”, 12 px secondary
  line, and a **Clear filters** secondary button.
- Clear resets search + both combos + restores the full table; selection cleared.

### 8.7 Checkout dialog

- 480 px modal over the shared scrim; title “Check out gear”.
- Body: item name (16 semibold), category/serial/value rows, due-date picker.
- `DatePicker`: non-editable combo of `T … T+14` formatted `yyyy-MM-dd`;
  default `T+3`; accessible name “Due date”.
- Footer: Cancel (secondary) + Check out (primary, rightmost); Enter confirms,
  Esc cancels.
- On service error, a DANGER line appears above the footer and the dialog stays
  open; on success it closes and the caller shows the toast.
- No condition/damage fields (out of scope).

### 8.8 Toasts

- Bottom-right, 320 px, `BG_ELEVATED` + border, 4 s, max 3 stacked.
- Trigger in M3: successful checkout (`Checked out <name> · due <date>`).
- Use `JFrame.getLayeredPane()` `POPUP_LAYER` — never the glass pane owned by
  `Dialogs`.
- Theme-aware; no shadow, no accent fill.

### 8.9 Photos and skeletons

- Seed items have no photos → gradient tile for every card/preview in M3.
- `PhotoTile` still decodes `kitnapper-images/<file>` when present (M5 will write
  them): skeleton MEDIA while decoding off-EDT, then image; failure → gradient.
- Skeleton timing: 150 ms delay, 300 ms minimum, 167 ms fade, 1,000 ms pulse,
  10 s watchdog; reduced motion (`Theme.animateSkeletons=false`) = static blocks
  + instant swap.
- Catalog queries stay synchronous: at seed scale they are far below 150 ms, so
  no query skeleton is shown (design §13.1 “gated, never speculative”). Record
  the measured time in the probe output.
- Skeletons are never focusable/clickable; host exposes “Loading photo” /
  `PROGRESS_BAR` and one start/end name event.

### 8.10 Theme refresh

- Custom-painted components read `Theme` at paint time → `repaint()` suffices.
- `UiFactory.refresh` additionally recolours `JTable`, `JTableHeader`,
  `JComboBox`, and scroll panes.
- `AppController.toggleTheme` calls `mainFrame.refreshTheme()` then
  `catalogScreen.refreshTheme()`; table header and combos are repainted.
- No `updateComponentTreeUI` (would reset custom skins).

---

## 9. Probe plan

Probes live outside the repo at `/Users/student/kitnapper-probes/` and are run by
`run-all.sh`. They are plain `main` classes with a `check(name, ok)` counter — no
test framework.

### 9.1 Harness updates

- Compile new sources into `$BASE/probe` (public APIs) and `$BASE/out`
  (package-private view probes), same as `SkinProbe`/`EdgeProbe`.
- Set `-Djava.awt.headless=true` for render probes that do not need a window.
- Exit non-zero on any failure; print `PASS=n FAIL=0`.
- Keep existing sections and add:
  `CatalogProbe`, `CatalogUiProbe`, `SkeletonProbe`, `PhotoTileProbe`.

### 9.2 `CatalogProbe` — rules and persistence (temp dir each scenario)

Use `AppContext(store, () -> T)` with fixed `T = LocalDate.of(2026, 11, 14)` so
results are deterministic.

Catalog:
1. `all().size() == 10`, sorted by name.
2. `search("sony", null, null)` → 2 (A7 IV, FE lens).
3. `search("SONY", ...)` → 2 (case-insensitive).
4. `search("SN-EOS", ...)` → 1 (Canon EOS R6).
5. `search(null, CAMERA, null)` → 2.
6. `search(null, null, AVAILABLE)` → 7; `LOANED` → 3.
7. `search("sony", null, LOANED)` → 1; `search("sony", null, AVAILABLE)` → 1.
8. `search("zzz", ...)` → 0; `search("  ", ...)` → 10.

Checkout:
9. student1 checks out e-002 due `T+3` → `L-004`, e-002 LOANED, loan count 4.
10. Save + new `DataStoreImpl(dir).load()` → loan/status survive.
11. student1 now has 3 active/overdue (L-001, L-003, new); e-003 checkout →
    rejected, e-003 still AVAILABLE, loan count unchanged.
12. student2 (1 loan) checks out e-003 due `T+15` → rejected; e-003 AVAILABLE.
13. student2 checks out e-001 (LOANED) → rejected (INV-1).
14. manager checkout → rejected (INV-3).
15. `dueDate = T-1` → rejected (INV-2).

Return (service, UI in M4):
16. manager returns L-003 with `T` → fee 200, exactly one `p-001` LATE_FEE,
    status RETURNED, e-007 AVAILABLE, penalty count 1.
17. second return L-003 → rejected; penalty count still 1.
18. return L-002 with `T+1` → rejected (future); loan stays ACTIVE.
19. zero-fee: student2 checks out e-003 due `T`, manager returns `T` → no penalty.
20. maintenance: manager flags e-001 UNDER_MAINTENANCE while L-001 is LOANED,
    then returns L-001 with `T` → item stays UNDER_MAINTENANCE, loan RETURNED.
21. `accruedFee("L-003") == 200`; after return, still 200 (frozen at return date).
22. `overdueLoans()` before returns = exactly `[L-003]`; after = empty.
23. `myLoans()` filters by session borrower; `allLoans()` includes all.

Controller:
24. `BorrowingController.dueDateChoices()` size 15, first `T`, last `T+14`.
25. `checkout` returns empty on success and a non-empty message on cap/window.

### 9.3 UI/render probes

`CatalogUiProbe` (headless, offscreen 1280×800):
1. Mount `CatalogScreen` in `MainFrame`; table row count 10.
2. Status-left contains “10 items”; status-right `₱872,000.00`.
3. Search “Sony” → 2 rows; status LENS → 2; composed LENS+LOANED → 1.
4. Select Canon EOS R6 → preview title/badge/value update.
5. Controller checkout succeeds → badge LOANED, AVAILABLE count 6, preview
   action disabled.
6. Manager screen → checkout action hidden.
7. Cards toggle → 10 cards; selection synchronised; toggle back keeps selection.
8. Empty search → empty state visible; Clear filters restores 10.
9. Paint dark and light; assert no exceptions and non-background pixel count is
   above a threshold (catalog is not blank).
10. Query timing printed; assert < 150 ms (documents “no query skeleton”).

`SkeletonProbe` (package `kitnapper.view`, package-private timings):
1. Work 0 ms → skeleton never becomes visible.
2. Work 400 ms → skeleton visible, stays ≥ 300 ms.
3. Work throws → failure callback, no skeleton left behind.
4. Watchdog 50 ms on a 500 ms task → failure callback fires, skeleton removed.
5. `Theme.animateSkeletons=false` → no pulse timer, instant swap.

`PhotoTileProbe` (package `kitnapper.view`, temp image dir):
1. Missing file → gradient fallback, no exception.
2. Valid JPEG → decoded, cached, painted (pixel diff from gradient).
3. Corrupt file → gradient fallback.
4. Path traversal (`../x.jpg`) rejected.

### 9.4 Expected baseline after M3

| Probe | Expected |
| :--- | :--- |
| `DataProbe` | `PASS=26 FAIL=0` (seed unchanged) |
| `SkinProbe` | `light_pixels=175` |
| `EdgeProbe` | `PASS=4 FAIL=0` |
| `FrameProbe` | `PASS=5 FAIL=0` |
| `FontProbe` | `text=Inter` |
| `SizeProbe` | unchanged key heights |
| `CloseProbe` | `EXITED_OK` |
| `CatalogProbe` | new `PASS=n FAIL=0` (record n) |
| `CatalogUiProbe` | new `PASS=10 FAIL=0` |
| `SkeletonProbe` | new `PASS=5 FAIL=0` |
| `PhotoTileProbe` | new `PASS=4 FAIL=0` |

---

## 10. Manual verification

### 10.1 SRS §6.2 steps (M3 slice)

1. Launch → login screen (regression).
2. Wrong password → inline error (regression).
3. `student1` login → Catalog with search/filter; typing “Sony” narrows to 2.
4. Select Canon EOS R6, due `T+3` → checkout succeeds; row badge LOANED.
5. Loaned item’s checkout action is disabled (availability lock).
6. 15-day rejection: covered by `CatalogProbe` (picker cannot emit `T+15`);
   record this in SRS/decision docs.
7. Restart → new loan persists; login again → Canon still LOANED.
8. Manager login → default Dashboard; Catalog appears in nav; browsing shows no
   checkout action.
9. Theme toggle on Catalog (dark ↔ light) → table, header, badges, preview,
   combos all recolour.
10. Cards toggle → 10 cards; selection syncs; toggle back keeps selection.
11. Empty search “zzz” → empty state; Clear filters restores 10.
12. Ctrl+F focuses search from anywhere in the shell.

### 10.2 Visual/behaviour checks

- 1280×800 default and 1024×640 minimum: no clipped filters, preview stays 280.
- Status bar reflects filtered counts + total replacement value.
- Money `₱872,000.00`; dates `yyyy-MM-dd`.
- No gradient outside the fallback tile; no shadow/glow.
- Focus rectangles visible on buttons, table, combos, search.
- Manager catalog hint is explicit (not a silently missing button).

### 10.3 Notes

- Real mouse drag/resize and DPI scaling remain manual M2 caveats; retest once.
- The 15-day UI path is intentionally unreachable; the service still rejects it
  defensively, and the probe proves it.

---

## 11. Documentation updates (same change as behavior)

| File | Update |
| :--- | :--- |
| `README.md` | Status: M3 delivered; remove the M2 “not verified after split” caveat; mention manager read-only catalog; add `HANDOFF-M4.md` to contents |
| `docs/decisions.md` | §5 add manager Catalog nav + read-only decision; §9 add M3 status; §4 document version rows |
| `docs/implementation-plan.md` | M3 row (catalog + checkout only, manager read-only, picker); §2.5 inventory additions; decisions **D24** (manager read-only catalog), **D25** (picker + probe verification of INV-2), **D26** (screen mounting + file splits); revision 1.5.0 |
| `docs/design.md` | §5 sidebar manager group adds Catalog; screen inventory role “both (manager read-only)” |
| `docs/SRS.md` | §6.2 step 6 note: picker constrains the UI, service/probe proves rejection; revision 3.1.2 |
| `HANDOFF-M4.md` (new) | Current state, M4 scope (returns/fees/manager views), gotchas, probe commands |
| `HANDOFF-SESSION-2026-11-14.md` | Decide: commit it as the session record (recommended) or leave untracked |
| `AGENTS.md` (local, ignored) | Current milestone M3, next M4; never commit |

Rule: no code commit without the matching doc update (repo convention).

---

## 12. Git plan

All writes need **explicit approval**, per `AGENTS.md`; approval for a commit does
not approve its push.

### Proposed commits

**A — services**

```
feat(catalog): add catalog and loan services

Add the M3 service layer: catalog search/filtering, checkout and
return workflows, sequential ID generation, and the AppContext
wiring that exposes both services with an injectable clock.

- Checkout enforces role, availability, the 3-loan cap, and the
  14-day window; every mutation saves through DataStore
- Return freezes at most one late-fee penalty, rejects future
  return dates, and preserves UNDER_MAINTENANCE items (D15/D20)
- CatalogController/BorrowingController keep user-facing messages
  out of the views and translate failures for dialogs
```

**B — UI**

```
feat(ui): add catalog, preview, and checkout

Replace the catalog placeholder with a real Win11 catalog: a
details table plus preview pane by default, a cards toggle,
search and filters in the omnibar, and instant checkout.

- Shared badge, table, preview, photo-tile, skeleton, toast, and
  dialog infrastructure for M4/M5
- Managers get a read-only catalog; checkout stays borrower-only
- Successful checkout persists, refreshes the row, and shows a
  toast; validation errors stay inline in the dialog
```

**C — docs**

```
docs: record M3 completion and hand off M4

Update the README, decision log, implementation plan, design
sidebar, and SRS verification note for the shipped catalog and
checkout, and add the M4 handoff.
```

- Each commit: author `Ling Chen`, both co-author trailers, SSH signature,
  conventional subject, ~72-col body.
- Review `git log -12 --format='%h %s%n%b'` before writing messages.
- Stage only intended source/docs; never `build/`, `dist/`, `kitnapper-data.json`,
  `kitnapper-images/`, `AGENTS.md`.
- After push: confirm `git ls-remote` SHA and GitHub `verified: true`; never claim
  success from the local commit alone.

---

## 13. Risks and mitigations

| Risk | Mitigation |
| :--- | :--- |
| 200-line cap breached late | Split `MainFrame`, `Dialogs`, `CatalogScreen` first; audit before commit |
| Toast layer fights `Dialogs` scrim | Toasts on `POPUP_LAYER`, dialogs on the glass pane |
| Theme switch leaves table/combos stale | Explicit renderers + `UiFactory.refresh` + per-screen repaint |
| Modal dialog blocks headless probes | Test controller/service and dialog body; do not `setVisible(true)` in probes |
| 15-day demo step impossible via picker | Service validation + `CatalogProbe`; document in SRS |
| Date flakiness across midnight | Injectable `Supplier<LocalDate>` in `AppContext`/services; probes fix `T` |
| ID collision after manual edits | Max-suffix scan; ignore non-numeric suffixes; probe after reset |
| Save failure crashes EDT | Controllers catch `UncheckedIOException` and show a retry message |
| Manager nav change breaks probes | Keep `MainFrame` constructor/signature; only `navKeys` changes |
| `LoanServiceImpl` too large | Extract `LoanQueries`/`LoanMessages` if >190 lines |
| Catalog sorting vs. filtered status text | Typed model values + `TableRowSorter`; status text from the shown list |
| Photo decode thread leaks | `SkeletonGate` watchdog + cancel; cache capped and synchronized |

---

## 14. Definition of Done

- [ ] `ant clean jar` → BUILD SUCCESSFUL (JDK 17).
- [ ] Full probe suite green; record every count in the final report.
- [ ] SRS §6.2 steps 1–6 pass (step 6 via probe) and manager/theme/cards/empty/
      Ctrl+F/restart checks pass.
- [ ] Table+preview default, cards toggle, 10 seed rows, 7 AVAILABLE / 3 LOANED,
      ₱872,000.00 aggregate.
- [ ] Checkout persists, locks the item, respects cap/window, and shows inline
      errors; manager catalog has no checkout action.
- [ ] All files ≤ 200 lines; LF; 4-space; no hex outside `Theme` (transparent
      window colors excepted and noted).
- [ ] Views import no `controller`/`service`/persistence classes; services are
      the only mutators/savers.
- [ ] Docs updated; `HANDOFF-M4.md` written; untracked handoff decision recorded.
- [ ] Commits signed, both trailers, remote verified after push.
- [ ] Working tree clean except ignored runtime/build files.

---

## 15. Out of scope (do not do in M3)

- My Loans, All Loans, Overdue, Penalties, Inventory, Settings screens.
- Return/settle UI, penalties tables, dashboard KPIs.
- Photo import dialog / `ImageStore` / reset demo data (M5).
- Thin scrollbars, motion polish, skeleton audit, DPI pass (M6).
- Schema changes, `schemaVersion` bump, new libraries, test framework.
- Loan cancellation/extension, reservations, damage tracking.
- Refactors unrelated to M3 that would disturb M2 probes.

---

## 16. Follow-ups after M3

- **M4**: My Loans / All Loans / Overdue / Penalties views, return and settle
  dialogs, loan row view-models (borrower/item names), table reuse via
  `TableSkins`.
- **M5**: Inventory CRUD, `ImageStore`, photo picker, settings/reset, reduced-
  motion switch.
- **M6**: `ThinScrollBarUI`, Fluent motion, 1080p/1366×768 passes, skeleton
  audit, gradient spot-check.
- **Debt**: `Dialogs` facade cleanup, `UiFactory` refresh coverage, manager
  catalog docs, 15-day UI validation note.

---

## 17. Execution log

Every plan/phase gets an entry here before the matching commit, and each entry
is committed and pushed with the work it describes.

### WP0 — Preflight baseline (2026-11-14)

**Status:** complete. Verification only; **no source, docs, or config changed**.

Recorded baseline before any M3 work:

| Item | Result |
| :--- | :--- |
| Branch / HEAD | `main` @ `745ae2899b894755d55740825e1dab5100b8ba3b` |
| Working tree | only untracked `HANDOFF-M3-PLAN.md` + pre-existing `HANDOFF-SESSION-2026-11-14.md` |
| JDK | OpenJDK 17.0.21 |
| Ant | Apache Ant 1.10.18 |
| `ant clean jar` | BUILD SUCCESSFUL |
| `DataProbe` | `PASS=26 FAIL=0` (expected unusable-save-file warning, seeds fresh) |
| `SkinProbe` | `light_pixels=175 total=6400` |
| `EdgeProbe` | `PASS=4 FAIL=0` |
| `FrameProbe` | `PASS=5 FAIL=0` |
| `FontProbe` | `text=Inter display=SansSerif mono=Monospaced icon=Inter` |
| `SizeProbe` | title bar 32, omnibar 48, sidebar 240, status bar 32; manager sidebar has 6 buttons |
| `CloseProbe` | `EXITED_OK` / `CALLING_CLOSE` |

Notes:

- Baseline matches `HANDOFF-SESSION-2026-11-14.md` exactly, so M3 starts from a
  known-green state and no repairs are folded into the first feature commit.
- Build output is correctly ignored (`build/`, `kitnapper-data.json`); the probe
  run did not leave a runtime save file in the repo.
- Manager sidebar currently has 6 buttons; M3-D2 makes it 7 by inserting
  Catalog, which `SizeProbe` output will reflect in WP2.
- This entry is committed together with the plan file itself under a `docs:`
  commit. No `[x]` marks are added to later WP sections until those phases land.

**Next:** WP1 — `Ids`, `CatalogService(+Impl)`, `LoanServiceImpl`, `AppContext`
wiring, `CatalogController`, `BorrowingController`, and `CatalogProbe`.

---

## Appendix A — Implementation checklist (copy into the working session)

```
[x] WP0 preflight: probes green, HEAD recorded
[ ] Ids + CatalogService(+Impl) + LoanServiceImpl + AppContext
[ ] CatalogController + BorrowingController
[ ] CatalogProbe green; DataProbe still 26
[ ] MainFrame split + mount/status/commands; manager nav
[ ] Badges, SegmentedControl, GradientTile, skeletons, PhotoTile, Toasts,
    TableSkins, DialogSkin
[ ] PreviewPane, EquipmentCard, CatalogTable(Model), CatalogCards,
    CatalogFilters, CatalogScreen
[ ] DatePicker + CheckoutDialog; toasts; inline errors
[ ] CatalogUiProbe + SkeletonProbe + PhotoTileProbe green
[ ] Manual SRS 1–6 + manager/theme/cards/empty/Ctrl+F/restart
[ ] line-cap/hex/view-import audits
[ ] docs + HANDOFF-M4
[ ] ask approval → commit A/B/C → push → verify remote
```
