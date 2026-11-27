# KitNapper

<p align="center">
  <img src="assets/banner.jpg" alt="KitNapper banner" width="100%" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-informational?style=flat-square" alt="Java 17" />
  <img src="https://img.shields.io/badge/build-Ant%20%2F%20NetBeans-informational?style=flat-square" alt="Build" />
  <img src="https://img.shields.io/badge/dependencies-none-informational?style=flat-square" alt="Dependencies" />
  <img src="https://img.shields.io/badge/status-complete-brightgreen?style=flat-square" alt="Status" />
</p>

A desktop application for managing equipment loans at a small institution. KitNapper replaces the paper sign-out sheet: staff publish available gear, students check items out, and the app tracks due dates, overdue loans, and late fees.

The domain core, the hand-written JSON persistence layer, the Windows 11 application shell with mock login, the catalog with instant checkout, the loan/penalty manager views, the inventory screens with photo uploads, and demo reset are implemented, building, and covered by an in-repository probe suite. M6 is closed: thin scrollbars, Fluent motion fades, minimum-size fixes, icon/DPI/skeleton audits, and the interactive SRS §6.2 walkthrough that found and fixed fifteen UI defects. The presentation deck is finished: five slides at [`assets/kitnapper-presentation.pdf`](assets/kitnapper-presentation.pdf), rebuilt from the sources in [`deck/`](deck/build.sh) and written to serve the class talk rather than the milestone record (D56). Every deliverable is now in the repository.

## Contents

- [Requirements](#requirements)
- [Repository structure](#repository-structure)
- [Getting started](#getting-started)
- [Documentation](#documentation)
- [Project conventions](#project-conventions)
- [Status](#status)

## Requirements

- JDK 17 (developed against OpenJDK 17.0.20)
- NetBeans IDE with Ant project support

Both are needed only to **build**. The packaged Windows app-image below runs on
a machine with no JDK, no NetBeans and no PATH setup.

## Repository structure

```
Finals/                       (the repository root)
├── README.md
├── .editorconfig             shared formatting
├── .gitattributes            LF everywhere; binary assets not normalized
├── .gitignore
├── assets/
│   ├── banner.jpg            README banner image
│   ├── kitnapper-presentation.pdf  the slide deck (WP6)
│   └── deck/                 the captures the deck embeds
├── deck/                     deck generator: base/components/panels/slides.css
│   │                         + 11 slide files, art/ and fonts/ (OFL) +
│   │                         build.sh (weasyprint → assets/…pdf)
├── docs/
│   ├── PRD.md                product requirements and domain model
│   ├── SRS.md                verifiable requirements and demo walkthrough
│   ├── design.md             Windows 11 / Fluent visual specification
│   ├── decisions.md          decision log
│   ├── implementation-plan.md  class inventory, JSON schema, milestones M0–M6
│   ├── preferences.md        tracked working preferences
│   ├── seed-data.md          seed data scope note
│   └── handoffs/             working docs for finished milestones
│       ├── HANDOFF-M1.md
│       ├── HANDOFF-M2.md
│       ├── HANDOFF-M3.md
│       ├── HANDOFF-M3-PLAN.md        M3 record: plan and execution log
│       ├── HANDOFF-M4.md             M4 scope note (historical at close-out)
│       ├── HANDOFF-M4-PLAN.md        M4 record: plan and execution log
│       ├── HANDOFF-M5.md             M5 scope note (historical at close-out)
│       ├── HANDOFF-M5-PLAN.md        M5 record: plan and execution log
│       ├── HANDOFF-M6-PLAN.md        M6 record: plan and execution log (WP0–WP5)
│       └── HANDOFF-SESSION-2026-11-14.md
└── desktop-app/              NetBeans Ant project
    ├── build.sh              JDK-only clean/build/run helper (no Ant needed)
    ├── build.bat             the same compile step for Windows (JDK only)
    ├── run.bat               Windows launcher: build, then kitnapper.Main
    ├── reset-demo.bat        Windows demo prep: delete save + photos, re-seed
    ├── package.bat           Windows: portable app-image (exe + private runtime)
    ├── build.xml
    ├── manifest.mf
    ├── nbproject/
    │   ├── project.xml
    │   ├── project.properties
    │   ├── build-impl.xml
    │   └── genfiles.properties
    ├── src/kitnapper/
    │   ├── Main.java         entry point — installs theme/fonts, shows LoginFrame
    │   ├── model/            domain core — 18 classes (M0)
    │   ├── service/          services, AppContext, ID/seed helpers (M1–M5)
    │   │   └── persistence/  hand-written JSON parser, writer, mappers (M1)
    │   ├── controller/       AppController, ScreenMounts, and the per-screen actions (14 classes)
    │   └── view/             theme, chrome, shell, screens, previews, dialogs, motion (89 classes)
    ├── test/                 probe harness: 20 probes + run-all.sh (xvfb-aware)
    └── resources/
        └── fonts/              bundled Inter (Regular, SemiBold, Bold)
```

`desktop-app/` is a separate project root because NetBeans identifies an Ant project by the presence of `nbproject/` next to `build.xml`. Keeping the Java project in its own directory allows the repository root to hold documentation and shared configuration without confusing the IDE.

This repository is checked out under `OOP/Finals/` in the course tree. Some handoff docs were written on machines where the repository root was a bare `kitnapper/` folder (or a NetBeans project at `OOP/Finals/`), so they carry machine-specific path prefixes; treat those as history and use paths relative to this repository root.

## Getting started

### NetBeans

1. Open NetBeans and choose **File > Open Project**.
2. Select `desktop-app/`, the directory containing `build.xml`.
3. Press **F6** to run or **F11** to build.

The main class is set to `kitnapper.Main` in `nbproject/project.properties`. It installs the theme and bundled fonts, builds `AppContext`, and shows `LoginFrame` on the EDT.

### Command line

Apache Ant is not installed on every machine, so use whichever of these is available:

```bash
# Ant on the PATH
cd desktop-app
ant clean jar
ant run

# Ant bundled with NetBeans (adjust the path to your install)
cd desktop-app
../../netbeans/extide/ant/bin/ant clean jar

# No Ant at all — the helper script (JDK only)
cd desktop-app
./build.sh build      # clean | build | run
./build.sh run

# ...or plain javac by hand
cd desktop-app
mkdir -p build/classes
find src -name '*.java' > /tmp/kn-sources.txt
javac -encoding UTF-8 -d build/classes @/tmp/kn-sources.txt
java -cp build/classes kitnapper.Main
```

### Windows (no Ant needed)

`build.bat` and `run.bat` mirror `build.sh` for a JDK-only Windows machine; both
start with `cd /d "%~dp0"`, so a double-click always uses the right working
directory and the save file stays in `desktop-app\`.

```bat
cd desktop-app
build.bat            :: compile every source file into build\classes
run.bat              :: build, then launch the login screen
reset-demo.bat       :: delete the save + photos so the next launch re-seeds
```

Run `reset-demo.bat` with the app closed, on the day of a demo: the seed dataset
is computed relative to the date it is written, so a stale `kitnapper-data.json`
shows more overdue loans (and higher live fees) than the SRS §6.1 baseline.

### Windows portable app (no JDK on the demo machine)

`package.bat` turns the build into a self-contained folder — a private Java
runtime, the app jar, and a `KitNapper.exe` launcher (D55). `jpackage` ships
with the JDK, so this adds no dependency; it only packages for the host OS, so
run it on Windows with a full JDK 17 or newer:

```bat
cd desktop-app
package.bat          :: -> build\dist\KitNapper\KitNapper.exe (~77 MB, ~26 MB zipped)
```

Copy that whole `KitNapper` folder to the demo machine and double-click
`KitNapper.exe`; there is no console window and no installer. The jar inside it
carries the classes plus `resources\` (Inter and the bundled seed photos), and
the runtime is trimmed to `java.desktop`.

**Start it from the exe.** `kitnapper-data.json` and `kitnapper-images\` are
created in the *working directory*, exactly as for `run.bat`; Explorer sets that
to the folder holding the exe on a double-click, so the save file lands beside
`KitNapper.exe`. A shortcut keeps its own "Start in" folder, so leave that at
the app folder. Never extract under `Program Files`: a standard user cannot
write there, and the app needs to save.

The `KitNapper` folder is not signed, so a copy that arrives as a *download* may
trigger SmartScreen ("More info" → "Run anyway"). From a USB stick it usually
does not. Treat `build\dist\` as build output: it is git-ignored.

Build output is written to `build/` and `dist/`, both of which are ignored by Git. The runtime save file (`kitnapper-data.json`) and photo folder (`kitnapper-images/`) are created in the working directory and are ignored too.

## Documentation

| Document | Description |
| :--- | :--- |
| [`docs/PRD.md`](docs/PRD.md) | Product scope, domain model, state transitions, seed data |
| [`docs/SRS.md`](docs/SRS.md) | Numbered requirements, invariants, acceptance criteria, demo script |
| [`docs/design.md`](docs/design.md) | Windows 11 / Fluent visual spec (v2.0): tokens, custom chrome, components, photo uploads, skeleton loading states (§13) |
| [`docs/decisions.md`](docs/decisions.md) | Record of confirmed decisions and known gaps |
| [`docs/implementation-plan.md`](docs/implementation-plan.md) | Build order, class/file inventory, JSON persistence spec, decisions D1–D49 |
| [`docs/seed-data.md`](docs/seed-data.md) | Where seed data lives and when to split it out |
| [`docs/preferences.md`](docs/preferences.md) | Tracked, machine-independent working preferences: planning, one-phase execution, document-commit-push, verification |
| [`docs/pdf-decks.md`](docs/pdf-decks.md) | Portable playbook for the deck pipeline: HTML/CSS → WeasyPrint, font instancing and subsetting, the layout bugs it hit, and the verification loop |
| [`docs/handoffs/HANDOFF-M4.md`](docs/handoffs/HANDOFF-M4.md) | M4 scope note: current state, reuse map, gotchas, probe commands (historical at M4 close-out; the contract was `HANDOFF-M4-PLAN.md`) |
| [`docs/handoffs/HANDOFF-M4-PLAN.md`](docs/handoffs/HANDOFF-M4-PLAN.md) | M4 record: plan, work packages, probe plan, execution log, width pass |
| [`docs/handoffs/HANDOFF-M5.md`](docs/handoffs/HANDOFF-M5.md) | M5 scope note: inventory, photos, reset (historical at M5 close-out; the contract was `HANDOFF-M5-PLAN.md`) |
| [`docs/handoffs/HANDOFF-M5-PLAN.md`](docs/handoffs/HANDOFF-M5-PLAN.md) | M5 record: plan, work packages, probe plan, execution log, deviations |
| [`docs/handoffs/HANDOFF-M6-PLAN.md`](docs/handoffs/HANDOFF-M6-PLAN.md) | **M6 record:** plan, work packages (WP0–WP6 shipped), probe plan, execution log |
| [`assets/kitnapper-presentation.pdf`](assets/kitnapper-presentation.pdf) | The presentation deck (five slides, 16:9, rebuilt for the class talk in D56); rebuilt by `deck/build.sh` |
| [`docs/handoffs/HANDOFF-M3-PLAN.md`](docs/handoffs/HANDOFF-M3-PLAN.md) | M3 record: plan, work packages, probe plan, execution log |
| [`docs/handoffs/HANDOFF-M3.md`](docs/handoffs/HANDOFF-M3.md), [`docs/handoffs/HANDOFF-M2.md`](docs/handoffs/HANDOFF-M2.md), [`docs/handoffs/HANDOFF-M1.md`](docs/handoffs/HANDOFF-M1.md), [`docs/handoffs/HANDOFF-SESSION-2026-11-14.md`](docs/handoffs/HANDOFF-SESSION-2026-11-14.md) | Historical records of finished milestones (machine-specific paths and probe commands are stale) |

The PRD and SRS are the sources of truth for behavior. If implementation and documentation disagree, the documentation takes precedence until both are updated together.

## Project conventions

- Java 17 and Swing. No JavaFX.
- NetBeans Ant build. No Maven or Gradle.
- No third-party libraries; JSON persistence and the Swing theme are written against the JDK only.
- MVC layering: `model`, `service`, `controller`, `view`.
- Source files stay under 200 lines, with 100 to 150 as the target.
- Currency is the Philippine peso, formatted as `₱1,234.00`.
- Dates use `java.time.LocalDate`.

## Status

Documentation complete; M0 (domain core), M1 (JSON persistence), M2 (Win11 shell + mock login), **M3 (catalog + instant checkout)**, **M4 (returns, fees, manager views)**, **M5 (inventory, photos, reset, settings)** and **M6 (polish, closed 2026-11-17)** are implemented and probed on JDK 17. The SRS §6.2 walkthrough and the deck restyle (WP7) are recorded in `docs/handoffs/HANDOFF-M6-PLAN.md` §16; every deliverable is in the repository.

M3 ships the real Catalog screen: search plus category/status filters, a details table with a 280 px preview pane, a cards view with shared selection, an empty state, and a 480 px checkout dialog whose due date is a `T … T+14` picker. Checkout persists, locks the item, and keeps validation errors inline. Managers get the same catalog read-only — no checkout action, an explicit caption instead.

M5 ships the manager's inventory: an items table with the omnibar text filter and a photo preview, an add/edit dialog whose extra field follows the item's concrete kind, and per-item photo import that validates, downscales to 1024 px, re-encodes JPEG q0.85, and stores `kitnapper-images/<equipmentId>.jpg` atomically. Photos render through a shared tile with the app's first loading skeletons (150 ms delay, 300 ms minimum, 167 ms fade, 10 s watchdog), falling back to the category tile when a file is missing or corrupt. Maintenance and decommission are refused while the item is still out, and Settings carries the theme switch, the "Animate loading placeholders" switch, the save-file and photo-folder locations, and a confirmed demo reset that empties the photo folder and restores the seed relative to today.

M6 is polish, not features: thin overlay-style scrollbars on every scroll surface, the dialog and screen fades behind a `-Dkitnapper.motion=off` probe switch (the theme switch stays instant), one width policy that makes all four tables scroll at their preferred widths instead of clipping badges, compact preview density plus a metadata scroll guard so the 1024×640 minimum keeps every row and the pinned action, preview-aware toasts, and the icon/DPI/skeleton audits. The audits closed D32 by measurement — the slowest of eight catalog/loan/dashboard queries is 0.46 ms against the 150 ms gating threshold, so no query gets a skeleton — and fixed a real rendering defect: fallback glyphs were being drawn with the physical Inter font, which lacks them, so on Linux the title-bar brand, window controls and every category tile rendered as missing-glyph boxes. `Fonts.symbol()` now resolves them through the logical `Dialog` font. The closing interactive pass added the last polish: two-line toasts, token-styled combo and date-picker focus, status columns sized from the widest badge, wrapped dialog copy and fee notes, a themed file chooser, search hidden on screens that do not filter, per-route status memory, and an immediate selection status (D44–D49).

Verification lives **inside** the repository: `desktop-app/test/run-all.sh` builds the app and runs twenty probes (`DataProbe`, `CatalogProbe`, `LoanProbe`, `InventoryProbe`, `SkinProbe`, `FontProbe`, `EdgeProbe`, `SizeProbe`, `FrameProbe`, `CloseProbe`, `CatalogUiProbe`, `PhotoUiProbe`, `InventoryEditorProbe`, `InventoryUiProbe`, `SettingsUiProbe`, `M5FlowProbe`, `LoansLayoutProbe`, `LoansUiProbe`, `GeometryProbe`, `MotionProbe`), using `xvfb-run -a` for the paint probes so they work with or without a display. `KITNAPPER_UI_SCALE=1.25` (or `1.5`) runs the same suite at a DPI proxy. It is the gate for every remaining work package, together with the SRS §6.2 walkthrough.

Delivered: WP6 — the presentation deck, restyled in WP7 to the maintainer's reference design with bundled Montserrat and Open Sauce One. The interactive SRS §6.2 pass was run on 1920×1080, 1366×768 and 1024×640 in both themes and by keyboard; its fifteen findings were fixed at the root (D44–D49) and the 20-probe suite is green. See `docs/decisions.md` for the decision record, `docs/implementation-plan.md` for the build order, and `docs/handoffs/HANDOFF-M6-PLAN.md` for the M6 record.
