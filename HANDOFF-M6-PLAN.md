# KitNapper M6 — Implementation Backbone (draft)

> **Status (2026-11-16):** draft prepared; it becomes the M6 contract as
> `HANDOFF-M6-PLAN.md` in the repository root (WP0, first commit).
> Decisions **D38–D43** are proposed and are confirmed by committing this plan.
>
> **Baseline re-proved before writing this plan:** `main` @ `97fc44c`, tree clean,
> local == `origin/main`; `./test/run-all.sh` → `ALL PROBES GREEN` (**18/18**);
> build → `BUILD OK`, 242 classes, zero `-Xlint:all` warnings; static audits clean
> (no file > 200 lines, no `@SuppressWarnings`, no hex outside `Theme`, no
> `service`/`controller` imports under `view`); inventory 154 source files /
> 14,450 lines plus 38 probe files / 4,287 lines (18 probes + 4 support classes).
>
> **Inputs:** `docs/implementation-plan.md` §4 (M6 row) and §5 (D27, D32, D37),
> `docs/design.md` §6–§10 and §13, `docs/SRS.md` §6.2, `docs/preferences.md`
> §1–§4, `HANDOFF-M5.md` §5 and §8, `HANDOFF-M5-PLAN.md` §16–§17.
>
> **Process:** one work package per turn (`docs/preferences.md` §2); every WP ends
> with a build + probe run and a report. No WP starts until the previous one is
> green and committed.

## 1. Objective

Deliver **M6 — polish**, the last milestone: thin scrollbars, the dialog and screen
fades, the deferred minimum-size and width fixes, the icon/DPI/skeleton audits,
and the full interactive SRS §6.2 walkthrough — then produce the presentation
deck as the final deliverable. No new product behavior, no schema change, no
rewriting M3–M5 screens beyond the wiring named in this plan.

M6 exists because the app is **feature-complete** (FR-01…FR-12 and NFR-01…NFR-08
are implemented and probed) but not yet **design-complete**: the Windows 11 /
Fluent specification is only partly realized, and the visual pass has never been
performed on a real display.

### Milestone exit criteria

| # | Criterion | Source |
| :-- | :--- | :--- |
| 1 | Every scroll surface uses the thin overlay-style bar: transparent track, 4 px thumb, 8 px hover, no arrow buttons, both themes | design §6 |
| 2 | Dialog open/close fade + 98→100 % scale and the 120 ms screen fade ship; the theme switches instantly (an allowed §7 motion, not needed for the demo); toasts already animate; nothing blocks the EDT | design §7 |
| 3 | The theme switch re-themes every mounted screen and both themes keep the §8 contrast ratios | design §8, SRS NFR-02 |
| 4 | Every §10 icon resolves to a real glyph on Windows icon fonts **and** to a non-tofu fallback on Linux | design §10 |
| 5 | At the 1024×640 minimum the preview shows all metadata rows and the pinned action; tables scroll horizontally instead of clipping badges, dates or money | design §9 |
| 6 | `PenaltiesTable` at 1280 px scrolls horizontally instead of clipping its last column; toasts no longer cover the status bar or the preview action bar | HANDOFF-M5 §5 |
| 7 | Skeleton audit recorded: photo decode/import gated, timing constants asserted, reduced motion honoured, watchdog proven, no skeleton outlives its work; query gating decided by measurement (D32 closed) | design §13, D32 |
| 8 | Full SRS §6.2 walkthrough passes interactively at 1920×1080 (1280×800 window) and 1366×768, plus 1024×640 and the DPI pass | SRS §6.2, design §8–§9 |
| 9 | `./build.sh clean && ./build.sh` and `sh ../../netbeans/extide/ant/bin/ant clean jar` green, zero warnings; `./test/run-all.sh` green with **20** registered probes | preferences §4 |
| 10 | README, docs, handoffs and the local `AGENTS.md` match shipped behavior in the same change | Golden Rule 1 |

## 2. Baseline and ground truth (verified 2026-11-16)

| Fact | Value |
| :--- | :--- |
| HEAD / remote | `97fc44c` == `origin/main`, clean tree |
| Build | `./build.sh` → `BUILD OK`, 242 classes, zero `-Xlint:all` warnings |
| Probes | 18/18 green, `LoansUiProbe PASS=105 FAIL=0` |
| Source | 154 files / 14,450 lines: 18 model, 16 service, 10 persistence, 10 controller, 62 view, `Main` |
| Probes | 38 files / 4,287 lines: 18 probes + `ProbeSupport`, `UiProbeSupport`, `EditorProbeSupport`, `PhotoProbeSupport` |
| Tallest | `PreviewPane` 198, `PenaltyPreviewPane` 195, `LoanPreviewPane` 194, `InventoryPreviewPane` 193, `CatalogTable` 183, `SkeletonPanel` 182, `LoansTable` 178, `SkeletonGate` 177, `PenaltiesScreen` 176, `Theme` 175, `InventoryScreen` 174, `PenaltiesTable` 171, `InventoryTable` 171, `DialogSkin` 163, `MainFrame` 161 |
| Scroll surfaces | five `JScrollPane`s: `CatalogTable`, `CatalogCards`, `LoansTable`, `PenaltiesTable`, `InventoryTable`; all use stock bars, `Theme.installDefaults()` sets no `ScrollBar*` key |
| Motion today | `ToastView` (slide 8 px + fade 167 ms) and `SkeletonPanel` (pulse), `SkeletonGate` (150/300/167/10 s). No dialog, screen or theme animation |
| Width policy today | `LoansWidthPolicy` only; `CatalogTable` (Σ 670 px), `InventoryTable` (Σ 720), `PenaltiesTable` (Σ 782) use `AUTO_RESIZE_SUBSEQUENT_COLUMNS` and squeeze |
| Theme | `Theme.apply(boolean)` + `Theme.addThemeListener`; `AppController.applyTheme` refreshes `MainFrame`, mounted screens, login; `SettingsScreen` wires the switch; `SkinProbe` asserts contrast in both token sets. **The toggle already shipped in M5** — M6 only audits it |
| Known carried defects | 1024×640 preview ID/caption clip and table badge truncation; `PenaltiesTable` 1280 px clip; toast overlaps preview action bar; 1366×768 interactive pass never done |
| Docs drift | README tree says “11 probes” (body says 18) and lists only 3 controllers; local `AGENTS.md` still describes the M3 tree |

### Facts that shape the plan

- **No new persisted state.** `schemaVersion` stays 1; nothing in M6 writes to the save file.
- **Four preview panes sit at 193–198 lines.** WP3 must extract shared code *before* adding compact metrics; otherwise the cap breaks.
- **`settle()` in `UiProbeSupport` does not wait.** Any animation that overlays content will corrupt pixel assertions in existing probes; motion must be switchable off for the suite (D39).
- **All UI probes run under `xvfb-run -a`.** `JDialog` opacity and window translucency are platform-dependent; every animation needs a guarded no-op fallback.
- **`SettingsScreen` already owns two session switches** (“Dark theme”, “Animate loading placeholders”); M6 adds no third switch.
- **The manual pass cannot be done headlessly.** It is a named deliverable of WP5, performed by the maintainer, and the agent never claims it from a probe.

## 3. Decisions (proposed; recorded as D38–D43)

| # | Question | Decision | Why |
| :-- | :--- | :--- | :--- |
| D38 | How close to “overlay” can the scrollbar get? | **Thin lane, not true overlay.** An 8 px lane with a transparent track, 4 px thumb, 8 px thumb on hover, radius 2, no arrow buttons; the lane still occupies layout space. | A zero-width overlay needs a custom `JScrollPane`/layer that fights selection, wheel and focus handling on five scroll surfaces; the visual target is met within the Swing constraint, and the deviation is recorded in design §6. |
| D39 | Which §7 motions ship? | **Dialog and screen transitions only**: dialog open/close fade + 98→100 % scale (83 ms) and the screen fade (120 ms). The theme switches **instantly**; the crossfade is an allowed §7 motion left unshipped. A package-private `-Dkitnapper.motion=off` switch disables the two that ship; probes run with it off except `MotionProbe`. | §7 lists these as allowed, not required. The crossfade is the only snapshot-based animation and SRS §6.2 never switches themes; the screen fade is seen on every navigation, so the risk budget goes there. The switch is not a user setting (the Settings toggle stays skeleton-only, design §13.4). |
| D40 | Do catalog/dashboard/loan queries get skeletons? | **No code change; close D32 by measurement.** The audit records per-query timings at seed scale and asserts they are far below the 150 ms gate; gating stays measurement-driven, as design §13.1 specifies. | A 10-item scan is microseconds; adding a worker to locked screens buys nothing and risks NFR-02 immediacy. |
| D41 | Which tables get the width policy? | **All four tables**: catalog, inventory, penalties, loans. Columns never shrink below preferred width; narrow viewports scroll horizontally. `LoansWidthPolicy` becomes the shared `TableWidthPolicy`. | Fixes the 1024×640 badge truncation and the 1280 px penalties clip with one rule, and matches the existing loan behavior. |
| D42 | How does the preview survive 1024×640? | **Compact density below 560 px of preview height**: tighter struts/row gaps and a one-line hint (tooltip keeps the full text); the 16:9 media and pinned action stay. | The current layout leaves roughly 4 px of slack at 640; a font-metric change or 125 % scaling overflows. Compact mode buys ~28 px without a scrollbar inside the preview. |
| D43 | Where do toasts sit? | **Bottom-right with a 96 px bottom inset and 16 px side inset**, clearing the status bar (32 px) and the pinned preview action (~64 px). | The current 16 px inset covers the status bar and the preview hint for 4 s; a constant inset is simpler and more honest than teaching `Toasts` about screen layouts. Record in design §6. |

### Locked constraints carried into M6 (do not re-litigate)

- **JDK only.** No FlatLaf, no animation library, no image library, no test framework.
- **`schemaVersion` stays 1.** No new persisted fields; no change to the save-file shape.
- **Locked domain rules** (instant checkout, no cancellation/extension, ₱50/day with live accrual frozen on return, 3-loan limit, 14-day window, manager read-only checkout, D15 maintenance rule) are untouched.
- **Layering:** views never import `controller`/`service`; services are the only mutators/savers.
- **File cap:** 200 lines hard, 100–150 target; split before extending a 190+ line file.
- **Theme tokens:** every hex stays in `view/Theme`.
- **EDT:** all Swing work on the EDT; no blocking work in paint paths.
- **One phase per turn; never commit without approval for the first WP, standing approval afterwards.**

## 4. Architecture and integration points

### 4.1 Scrollbars

`UIManager` is the single wiring point: `Theme.installDefaults()` registers
`ScrollBarUI = ThinScrollBarUI` and `ScrollBar.width = 8`. Every `JScrollPane`
created after startup picks the UI up; tokens are read at paint time, so a theme
switch needs no UI rebuild. The UI overrides track/thumb painting, removes the
arrow buttons, tracks hover with a mouse listener, and returns an 8 px preferred
cross-axis size.

### 4.2 Motion

One `Motion` facade owns durations, easing, and the guarded window helpers. A
package-private `FadeOverlay` paints a `Theme.bgApp` wash with a decreasing alpha
for screen changes. Overlays mount on the frame's `JLayeredPane.MODAL_LAYER` (200)
— above content, below toasts (`POPUP_LAYER`, 300) and never on the glass pane
that `DialogSkin` uses for the scrim. Dialog fades use `Window.setOpacity`,
guarded by `GraphicsDevice.isWindowTranslucencySupported(PERPIXEL_TRANSLUCENT)`;
unsupported platforms get the instant path. Theme changes apply immediately
(no crossfade, D39).

`Motion.enabled()` reads `kitnapper.motion` (`off` disables). `run-all.sh` passes
`-Dkitnapper.motion=off` to every display probe except `MotionProbe`; production
runs with motion on.

### 4.3 Geometry

- `TableWidthPolicy` generalizes the existing loan policy: `install(table,
  widths, pane)` flips `AUTO_RESIZE_OFF` + horizontal scroll when the viewport
  is narrower than the sum, and `AUTO_RESIZE_SUBSEQUENT_COLUMNS` when it fits.
- `PreviewLayout` extracts the repeated label/row/caption builders from the four
  preview panes and exposes `compactFor(height)` plus the compact struts/gaps.
  Each pane keeps its own rows and action bar; only the density values and the
  row factory are shared.
- `Toasts` gains `SIDE_MARGIN = 16`, `BOTTOM_MARGIN = 96`.

### 4.4 Audits

- `FontProbe` gains the §10 codepoint table: every fluent codepoint maps to a
  fallback that the resolved UI font can display; a missing fallback is replaced
  and design §10 is updated.
- The suite is run at 100/125/150 % via `-Dsun.java2d.uiScale` (approximate
  proxy; the Windows pass is authoritative).
- Query timings are measured and printed by `CatalogProbe`/`LoanProbe`, then
  recorded in the plan and design §13; D40 closes with numbers.
- The skeleton audit reuses the existing `PhotoTileChecks` assertions
  (150/300/167/10 s, reduced motion, failure path) and adds nothing unless a gap
  is found.

## 5. API contracts

### 5.1 `view/ThinScrollBarUI` (new, public)

```java
public final class ThinScrollBarUI extends BasicScrollBarUI {
    static final int LANE = 8;
    static final int THUMB = 4;
    static final int THUMB_HOVER = 8;
    static final int RADIUS = 2;

    protected JButton createDecreaseButton(int orientation); // 0×0, no focus
    protected JButton createIncreaseButton(int orientation); // 0×0, no focus
    protected void paintTrack(Graphics g, JComponent c, Rectangle r); // nothing
    protected void paintThumb(Graphics g, JComponent c, Rectangle r);  // rounded, Theme.scrollThumb
    public Dimension getPreferredSize(JComponent c);          // 8 px cross-axis
}
```

Public because `UIManager` instantiates it reflectively; no business logic.

### 5.2 `view/Motion` (new, public) + `view/FadeOverlay` (new, package-private)

```java
public final class Motion {
    public static final int FAST_MS = 83;
    public static final int SCREEN_MS = 120;
    public static final int TICK_MS = 16;

    public static boolean enabled();                  // kitnapper.motion != "off"
    public static float ease(float t);                // 0→0, 1→1, monotone decelerate
    public static void openDialog(JDialog dialog);    // fade 0→1 + 98→100 % size
    public static void closeDialog(JDialog dialog, Runnable after); // fade 1→0, then after
    public static void fadeScreen(JComponent content); // 120 ms wash over the routed content
}
```

`FadeOverlay(Color wash, int durationMs, Runnable done)` paints in
`paintComponent`, ticks with `javax.swing.Timer`, removes itself when finished, and
overrides `contains` to return `false` so clicks pass through during the animation.
Only one overlay per root pane (client property key), and no overlay when
`!Motion.enabled()`, the window is not showing, or the tree is headless.

### 5.3 `view/TableWidthPolicy` (new, package-private; replaces `LoansWidthPolicy`)

```java
final class TableWidthPolicy {
    static void install(JTable table, int[] widths, JScrollPane pane); // evaluates once, then on resize
    static void applyPreferred(JTable table, int[] widths);
    static int total(int[] widths);
}
```

Callers: `CatalogTable` (`COLUMN_WIDTHS`), `InventoryTable` (`COLUMN_WIDTHS`),
`PenaltiesTable` (widths from `PenaltiesTableModel.widthOf`), `LoansTable`
(widths from `LoansTableModel.widthOf`). `LoansLayoutProbe` switches its one call
to `total(...)` or the model helper.

### 5.4 `view/PreviewLayout` (new, package-private)

```java
final class PreviewLayout {
    static final int PAD = 16;
    static final int COMPACT_BELOW = 560;

    static boolean compactFor(int previewHeight);
    static int mediaGap(boolean compact);   // 12 / 8
    static int rowGap(boolean compact);     // 10 / 6
    static JPanel row(String label, JComponent value, boolean compact);
    static String captionLines(String caption, boolean compact); // 1 line + tooltip when compact
}
```

The four preview panes replace their private `row()`/`captionLines()` with these
helpers, then call a small `applyDensity(int height)` when the compact threshold
flips. `PreviewPane` must lose lines before it gains any: the extraction is the
first edit in WP3.

### 5.5 `view/Toasts` (modified)

`MARGIN` splits into `SIDE_MARGIN = 16` and `BOTTOM_MARGIN = 96`; `layout()` uses
both. No API change.

## 6. File inventory

### 6.1 New (7 files, ~700 lines)

| File | Contents | Est. |
| :--- | :--- | ---: |
| `view/ThinScrollBarUI.java` | scrollbar skin (D38) | 120 |
| `view/Motion.java` | durations, easing, dialog/screen helpers (D39) | 130 |
| `view/FadeOverlay.java` | wash overlay, timer, click-through (D39) | 70 |
| `view/TableWidthPolicy.java` | shared width policy (D41) | 70 |
| `view/PreviewLayout.java` | shared row factory + compact metrics (D42) | 90 |
| `test/kitnapper/view/MotionProbe.java` | motion constants, easing, overlay lifecycle | 140 |
| `test/kitnapper/view/GeometryProbe.java` | 1024×640 / 1280×800 / 1366×768 / 1920×1080 layout checks | 170 |

### 6.2 Modified (production)

| File | Change | Net |
| :--- | :--- | ---: |
| `view/Theme.java` | register `ScrollBarUI` + `ScrollBar.width` (175 → ~180) | +5 |
| `view/DialogSkin.java` | open/close through `Motion`; dispose after fade (163 → ~180) | +17 |
| `view/MainFrame.java` | `Motion.fadeScreen(content)` after `cards.show` (161 → ~172) | +11 |
| `view/Toasts.java` | split margins, bottom 96 (110 → ~114) | +4 |
| `view/SettingsScreen.java` | vertical scroll only if WP3 measures overflow at 640 (119 → ~125) | +6 |
| `view/CatalogTable.java` | `TableWidthPolicy` wiring (183 → ~186) | +3 |
| `view/InventoryTable.java` | `TableWidthPolicy` wiring (171 → ~174) | +3 |
| `view/PenaltiesTable.java` | `TableWidthPolicy` wiring (171 → ~174) | +3 |
| `view/LoansTable.java` | `TableWidthPolicy` wiring; delete the old call (178 → ~180) | +2 |
| `view/PreviewPane.java` | extract rows/caption; add density flip (198 → ≤190) | −8 |
| `view/PenaltyPreviewPane.java` | same extraction (195 → ≤190) | −5 |
| `view/LoanPreviewPane.java` | same extraction (194 → ≤190) | −4 |
| `view/InventoryPreviewPane.java` | same extraction (193 → ≤190) | −3 |
| `view/LoansWidthPolicy.java` | **deleted**; replaced by `TableWidthPolicy` | −52 |

### 6.3 Modified (probes / harness)

| File | Change |
| :--- | :--- |
| `test/run-all.sh` | `-Dkitnapper.motion=off` for display probes; `MotionProbe` runs without it; `KITNAPPER_UI_SCALE` passes `-Dsun.java2d.uiScale`; two new registrations (20 total) |
| `test/kitnapper/SkinProbe.java` | 6 scrollbar checks (UI installed, lane, no arrows, thumb paint, hover width, both themes) |
| `test/kitnapper/FontProbe.java` | §10 codepoint table (fluent + fallback renderable) |
| `test/kitnapper/CatalogProbe.java` | catalog query timing, printed and asserted < 150 ms |
| `test/kitnapper/LoanProbe.java` | loan/overdue query timing, printed and asserted < 150 ms |
| `test/kitnapper/view/LoansLayoutProbe.java` | one call site updated to `TableWidthPolicy` |

### 6.4 Line-cap strategy

The four preview panes are the only real pressure. WP3 starts with the
`PreviewLayout` extraction (row factory + caption), which removes ~20 lines from
each, then adds the density flip (~10). If any file still lands above 190, split
by section (`PreviewInfo` / `PreviewActionBar`) before continuing. No M6 file may
exceed 200 lines, and `PreviewPane` must end at 190 or lower to leave headroom.

## 7. Work packages (execution order)

### WP0 — Plan, preflight, and contract commit

- Move this file to the repository root as `HANDOFF-M6-PLAN.md` (same content).
- Re-run: `./build.sh clean && ./build.sh`; `./test/run-all.sh`; static audits.
- Record HEAD, class count, probe tally, and audit results in §16.
- Commit `docs(m6): author the M6 implementation backbone` (signed, co-authors).
  **Needs a direct “yes” for this first git write; later WPs use standing approval.**

**Exit:** plan committed, pushed, remote SHA reported, baseline log entry present.

### WP1 — Thin scrollbars (D38)

- Add `ThinScrollBarUI`; register it in `Theme.installDefaults()`.
- Extend `SkinProbe` with the six scrollbar checks.
- Verify with a headless paint: thumb pixels use `Theme.scrollThumb` in both themes.

**Exit:** `SkinProbe` green; build + suite green; no file > 200; commit
`feat(m6): add thin scrollbars`.

### WP2 — Fluent motion (D39)

- Add `Motion` + `FadeOverlay`.
- `DialogSkin`: fade in after `pack()/resize()`; route both confirm and cancel
  close paths through `Motion.closeDialog`; keep the accepted result intact.
- `MainFrame.showScreen`: fade the routed content after `cards.show`, skipping
  the first screen and non-showing frames.
- `AppController.applyTheme` stays unchanged: the theme switch is instant (D39).
- `run-all.sh`: motion-off for the display suite; register `MotionProbe`.
- `MotionProbe` asserts constants, easing endpoints/monotonicity, overlay
  presence/removal on a screen fade, no overlay on a theme switch, and no
  overlay when motion is off (child JVM or a package-private setter).

**Exit:** `MotionProbe PASS`; every other display probe green with motion off;
`MotionProbe` green with motion on; commit
`feat(m6): add the Fluent motion fades`.

**Risk gate:** if the 98→100 % scale causes visible text reflow, keep the fade
and record the scale as a deliberate deviation in design §7 rather than adding
layout complexity. Two scope calls are recorded up front — the theme crossfade is
not shipped (D39) and the dialog scale may be dropped after the visual check —
and both are written into design §7, never left silent.

### WP3 — Minimum-size and width fixes (D41–D43)

1. **Measure first:** render catalog, inventory, penalties and Settings at 1024×640,
   1280×800, 1366×768 and 1920×1080; print the overflowing bounds. No fix before numbers.
2. Replace `LoansWidthPolicy` with `TableWidthPolicy`; wire all four tables.
3. Extract `PreviewLayout`; add compact density to all four previews.
4. Split `Toasts` margins; bottom inset 96.
5. Add `GeometryProbe`; update `LoansLayoutProbe` and `run-all.sh`.

**Exit:** at 1024×640 every preview row and the pinned action are inside the
preview; tables scroll horizontally and every cell keeps its preferred width;
`PenaltiesTable` at 1280 scrolls; toasts clear the status bar and action bar;
`GeometryProbe PASS`; commit
`fix(m6): keep tables and previews usable at the minimum size`.

### WP4 — Icon, DPI and skeleton audits (D40)

- Extend `FontProbe` with the §10 codepoint pairs; fix any fallback the UI font
  cannot display and update design §10 in the same change.
- Run the suite at `KITNAPPER_UI_SCALE=1.25` and `1.5`; record failures and fix
  only real layout breaks (no redesign).
- Add query timings to `CatalogProbe`/`LoanProbe`; record the numbers; close D32
  with “no gating: measured X ms at seed scale”.
- Walk the design §13 audit list against the existing assertions: delay 150,
  minimum 300, fade 167, watchdog 10 s, reduced motion, failure fallback,
  no skeleton outlives work. Add a probe assertion only where evidence is
  missing.
- Theme-sweep audit: switch both themes on every screen; confirm the explicit
  `refreshTheme` path reaches every mounted screen.

**Exit:** audit numbers and results in §16; `FontProbe` green; no probe
regressions; commit `test(m6): audit icons, DPI and skeleton gating` (plus a
`fix(m6)` commit if a real defect was fixed).

### WP5 — Full verification and M6 close-out

- `./build.sh clean && ./build.sh`; `sh ../../netbeans/extide/ant/bin/ant clean jar`.
- `./test/run-all.sh` → 20/20; static audits; `schemaVersion` still 1.
- Interactive SRS §6.2 walkthrough (maintainer, real display) at 1920×1080
  (1280×800 window) and 1366×768, plus 1024×640 and the scaling matrix; record
  machine, date, resolutions and any deviation.
- Update docs (§10 below) and `HANDOFF-M6.md`; refresh local `AGENTS.md`
  (never committed).
- Commit `docs(m6): verify and close M6`.

**Exit:** every criterion in §1 checked with evidence; docs and code agree; plan
§16 complete. If the interactive pass cannot be performed, M6 stays open — it is
not replaced by probes.

### WP6 — Presentation deck (final deliverable, after M6)

- Use the PDF skill; produce the deck only when WP5 is closed.
- Outline: title/team, problem, demo flow (SRS §6.2 highlights), architecture
  (MVC + layering), OOP evidence (inheritance, polymorphism, strategy,
  encapsulation, exceptions), persistence, verification numbers, limitations,
  lessons learned.
- Output `assets/kitnapper-presentation.pdf` (or the team’s chosen name), commit
  `docs(m6): add the presentation deck`.

**Exit:** PDF opens, matches the shipped app, and is committed.

## 8. Probe plan

### 8.1 Harness changes

- `run(probe, mode)` gains an optional JVM-args argument.
- Display probes run with `-Dkitnapper.motion=off`; `MotionProbe` runs with
  motion on; `KITNAPPER_UI_SCALE` (unset by default) adds
  `-Dsun.java2d.uiScale=<value>`.
- Registration order stays alphabetical-by-package; suite count 18 → **20**.

### 8.2 `SkinProbe` additions (headless)

1. `scrollbar-ui-installed` — a new `JScrollBar` uses `ThinScrollBarUI`.
2. `scrollbar-lane` — vertical preferred width 8, horizontal height 8.
3. `scrollbar-no-arrows` — both buttons are 0×0 and non-focusable.
4. `scrollbar-thumb-paints` — painting a 100 px bar produces `Theme.scrollThumb`
   pixels inside the 4 px lane.
5. `scrollbar-hover-widens` — the hover flag changes the painted thumb from 4 to
   8 px (package-private probe hook).
6. `scrollbar-light-theme` — the thumb uses the light token after `Theme.apply(false)`.

### 8.3 `MotionProbe` (display, motion on)

- Constants are 83/120/167/16; `ease(0)=0`, `ease(1)=1`, monotone.
- A shown frame + `showScreen` creates exactly one `FadeOverlay` and removes it
  after the duration.
- A theme switch applies immediately and creates no overlay (D39).
- `Motion.enabled()` is false when the property is `off` (checked in a child JVM
  or by a package-private test setter).
- No overlay is created for a non-showing frame.

### 8.4 `GeometryProbe` (display)

- Mounts catalog (borrower), inventory, penalties and Settings (manager) at
  1024×640, 1280×800, 1366×768 and 1920×1080.
- Asserts every preview metadata label and the action bar are inside the preview
  bounds; no negative/overlapping child bounds.
- Asserts tables at 1024×640 and penalties at 1280 use
  `AUTO_RESIZE_OFF` + a visible horizontal scrollbar; at wider sizes they stretch.
- Asserts toast bounds do not intersect the status bar or the preview action bar.
- Asserts Settings content fits or is scrollable at 1024×640.
- Paints with `printAll` and records a non-blank pixel count per size.

### 8.5 `FontProbe` / `CatalogProbe` / `LoanProbe`

- Font: every §10 (fluent, fallback) pair returns a non-empty glyph and the
  fallback is displayable by the resolved UI/icon font.
- Catalog: time `CatalogService` filtering over the seed; print min/median/max
  and assert `< 150 ms`.
- Loan: time overdue/all-loans/dashboard queries; print and assert `< 150 ms`.

### 8.6 Expected suite after M6

`DataProbe`, `CatalogProbe`, `LoanProbe`, `InventoryProbe`, `SkinProbe`,
`FontProbe`, `EdgeProbe`, `SizeProbe`, `FrameProbe`, `CloseProbe`,
`CatalogUiProbe`, `PhotoUiProbe`, `InventoryEditorProbe`, `InventoryUiProbe`,
`SettingsUiProbe`, `M5FlowProbe`, `LoansLayoutProbe`, `LoansUiProbe`,
`MotionProbe`, `GeometryProbe` → **20/20 green**.

## 9. Manual verification (WP5, maintainer)

| # | Check | Pass condition |
| :-- | :--- | :--- |
| 1 | SRS §6.2 steps 1–12 at 1920×1080 (1280×800 window) | every step behaves as written |
| 2 | Same walkthrough at 1366×768 | no clipping; horizontal scroll where designed |
| 3 | 1024×640 minimum | preview complete, tables scroll, no truncated badges |
| 4 | Windows scaling 100/125/150 % | no clipped text or controls; window controls reachable |
| 5 | Both themes | §8 contrast holds; focus rectangle visible; badges keep dot + word |
| 6 | Keyboard | tab order visual, Enter confirms, Esc closes, Space toggles, arrows navigate, Ctrl+F focuses search |
| 7 | Icons | no tofu on Windows icon fonts; fallbacks legible on Linux |
| 8 | Motion | dialog fade/scale, screen fade; the theme switches instantly; no jank; reduced-motion switch still stops skeletons |
| 9 | Toasts | clear of status bar and preview action bar; max 3 stacked |
| 10 | Skeleton audit | delay/minimum/fade/watchdog/reduced motion/failure path; no permanent skeleton |
| 11 | Gradient spot-check | only `GradientTile` uses a gradient (design §3.4) |
| 12 | Restart + reset | state survives restart; reset restores §6.1 and wipes photos |

Screenshots are captured for the presentation (WP6) but **not committed** unless
the team wants them; results are logged with date, machine, resolution and scaling.

## 10. Documentation updates (same change as behavior)

| File | Update |
| :--- | :--- |
| `README.md` | Status → M6 complete; file tree (controller list, probe count); probe list 20; remove “Next: M6”; add the deck |
| `docs/implementation-plan.md` | §2.2/§2.4 inventory rows for the five new classes; §4 M6 row → delivered (note that the theme toggle shipped in M5); §5 D38–D43; revision 2.1.0 |
| `docs/design.md` | §6 scrollbar lane + toast inset; §7 shipped motion (and the crossfade that is not shipped); §9 width-policy extension; §10 verified fallbacks; §13 audit result and D32 closure; decision log entry |
| `docs/SRS.md` | Revision 3.3.0: M6 verification evidence, min-size and scaling notes; **no requirement change** |
| `HANDOFF-M6.md` | New close-out note: what shipped, deviations, gotchas, commands, next steps |
| `docs/preferences.md` | Only if the process changes (expected: no change) |
| `AGENTS.md` (local, gitignored) | Current-state table, inventory counts, probe count, M6 status — **never staged** |

If a design deviation is accepted (D38 lane, D42 compact density, D43 toast
inset, or the motion-scale fallback), it is recorded in design.md in the same
commit as the code.

## 11. Git plan

| WP | Commit | Type |
| :-- | :--- | :--- |
| WP0 | `docs(m6): author the M6 implementation backbone` | docs |
| WP1 | `feat(m6): add thin scrollbars` | feat |
| WP2 | `feat(m6): add the Fluent motion fades` | feat |
| WP3 | `fix(m6): keep tables and previews usable at the minimum size` | fix |
| WP4 | `test(m6): audit icons, DPI and skeleton gating` (+ `fix(m6)` only for real defects) | test/fix |
| WP5 | `docs(m6): verify and close M6` | docs |
| WP6 | `docs(m6): add the presentation deck` | docs |

Rules: signed SSH commits, author + both co-author trailers, one logical change,
push after each green WP, report local SHA + remote SHA + GitHub verified badge.
The WP0 plan commit needs a direct “yes”; WPs 1–6 use the standing approval for
documented tasks. Never tag/branch/amend/revert without a new “yes”.

## 12. Risks and mitigations

| Risk | Impact | Mitigation |
| :--- | :--- | :--- |
| Motion overlays corrupt existing paint probes | High | `-Dkitnapper.motion=off` for the suite; `MotionProbe` is the only motion-on run; `printAll` for captures |
| `JDialog.setOpacity` unsupported under Xvfb or on a platform | Medium | Guard with `isWindowTranslucencySupported`; instant fallback; the dialog still opens/closes |
| 98→100 % dialog scale reflows text | Medium | Visual gate in WP2; drop the scale and record the deviation rather than complicating layout |
| Preview panes hit the 200-line cap | High | `PreviewLayout` extraction first; split by section if still >190; never suppress |
| Width policy changes sort/selection behavior | Medium | `GeometryProbe` + existing `CatalogUiProbe`/`LoansUiProbe`/`InventoryUiProbe`; policy only toggles resize mode |
| Global `ScrollBarUI` affects combo popups/file chooser | Low | Intended skin; smoke-test the file chooser and a combo popup in WP5 |
| DPI proxy on Linux ≠ Windows scaling | Medium | Treat 1.25/1.5 as a smoke test; the Windows 100/125/150 % pass is authoritative |
| Interactive pass unavailable (headless session) | High | WP5 explicitly requires the maintainer; M6 stays open until recorded; never claim it from probes |
| Docs drift during a long milestone | Medium | Docs update in the same commit as each behavior change; WP5 audits the whole set |
| Theme switch reads as abrupt without the crossfade | Low | Accepted by D39; the switch is one click in Settings and the token refresh is immediate |
| Time: motion is the largest unknown | Medium | WP2 risk gate; WP1/WP3/WP4 are bounded; deck last |

## 13. Definition of Done

- [ ] All ten §1 exit criteria checked with evidence.
- [ ] `./build.sh clean && ./build.sh` green, zero `-Xlint:all` warnings.
- [ ] `sh ../../netbeans/extide/ant/bin/ant clean jar` green.
- [ ] `./test/run-all.sh` → `ALL PROBES GREEN`, **20/20**, with `SkinProbe`,
      `FontProbe`, `MotionProbe`, `GeometryProbe` counts recorded.
- [ ] Suite re-run at 1.25 and 1.5 UI scale; results logged.
- [ ] Static audits: no file > 200 lines, no `@SuppressWarnings`, no hex outside
      `Theme`, no `service`/`controller` imports under `view`, no `System.exit`
      outside `Main`.
- [ ] `schemaVersion` remains 1; no save-file shape change.
- [ ] SRS §6.2 interactive pass recorded at 1920×1080 (1280×800 window) and
      1366×768, plus 1024×640.
- [ ] README, implementation-plan, design, SRS revision, `HANDOFF-M6.md` updated
      in the same change as behavior.
- [ ] Every WP committed, pushed, remote SHA confirmed, GitHub verified badge.
- [ ] `HANDOFF-M6-PLAN.md` §16 execution log complete with commands and results.

## 14. Out of scope (do not do in M6)

- New features or requirements; inventory filter chips/layout toggle (follow-up).
- Schema changes, new persisted fields, export/import of the save file.
- Rewriting M3–M5 behavior beyond the named wiring.
- Animation beyond design §7 (no hover animation, bounce, overshoot, ripples).
- Drag-and-drop photos, cloud/hosted images, networking, real auth, accounts.
- Packaging/installer, code signing, auto-update.
- UML diagrams (not required).

## 15. Follow-ups after M6

1. Presentation deck (WP6) and a dry run of the demo on the presentation machine.
2. `AGENTS.md` refresh on every checkout (local only).
3. Optional: inventory filter chips + cards layout for demo parity.
4. Optional: photo drag-and-drop onto the well.

## 16. Execution log

> Filled as work packages land. Every entry records the starting HEAD, the
> commands run, the concrete results, and the commit SHA / remote SHA.

### WP0 — plan authored and baseline re-proved (2026-11-16)

- Starting HEAD: `97fc44c` (clean, local == remote).
- `cd desktop-app && ./build.sh clean && ./build.sh` → `BUILD OK`, 242 classes.
- `./test/run-all.sh` → `ALL PROBES GREEN`, 18/18.
- Static audits: no file > 200 lines; no `@SuppressWarnings`; no hex outside
  `Theme`; no `service`/`controller` imports under `view`.
- Baseline inventory: 154 source files / 14,450 lines; 38 probe files / 4,287
  lines; 18 registered probes + 4 support classes.
- Plan committed as `7ba6725` (`docs(m6): author the M6 implementation backbone`)
  and pushed; remote `main` at the same SHA, GitHub `verified: true` (`valid`).
- Amendment before the WP0 commit (review): D39 cut the theme crossfade (dialog and
  screen fades only) and D43 kept the 96 px toast inset; §1, §3–§6, WP2, §8 and the
  risk table were adjusted together.

### WP1 — thin scrollbars shipped (2026-11-16)

- Starting HEAD: `b573514` (clean, local == remote).
- Added `view/ThinScrollBarUI` (148 lines): `BasicScrollBarUI` subclass with an
  own static `createUI` (the inherited one would return a stock bar), 8 px lane,
  transparent track (`setOpaque(false)` + no track paint), 4 px thumb widening to
  the full lane on `mouseEntered` (no overlay animation, no repaint timer), 0×0
  non-focusable arrow buttons, and a cross-axis `getPreferredSize` of 8 px.
- `Theme.installDefaults()` registers `ScrollBarUI = ThinScrollBarUI` and
  `ScrollBar.width = 8` (175 → 177 lines). Tokens are read at paint time.
- `SkinProbe` delegates six checks to the new package-private
  `test/kitnapper/ScrollBarChecks` (125 lines) so the probe itself stays small
  (128 → 132): `scrollbar-ui-installed`, `scrollbar-lane`,
  `scrollbar-no-arrows`, `scrollbar-thumb-paints`, `scrollbar-hover-widens`,
  `scrollbar-light-theme`. Hover is driven by real `MouseEvent`s, not a test
  hook, and thumb width is measured from painted pixels (4 columns → 8).
- The package-private probe hook sketched in §8.2 was **not** needed: the
  hover flag is exercised through the public event path, which is stricter.
- `./build.sh` → `BUILD OK`, 244 classes, zero warnings.
- `./test/run-all.sh` → `ALL PROBES GREEN`, 18/18; `SkinProbe PASS=10 FAIL=0`
  (was 4 checks). Static audits clean: no file > 200 lines, no
  `@SuppressWarnings`, no hex outside `Theme`, no `service`/`controller` imports
  under `view`.
- Docs in the same change: design §6 records the D38 lane deviation, §11 notes
  it, and the §14.2 decision log gains `D-Scrollbars`.
- Commit `feat(m6): add thin scrollbars`.

### WP2 — pending

### WP3 — pending

### WP4 — pending

### WP5 — pending

### WP6 — pending
