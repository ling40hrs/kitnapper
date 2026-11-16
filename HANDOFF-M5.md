# HANDOFF — M4 done, next: M5 (inventory, photos, reset)

> **Status (2026-11-16, M4 close-out):** M4 is **delivered and closed** — WP0–WP6,
> the width pass and two close-out fixes pushed at `cf5a838`; **12/12 probes** green
> in `run-all.sh` with `LoansUiProbe PASS=105 FAIL=0` registered. M5 is **not
> started**. This file is the working scope note for M5. The detailed M5 plan (data
> contracts, work packages, probe plan, docs list, definition of done) should be
> written as `HANDOFF-M5-PLAN.md` before implementation begins, following the
> pattern of `HANDOFF-M4-PLAN.md`.

Read `docs/preferences.md` §2–§3 and `docs/implementation-plan.md` §4 before
starting.

## 1. Where things stand

- `main` @ `cf5a838`, tree clean, local = remote (as of 2026-11-16 close-out).
- Build: `cd desktop-app && ./build.sh clean && ./build.sh` → `BUILD OK`, 187
  classes, zero `-Xlint:all` warnings. `sh ../../netbeans/extide/ant/bin/ant
  clean jar` is the literal DoD check.
- Probes: `cd desktop-app && ./test/run-all.sh` → `ALL PROBES GREEN` (12/12).
  `LoansUiProbe` is registered (display): `PASS=105 FAIL=0`.
- Inventory: 117 Java files / 10,788 lines (18 model, 16 service, 10
  persistence, 10 controller, 62 view, `Main` 1) plus 25 probe files
  (2,811 lines).
- Tallest files: `view/CatalogScreen.java` 199, `view/PreviewPane.java` 197,
  `view/PenaltyPreviewPane.java` 195, `view/LoanPreviewPane.java` 194,
  `view/SegmentedControl.java` 189. **Split before adding** to anything near
  the 200-line cap.
- Inventory and Settings are still **placeholder** screens
  (`PlaceholderScreen` via `ScreenCatalog`); `ImageStore`, `PhotoTile`,
  `SkeletonPanel`, `SkeletonGate`, `InventoryScreen`, `InventoryTableModel`,
  `InventoryController`, `InventoryService(+Impl)`, `EquipmentDialog`, and
  `SettingsScreen` do not exist yet.

## 2. M5 scope (from `docs/implementation-plan.md` §4, `docs/design.md` §12–§13, D27)

| Deliverable | Notes |
| :--- | :--- |
| Inventory screen (manager) | Items table + preview (photo, metadata) + add/edit/decommission/maintenance actions; replaces placeholder route `inventory` |
| `EquipmentDialog` | Add/edit form, category-specific subclass fields, photo picker; kind preserved on category edit (D19); IDs system-generated (D22) |
| Photo import & storage | `ImageStore(+Impl)`: validate extension (png/jpg/jpeg/gif/bmp) and ≤ 20 MB, decode via `ImageIO`, downscale to 1024 px longest edge, re-encode JPEG q0.85, atomic tmp+move into `kitnapper-images/` (`<equipmentId>.jpg`) (design §12, D7) |
| Photo display | `PhotoTile` (off-EDT decode + cache, skeleton placeholder, fallback to `GradientTile`) and `SkeletonPanel`/`SkeletonGate` per design §13 (D27) |
| Reset demo data | Manager action wipes `kitnapper-images/`, regenerates seed relative to today (D4), overwrites save file; orphan sweep on load deletes unreferenced images (D11) |
| Settings screen (manager) | Save-file and photo-folder locations, theme, reset demo data, "Animate loading placeholders" switch (design §13.4); replaces placeholder route `settings` |

M5 is **not** a loan/fee milestone. Loan/penalty behavior and catalog/checkout are
locked; do not rewrite them except for wiring (e.g. showing `PhotoTile` where a
`GradientTile` is currently used, if planned).

## 3. Reuse map (do not rebuild these)

| Need | Use |
| :--- | :--- |
| Screen shell, routing, status bar | `MainFrame.mount(...)` + `showScreen(...)`, `ScreenCatalog` |
| Table skin/pattern | `TableSkins`, `TableCells`, `CatalogTable`/`LoansTable` patterns |
| Preview pane, page header, empty state | `PreviewPane` (equipment-typed), `PageHeader`, `EmptyState` |
| Badges, toasts, dialogs | `Badges`/`StatusBadges`, `Toasts`, `Dialogs.showForm`, `DialogSkin`, `FlatButton` |
| Money/date/category text | `Formats.money`, `Formats.date`, `Formats.category` |
| Category fallback tile | `GradientTile` (the permitted gradient, design §3.4/§3.5) |
| Change notification | `AppContext.addChangeListener` / `DataStore` save + notify pattern |
| ID generation | `Ids` (`e-NNN`, D22) |
| Persistence | `service.persistence` mappers; `photoFile` is nullable/additive, `schemaVersion` stays 1 (D8) |

## 4. Key decisions & constraints (locked)

- **No third-party libraries** (JDK only). JSON, image decode/encode/scale, and all UI are hand-written/custom-painted.
- **`schemaVersion` stays 1**. `photoFile` is nullable/additive (D8); older saves must load unchanged.
- **Photo rules** (D7, D11, design §12): `kitnapper-images/<equipmentId>.jpg`, validate extension and ≤ 20 MB source, downscale to 1024 px longest edge, JPEG q0.85, atomic tmp+move; seed has no photos; reset wipes folder; orphan sweep on load.
- **Kind vs category** (D19): concrete kind chosen at creation (CAMERA/LENS/LAPTOP/AUDIO → DIGITAL, TOOL → MECHANICAL, OTHER → BASE) and preserved on edit; changing category does not change the concrete class.
- **IDs/filenames safe** (D22): equipment IDs system-generated; `ImageStore` builds `<equipmentId>.jpg`.
- **Skeletons** (D27, design §13): 150 ms delay / 300 ms minimum / 167 ms fade, flat blocks + pulse, gated queries, 10 s watchdog, "Animate loading placeholders" session toggle. First real use is M5.
- **Manager-only** inventory mutations and reset (INV-3, FR-10/FR-12).
- **Status transitions** via `Equipment` methods only (INV-1, INV-5).

## 5. Gotchas (carry forward)

- **EDT only.** Any Swing read/write must run on the EDT (`SwingUtilities.invokeAndWait` in probes). Image decode must be off the EDT (`SwingWorker` / background), but UI updates on the EDT.
- **Probe captures.** `frame.paint(g)` can replay stale buffers on a realized window; use `printAll(g)` for screenshots in probes.
- **Probes need a display** for paint probes; `test/run-all.sh` uses `xvfb-run -a`. Run the suite **serially** — it wipes and recompiles `test/out`.
- **Seed facts**: 10 items (7 AVAILABLE / 3 LOANED), 3 users, 3 loans, `₱872,000.00` total replacement value; no penalties, no photos. Seed dates are relative to the seeding date (D4).
- **Modal dialogs cannot be driven from a probe**: `JDialog.setVisible` blocks the
  EDT under xvfb, so `LoansUiProbe` asserts the dialog *bodies* and the controller
  paths instead. The full click-through lives in the throwaway manual harness
  (outside the repo) that a maintainer runs at each close-out.
- **The status sink ignores frames without a peer** (`AppController.openMain` guards
  on `frame.isDisplayable()` so a signed-out session cannot write). Show the frame
  before routing, or the first screen's status line is dropped — the bug the M4
  close-out fixed.
- **`ImageObserver.WIDTH`/`HEIGHT` shadow enclosing constants** inside `JPanel` subclasses — use `PILL_HEIGHT`-style names.
- **`git log --format=%G?` may print `N`** even when commits are SSH-signed; GitHub verification is authoritative.

## 6. Verification (M5 exit, from SRS/plan)

- SRS §6.2 steps 11–12: add a new inventory item **with photo** and decommission it (photo shows on card/preview, persists across restart); reset demo data restores §6.1 baseline and wipes `kitnapper-images/`.
- Extra checks: kind preserved on category edit (D19); photo filename safety (D22); missing/corrupt photo falls back to category tile without errors (NFR-08); orphan sweep.
- `./build.sh clean && ./build.sh` and `sh ../../netbeans/extide/ant/bin/ant clean jar` green, zero `-Xlint:all` warnings.
- `./test/run-all.sh` green (all registered probes) plus any new M5 probes.
- Static audits: no file > 200 lines; no hex outside `Theme`; no `service`/`controller` imports under `view`; no `@SuppressWarnings`.
- Docs and handoff updated in the same change as behavior.

## 7. Commands

```bash
cd desktop-app
./build.sh clean && ./build.sh          # JDK only, no Ant needed
./test/run-all.sh                       # registered probes, xvfb-aware
sh ../../netbeans/extide/ant/bin/ant clean jar   # literal DoD check
./build.sh run                          # launch (writes kitnapper-data.json in cwd)
```

## 8. Out of scope for M5

- Thin scrollbars, Fluent motion, DPI/contrast passes, 1024×640 preview fixes (M6).
- Any schema change; `schemaVersion` stays 1.
- Rewriting M3/M4 catalog or loan behavior except necessary wiring.
- Cloud/hosted images, networking, real auth.
