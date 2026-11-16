# HANDOFF — M6 (polish) — closed 2026-11-17

> **Status (2026-11-17):** M6 is **closed**. WP0–WP4 shipped; WP5's automated half
> (clean build, Ant, 20/20 probes at three UI scales, static audits) and the
> interactive SRS §6.2 walkthrough are both recorded. The walkthrough found
> fifteen defects, all fixed at the root in one work package (D44–D49) with new
> probes. `HANDOFF-M6-PLAN.md` holds the plan, the work packages, and the full
> execution log (§16); WP6 (the presentation deck) is the only milestone left.

## 1. Where things stand

- Build: `cd desktop-app && ./build.sh clean && ./build.sh` → `BUILD OK`,
  **255 classes**, zero `-Xlint:all` warnings.
- Probes: `cd desktop-app && ./test/run-all.sh` → `ALL PROBES GREEN`, **20/20**.
  Key tallies: `GeometryProbe PASS=122` (plus 8 status-column checks across four
  sizes), `CatalogUiProbe PASS=33`, `LoansUiProbe PASS=105`, `LoanProbe PASS=40`,
  `PhotoUiProbe PASS=31`, `CatalogProbe PASS=31`, `FrameProbe PASS=14`,
  `MotionProbe PASS=14`, `SkinProbe PASS=10`, `FontProbe PASS=5`.
- DPI matrix: `KITNAPPER_UI_SCALE=1.25 ./test/run-all.sh` and `=1.5` → both
  `ALL PROBES GREEN`, 20/20, no layout break.
- Inventory: **160 source files / 15,469 lines** (18 model, 26 service, 10
  persistence, 14 controller, 91 view, `Main`) plus **50 probe files / 5,594
  lines** and 20 registered probes.
- Tallest source: `view/LoanPreviewPane.java` **199**. Tallest probe:
  `view/M5FlowProbe.java` / `view/LoansUiChecks.java` 176,
  `view/SkeletonAuditChecks.java` 170. Split before adding to any of them.

## 2. What M6 shipped

| WP | Delivered |
| :-- | :--- |
| WP0 | `HANDOFF-M6-PLAN.md` (plan + decisions D38–D43), baseline re-proved |
| WP1 | `ThinScrollBarUI` + `UIManager` registration: 8 px lane, transparent track, 4 px thumb widening to the lane on hover, no arrows, paint-time tokens; six `ScrollBarChecks` assertions (D38) |
| WP2 | `Motion` + `FadeOverlay`: 83 ms dialog fade with 98→100 % grow, 120 ms screen wash on `MODAL_LAYER`, toasts on the shared constants, `-Dkitnapper.motion=off` probe switch, `MotionProbe` (D39) |
| WP3 | `TableWidthPolicy` for all four tables, `PreviewLayout` compact density + metadata scroll guard, preview-aware toasts, `GeometryProbe` (D41–D43) |
| WP4 | `GlyphChecks`, `QueryTimingChecks`, `SkeletonAuditChecks`, `ThemeSweepChecks`, `KITNAPPER_UI_SCALE` in the harness, the icon font fix, D32/D40 closed |
| WP5 | Interactive SRS §6.2 pass on a live display; fifteen findings fixed (F1–F7, F9–F13, F15) with `ToastChecks`, `FocusCueChecks`, `DialogTextChecks`, `ComboSkin`, `FileChooserSkin`, per-route status, status-column sizing (D44–D49) |

## 3. Deviations accepted (recorded in `docs/design.md`)

- **D38 — thin lane, not a true overlay.** A zero-width overlay needs a custom
  `JScrollPane` that fights selection, wheel and focus on five surfaces; the 8 px
  lane keeps layout space and meets the visual target.
- **D39 — no theme crossfade.** It is an allowed §7 motion, not a required one;
  the switch stays instant and `-Dkitnapper.motion=off` is a probe switch, never
  a user setting.
- **D42 — compact density plus a scroll guard.** Measurement, not a guess: the
  1024×640 shortfall is 48 px (catalog) and 174 px (inventory), so compacting
  alone could not work and the action bar is pinned outside the guard.
- **D43 — toasts shift left of a showing preview.** Any bottom-anchored toast
  crosses the ≥ 120 px action bar, so the 96 px inset stays and the column is
  avoided instead.
- **D-Icons — fallback glyphs use the symbol font.** `Fonts.icons()` used to
  return the physical Inter font, which lacks 8 of the 12 landscape glyphs; the
  title-bar brand, window controls and every category tile were tofu on Linux.
- **§13.5 — the skeleton opts out of focus explicitly.** This JDK reports
  `JPanel.isFocusable() == true`; the promise now holds by construction.

## 4. Commands

```bash
cd desktop-app
./build.sh clean && ./build.sh
./test/run-all.sh
KITNAPPER_UI_SCALE=1.25 ./test/run-all.sh
KITNAPPER_UI_SCALE=1.5  ./test/run-all.sh
sh ../../netbeans/extide/ant/bin/ant clean jar   # not +x on this machine
```

Static audits used at close-out: no file > 200 lines, no `@SuppressWarnings`, no
hex outside `Theme`, no `service`/`controller` imports under `view`, no
`System.exit` outside `Main`, `GradientTile` the only `GradientPaint`,
`schemaVersion` still 1.

## 5. The interactive SRS §6.2 pass — recorded 2026-11-17

Executed on a live 1920×1080 X display (Ubuntu, JDK 17), driver-clicked and
screenshot-captured, no window manager. Steps 1–12 in order: sign-in error,
borrower search + checkout with a `T+3` due date from the `T … T+14` picker,
restart persistence, manager dashboard, overdue return with a live ₱200.00 fee,
blank-reason waive refusal, inventory add/photo/edit/decommission, reset. Size
sweep 1920×1080 / 1366×768 / 1024×640; both themes; keyboard (Ctrl+F, tab order,
arrows, Space, Esc, Enter). Fifteen findings, all fixed at the root (D44–D49);
the exit criteria are recorded in `HANDOFF-M6-PLAN.md` §16, and the demo data is
back at the §6.1 baseline.

**F6 note:** the preview metadata sits in the D42 scroll guard at 1024×640 and
does not clip the pinned action; `GeometryChecks` asserts every showing label
inside the preview and no overlap with the action bar at four sizes.

## 6. Gotchas

- Every display probe runs with `-Dkitnapper.motion=off`; only `MotionProbe` runs
  with motion on, and it fails four checks by design when forced off.
- `Fonts.text()` is the **bundled physical Inter** on Linux. Anything drawn with
  it must be a character Inter has; icons and fallbacks go through
  `Fonts.icons()` / `Fonts.symbol()`.
- `PhotoUiProbe` takes ~13 s because the watchdog check waits the real 10 s.
- Seed items carry no photos, so `GradientTile` is what every catalog item shows —
  it is the highest-traffic consumer of the §10 category glyphs.
- Seed freezes no penalties: a penalties probe must add `Penalty` rows itself.
- Run probes serially; `run-all.sh` wipes and recompiles `test/out`.
