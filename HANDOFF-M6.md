# HANDOFF — M6 (polish) — WP0–WP4 delivered, WP5 verification recorded

> **Status (2026-11-16):** M6 work packages **WP0–WP4 are delivered, verified and
> pushed**; WP5's automated half (clean build, Ant, 20/20 probes at three UI
> scales, static audits, docs) is recorded here. **M6 is not closed:** the
> interactive SRS §6.2 walkthrough in §5 below is the maintainer's and has not
> been performed. The executable contract is `HANDOFF-M6-PLAN.md`, which holds
> the plan, the work packages, and the full execution log (§16).

## 1. Where things stand

- `main` @ `840e3f3` before the WP5 documentation commit; tree clean, local = remote.
- Build: `cd desktop-app && ./build.sh clean && ./build.sh` → `BUILD OK`,
  **253 classes**, zero `-Xlint:all` warnings.
- Probes: `cd desktop-app && ./test/run-all.sh` → `ALL PROBES GREEN`, **20/20**.
  Key tallies: `GeometryProbe PASS=122`, `LoansUiProbe PASS=105`, `LoanProbe
  PASS=40`, `PhotoUiProbe PASS=31`, `CatalogProbe PASS=31`, `MotionProbe PASS=12`,
  `SkinProbe PASS=10`, `FontProbe PASS=5`.
- DPI matrix: `KITNAPPER_UI_SCALE=1.25 ./test/run-all.sh` and `=1.5` → both
  `ALL PROBES GREEN`, 20/20, no layout break.
- Inventory: **158 source files / 15,059 lines** (18 model, 26 service, 10
  persistence, 14 controller, 89 view, `Main`) plus **46 probe files / 5,271
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
| WP5 | This note, README, SRS 3.3.0, plan §16 — automated verification recorded; **interactive pass pending** |

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

## 5. Remaining: the interactive SRS §6.2 pass (maintainer)

Run the app on a real display and record machine, date, resolutions and any
deviation. The checklist verbatim is `HANDOFF-M6-PLAN.md` §9; the parts a probe
cannot prove are: no clipping at 1366×768 and 1024×640 on screen, Windows
100/125/150 % scaling, both themes' contrast and focus rectangle, keyboard order,
**no tofu on Windows icon fonts**, motion feel (dialog fade/scale, screen fade,
instant theme switch), toast placement, and restart + reset behavior.

M6 closes only when that record exists; probes are not a substitute. Then: WP6
(presentation deck), `HANDOFF-M6.md` status → closed, and the local `AGENTS.md`
refresh.

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
