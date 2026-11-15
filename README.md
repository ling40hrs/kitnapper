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

The domain core, the hand-written JSON persistence layer, the Windows 11 application shell with mock login, and the catalog/loan service layer are implemented and building. The real catalog screen, the loan/penalty views, inventory and photo uploads, and the polish pass are still to come (M3 WP2–WP6, M4–M6).

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
├── HANDOFF-M3-PLAN.md        active M3 plan and execution log
├── HANDOFF-SESSION-2026-11-14.md
├── .editorconfig             shared formatting
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
│   ├── logout-tokens.md      credential runbook for the retired build machine
│   └── seed-data.md          seed data scope note
└── desktop-app/              NetBeans Ant project
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
    │   └── view/             theme, chrome, shell, login — catalog still a placeholder
    ├── test/
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

# No Ant at all — plain javac against the JDK
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
| [`docs/implementation-plan.md`](docs/implementation-plan.md) | Build order, class/file inventory, JSON persistence spec, decisions D1–D23 |
| [`docs/seed-data.md`](docs/seed-data.md) | Where seed data lives and when to split it out |
| [`docs/logout-tokens.md`](docs/logout-tokens.md) | Runbook: logging out of every stored GitHub credential on the build machine |
| [`docs/preferences.md`](docs/preferences.md) | Tracked, machine-independent working preferences: planning, one-phase execution, document-commit-push, verification |
| [`HANDOFF-M3-PLAN.md`](HANDOFF-M3-PLAN.md) | **Active working document:** M3 plan, work packages, probe plan, execution log |
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

Documentation complete; M0 domain core, M1 JSON persistence, and M2 Win11 shell + mock login are implemented, audited, and building. M3 WP1 (catalog search/filter, checkout and return services, sequential IDs, controllers) landed in `19a1e1b`; **M3 WP2–WP6 are still open**, so the Catalog screen is an M2 placeholder. Then come M4 (loans, returns, penalties), M5 (inventory, photos, reset) and M6 (polish). See `docs/decisions.md` for the decision record, `docs/implementation-plan.md` for the build order, and `HANDOFF-M3-PLAN.md` for the current work plan.

Automated verification lives outside the repository and is not part of this checkout; milestone verification is the SRS §6.2 walkthrough plus local probes.
