# KitNapper

<p align="center">
  <img src="assets/banner.jpg" alt="KitNapper banner" width="100%" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-informational?style=flat-square" alt="Java 17" />
  <img src="https://img.shields.io/badge/build-Ant%20%2F%20NetBeans-informational?style=flat-square" alt="Build" />
  <img src="https://img.shields.io/badge/dependencies-none-informational?style=flat-square" alt="Dependencies" />
  <img src="https://img.shields.io/badge/status-in%20development-yellow?style=flat-square" alt="Status" />
</p>

A desktop application for managing equipment loans at a small institution. KitNapper replaces the paper sign-out sheet: staff publish available gear, students check items out, and the app tracks due dates, overdue loans, and late fees.

The domain core, the hand-written JSON persistence layer, the Windows 11 application shell with mock login, and the catalog with instant checkout are implemented, building, and covered by an in-repository probe suite. Returns, late fees, penalty settlement, and the manager views (My Loans, All Loans, Overdue, Penalties, Dashboard) ship in M4. Inventory, photo uploads, and polish remain (M5–M6).

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

## Repository structure

```
Finals/                       (the repository root)
├── README.md
├── HANDOFF-M1.md             working docs for finished milestones
├── HANDOFF-M2.md
├── HANDOFF-M3.md
├── HANDOFF-M3-PLAN.md        M3 record: plan and execution log
├── HANDOFF-M4.md             M4 scope note (historical at close-out)
├── HANDOFF-M4-PLAN.md        M4 record: plan and execution log
├── HANDOFF-M5.md             active M5 scope note: inventory, photos, reset
├── HANDOFF-SESSION-2026-11-14.md
├── .editorconfig             shared formatting
├── .gitattributes            LF everywhere; binary assets not normalized
├── .gitignore
├── assets/
│   └── banner.jpg            README banner image
├── docs/
│   ├── PRD.md                product requirements and domain model
│   ├── SRS.md                verifiable requirements and demo walkthrough
│   ├── design.md             Windows 11 / Fluent visual specification
│   ├── decisions.md          decision log
│   ├── implementation-plan.md  class inventory, JSON schema, milestones M0–M6
│   ├── preferences.md        tracked working preferences
│   └── seed-data.md          seed data scope note
└── desktop-app/              NetBeans Ant project
    ├── build.sh              JDK-only clean/build/run helper (no Ant needed)
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
    │   ├── service/          services, AppContext, ID/seed helpers (M1–M3)
    │   │   └── persistence/  hand-written JSON parser, writer, mappers (M1)
    │   ├── controller/       AppController, CatalogController, BorrowingController
    │   └── view/             theme, chrome, shell, login, catalog, preview, dialogs
    ├── test/                 probe harness: 11 probes + run-all.sh (xvfb-aware)
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

Build output is written to `build/` and `dist/`, both of which are ignored by Git. The runtime save file (`kitnapper-data.json`) and photo folder (`kitnapper-images/`) are created in the working directory and are ignored too.

## Documentation

| Document | Description |
| :--- | :--- |
| [`docs/PRD.md`](docs/PRD.md) | Product scope, domain model, state transitions, seed data |
| [`docs/SRS.md`](docs/SRS.md) | Numbered requirements, invariants, acceptance criteria, demo script |
| [`docs/design.md`](docs/design.md) | Windows 11 / Fluent visual spec (v2.0): tokens, custom chrome, components, photo uploads, skeleton loading states (§13) |
| [`docs/decisions.md`](docs/decisions.md) | Record of confirmed decisions and known gaps |
| [`docs/implementation-plan.md`](docs/implementation-plan.md) | Build order, class/file inventory, JSON persistence spec, decisions D1–D31 |
| [`docs/seed-data.md`](docs/seed-data.md) | Where seed data lives and when to split it out |
| [`docs/preferences.md`](docs/preferences.md) | Tracked, machine-independent working preferences: planning, one-phase execution, document-commit-push, verification |
| [`HANDOFF-M4.md`](HANDOFF-M4.md) | M4 scope note: current state, reuse map, gotchas, probe commands (historical at M4 close-out; the contract was `HANDOFF-M4-PLAN.md`) |
| [`HANDOFF-M4-PLAN.md`](HANDOFF-M4-PLAN.md) | M4 record: plan, work packages, probe plan, execution log, width pass |
| [`HANDOFF-M5.md`](HANDOFF-M5.md) | **Active working document:** M5 scope note — where things stand, M5 scope, reuse map, gotchas, commands; the detailed `HANDOFF-M5-PLAN.md` is written before M5 code |
| [`HANDOFF-M3-PLAN.md`](HANDOFF-M3-PLAN.md) | M3 record: plan, work packages, probe plan, execution log |
| [`HANDOFF-M3.md`](HANDOFF-M3.md), [`HANDOFF-M2.md`](HANDOFF-M2.md), [`HANDOFF-M1.md`](HANDOFF-M1.md), [`HANDOFF-SESSION-2026-11-14.md`](HANDOFF-SESSION-2026-11-14.md) | Historical records of finished milestones (machine-specific paths and probe commands are stale) |

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

Documentation complete; M0 (domain core), M1 (JSON persistence), M2 (Win11 shell + mock login), **M3 (catalog + instant checkout)** and **M4 (returns, fees, manager views)** are implemented, probed, and building on JDK 17.

M3 ships the real Catalog screen: search plus category/status filters, a details table with a 280 px preview pane, a cards view with shared selection, an empty state, and a 480 px checkout dialog whose due date is a `T … T+14` picker. Checkout persists, locks the item, and keeps validation errors inline. Managers get the same catalog read-only — no checkout action, an explicit caption instead. The successful 2026-11-15 debt pass cleared every 200-line violation, enabled `-Xlint:all`, and added `desktop-app/build.sh` plus `.gitattributes`.

Verification lives **inside** the repository: `desktop-app/test/run-all.sh` builds the app and runs twelve probes (`DataProbe`, `CatalogProbe`, `LoanProbe`, `SkinProbe`, `FontProbe`, `EdgeProbe`, `SizeProbe`, `FrameProbe`, `CloseProbe`, `CatalogUiProbe`, `LoansLayoutProbe`, `LoansUiProbe`), using `xvfb-run -a` for the paint probes so they work with or without a display. It is the gate for every remaining work package, together with the SRS §6.2 walkthrough.

Next: M5 — inventory CRUD, per-item photo import, `kitnapper-images/` storage, photo fallback tiles, and the demo reset. M4 (returns, late fees, penalty settlement, My Loans / All Loans / Overdue / Penalties screens, manager Dashboard) is implemented, probed, and pushed. Then M6 (polish). See `docs/decisions.md` for the decision record, `docs/implementation-plan.md` for the build order, and `HANDOFF-M4-PLAN.md` for the M4 record.
