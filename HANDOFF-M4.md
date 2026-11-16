# HANDOFF — M3 done, next: M4 (loans, returns, fees)

> **Status (2026-11-16):** M4 is planned. The approved contract — data
> contracts, work packages, file inventory, probe plan, docs list, definition of
> done — is **`HANDOFF-M4-PLAN.md`**; where this note and the plan disagree, the
> plan wins. This file remains the scope note, reuse map, and gotcha list.

Written 2026-11-16 at the M3 close-out (`feat(m3)` + `test(m3)` commits, docs
commit alongside this file). Read `docs/preferences.md` §2–§3 and
`docs/implementation-plan.md` §4 before starting; this file is the working note,
the plan is the contract.

## 1. Where things stand

- `main` @ the M3 docs commit, tree clean, local = remote.
- Build: `cd desktop-app && ./build.sh clean && ./build.sh` → `BUILD OK`, 131
  classes, zero `-Xlint:all` warnings. `sh ../../netbeans/extide/ant/bin/ant
  clean jar` also succeeds (the bundled Ant binary is not `+x`, so invoke it
  with `sh`).
- Probes: `cd desktop-app && ./test/run-all.sh` → `ALL PROBES GREEN` (9/9).
  `CatalogUiProbe` prints `PASS=13 FAIL=0`.
- Inventory: 85 source files, 7,285 lines (18 model, 13 service, 10
  persistence, 3 controller, 40 view) plus 15 probe files (1,585 lines).
- Tallest files: `view/CatalogScreen.java` 199, `view/PreviewPane.java` 197,
  `view/SegmentedControl.java` 189, `view/CatalogTable.java` 183. **Split before
  adding** — these three have no headroom.

## 2. What M3 delivered

- **Catalog screen** for borrowers and managers: page header, search field in
  the omnibar, category + status combos, table/cards segmented control, details
  table + 280 px preview (default), cards grid, empty state with Clear filters,
  status bar counts and total replacement value.
- **Instant checkout**: 480 px dialog, `T … T+14` picker defaulting to `T+3`,
  inline DANGER on failure, toast + reload on success. Checkout persists and
  locks the item (`UNDER_MAINTENANCE`/decommissioned never reachable).
- **Manager read-only catalog** (D24): same screen, `Actions.isManager()` true,
  no checkout button, two-line caption "Manager view — / checkout is
  borrower-only."
- **Verification harness** in the repository: `desktop-app/test/run-all.sh`,
  eleven probes, `xvfb-run -a` for the paint ones.

Key files to read first (they define the patterns M4 should copy):
`view/CatalogScreen.java`, `view/CatalogPresenter.java`,
`view/CatalogTable.java`, `view/PreviewPane.java`, `view/TableSkins.java`,
`controller/AppController.java` (mount + `Actions`), `service/LoanService.java`.

## 3. M4 scope — returns, fees, manager views

Deliver these screens as real implementations, replacing the placeholders
(`view/PlaceholderScreen.java`, `view/ScreenCatalog.java` route metadata):

| Screen | Role | Content (design §5) |
| :--- | :--- | :--- |
| My Loans | Borrower | Active/Overdue/Returned segmented control; loans table; preview with fee breakdown |
| All Loans | Manager | Sortable loans table + preview; **return action** |
| Overdue | Manager | Overdue table, longest overdue first, days-late column, live accrued fees |
| Penalties | Manager | Per-loan penalty list, paid/waive actions + reason, preview |
| Dashboard | Manager | 3 KPI cards (out now, overdue, unpaid penalties), overdue table, recent activity |

The business layer is **already written and probed** (M3 WP1):
`LoanServiceImpl` + `LoanQueries` + `LoanMessages` cover `returnLoan`,
`effectiveStatus(asOf)`, `accruedFee`, cap/window checks, D15 maintenance rule
and D20 (zero fee → no penalty, future date rejected). `BorrowingController`
already exposes `returnLoan(loanId, returnDate)` and translates failures.
What is missing is loan **screens**, a penalty service/controller for settle, and
the return/settle dialogs.

Verify with SRS §6.2 steps 7–10 (restart persistence, manager overdue ₱200,
return freezes exactly one `LATE_FEE`, waive with reason), plus the same static
audits M3 used: line caps, no hex outside `Theme` (transparent window colors
excepted), no `service`/`controller` imports in `view`, no suppressions.

## 4. Reuse map (do not rebuild these)

| Need | Use |
| :--- | :--- |
| Screen shell, routing, status bar | `MainFrame.mount(...)` + `showScreen(...)`, `ScreenCatalog` |
| Query/selection state outside the view | copy `CatalogPresenter` (state in the presenter, `CatalogScreen` layout-only) — this is what kept the screen under 200 lines |
| Tables | `TableSkins`, `TableCells`, `CatalogTable` (non-opaque, hover painted **before** `super.paintComponent`) |
| Preview pane, page header, empty state | `PreviewPane`, `PageHeader`, `EmptyState` |
| Badges, toasts, dialogs | `Badges`, `Toasts`, `Dialogs.showForm`, `DialogSkin` |
| Money/date/category text | `Formats.money`, `Formats.date`, `Formats.category` |
| Due-date picker | `DatePicker` (reuse for the return-date dialog) |

## 5. Gotchas (cost real time in M3)

- **EDT only.** Any Swing tree read/write must run inside
  `SwingUtilities.invokeAndWait`; touching components from `main` deadlocks on
  `AWTTreeLock`. `UiProbeSupport.settle` is EDT-only too.
- **Probe captures.** `frame.paint(g)` on a realized window can replay the
  double-buffer and show stale/ghosted content. Use `printAll(g)` for
  screenshots (this produced a false "cards view is broken" scare in WP6).
- **Probes need a display** for the paint probes; `test/run-all.sh` handles
  `xvfb-run -a`. Ctrl+F can only be tested with a pointer click first, because
  Xvfb uses PointerRoot focus.
- **`ImageObserver.WIDTH`/`HEIGHT` shadow enclosing constants** inside
  `JPanel` subclasses — name constants `PILL_HEIGHT`-style.
- **`git log --format=%G?` prints `N`** even though commits are SSH-signed
  (GitHub shows `verified: true`). Do not "fix" this.
- **Seed facts**: 10 items, 7 AVAILABLE / 3 LOANED, `₱872,000.00`;
  `"Sony"` → 2 rows, `Sony` + LENS → 1 (the seed has no loaned lens). Anchored
  at `T = 2026-11-14` in the probes; the app uses the real date (D4).
- **Toast placement** overlaps the preview action bar while visible (4 s). Known
  polish item, not a bug to chase in M4.
- **1024×640** clips the preview's ID row/caption and truncates table badges.
  Known M6 item (design §9 minimum is met: filters unclipped, preview 280 px).

## 6. Commands

```bash
cd desktop-app
./build.sh clean && ./build.sh          # JDK only, no Ant needed
./test/run-all.sh                       # 11 probes, xvfb-aware
sh ../../netbeans/extide/ant/bin/ant clean jar   # literal DoD check
./build.sh run                          # launch (writes kitnapper-data.json in cwd)
```

## 7. Locked decisions (do not re-litigate)

Instant checkout, no approval state, no cancellations or due-date extensions;
₱50/day with no grace period, live accrual frozen into one `LATE_FEE` penalty on
return (zero fee → no penalty); 3 active loans per borrower and a 14-day window;
returns log a date only (no damage tracking); managers never check out (INV-3)
and `UNDER_MAINTENANCE` survives a return (D15); `schemaVersion` stays 1; no
third-party libraries; JTable-based screens stay inside the 200-line cap.

## 8. Out of scope for M4

Inventory CRUD, photo import, `ImageStore`, `PhotoTile`, skeletons, reset demo
data (M5 — D27); thin scrollbars, Fluent motion, DPI pass, contrast pass (M6);
any schema change; anything that rewrites M3's catalog behavior.
