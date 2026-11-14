---
project_name: "KitNapper"
team_name: "YAPPERS"
course: "CS203 – Object-Oriented Programming"
artifact_type: "Implementation Plan (structure, persistence format, build order)"
derived_from: "PRD.md v3.1.0, SRS.md v3.1.0, design.md"
version: "1.3.0"
---

# KitNapper — Implementation Plan

> **Status:** Written 2026-11-14, before any application code exists. This document is the
> structure reference for implementation sessions: it maps PRD/SRS requirements and the
> design tokens onto concrete Java files, defines the hand-written JSON format, and fixes
> the build order. It adds no new features and changes no behavior — if it ever disagrees
> with `PRD.md`/`SRS.md`, those win and this file is updated.
>
> **No code exists yet.** Follow the milestones in §4 one at a time; do not implement
> everything at once. Each milestone must end with the project compiling (`ant clean jar`)
> and, from M2 on, launching.
>
> **design.md is now v2.0 (Windows 11 / Fluent), §13 resolved.** The M2–M6 UI work
> implements its §3–§13.

## 1. Ground Rules (restated, from AGENTS.md + docs)

- Java 17 + Swing only; NetBeans Ant project (`desktop-app/`); **no third-party libraries**.
- Strict MVC: `kitnapper.model`, `kitnapper.service`, `kitnapper.controller`, `kitnapper.view`.
  Persistence/JSON classes live in `kitnapper.service.persistence` (a sub-package of the
  service/data layer, so the four top-level MVC packages stay as-is).
- Source files ≤ 200 lines (target 100–150). LF line endings, 4-space indent.
- Entities mutate only through their own methods (invariants INV-1…6, PRD §6.2).
- No business logic in views; views depend only on **service interfaces** (PRD §7.3).
- No automated tests. Verification is manual: build, launch, and walk the SRS §6.2 steps.

## 2. Class & File Inventory

Line estimates are sanity checks against the 200-line cap, not budgets to hit.

### 2.1 `kitnapper.model` (pure domain — no JSON, no Swing)

| File | Contents | Est. lines |
| :--- | :--- | --: |
| `User.java` | abstract base: `userId`, `fullName`, `email`, `password`; abstract `getRole()` | 60 |
| `Borrower.java` | extends `User`; `getRole()` → `BORROWER` | 40 |
| `Manager.java` | extends `User`; `getRole()` → `MANAGER` | 40 |
| `Equipment.java` | **abstract**; attributes per PRD §3.1 incl. the optional `photoFile` (§12 photo uploads); status transition methods (`markAvailable`, `markLoaned`, `markUnderMaintenance`, `decommission`) each validating the legal transitions of PRD §4.1 (INV-1 support) | 150 |
| `DigitalEquipment.java` | extends `Equipment`; thin subclass adding `warrantyContact` (shown in inventory detail) | 60 |
| `MechanicalTool.java` | extends `Equipment`; thin subclass adding `maintenanceIntervalDays` | 60 |
| `GeneralEquipment.java` | extends `Equipment`; concrete subclass with no extra fields — the target of the JSON `kind: "BASE"` discriminator (decision D14) | 30 |
| `LoanRecord.java` | attributes per PRD §3.1; static factory `checkout(...)` validating INV-2; `completeReturn(returnDate)`; `accruedFee(asOf, FeeStrategy)`; `isOverdue(asOf)`; `effectiveStatus(asOf)` (see §5 D1) | 150 |
| `Penalty.java` | attributes per PRD §3.1; `markPaid()`, `waive(reason)`; INV-4 non-negative amount | 90 |
| `FeeStrategy.java` | interface: `calculate(LoanRecord loan, LocalDate asOf): double` | 15 |
| `LateFeeStrategy.java` | ₱50/day, rate defined once as the constant referenced by REQ-007 | 30 |
| `UserRole.java` | enum `BORROWER`, `MANAGER` | 10 |
| `EquipmentCategory.java` | enum `CAMERA`, `LENS`, `LAPTOP`, `AUDIO`, `TOOL`, `OTHER` | 10 |
| `EquipmentStatus.java` | enum `AVAILABLE`, `LOANED`, `UNDER_MAINTENANCE`, `DECOMMISSIONED` | 10 |
| `LoanStatus.java` | enum `ACTIVE`, `OVERDUE`, `RETURNED` — `OVERDUE` exists for `effectiveStatus(asOf)` but is never persisted (§5 D1) | 10 |
| `PenaltyType.java` | enum `LATE_FEE` | 10 |
| `PenaltyStatus.java` | enum `UNPAID`, `PAID`, `WAIVED` | 10 |
| `PenaltyOutcome.java` | enum `PAID`, `WAIVED` (settlement action parameter) | 10 |

### 2.2 `kitnapper.service` (interfaces + implementations)

| File | Contents | Est. lines |
| :--- | :--- | --: |
| `AuthService.java` | interface: `login(email, password): User`, seeded-credential check (REQ-003) | 20 |
| `CatalogService.java` | interface: `search(query, category, status): List<Equipment>` (REQ-001) | 25 |
| `LoanService.java` | interface: `checkout(equipmentId, dueDate)` — borrow date is `LocalDate.now()` (D4) and the borrower comes from the session —, `returnLoan(loanId, returnDate)` (REQ-004/006), `myLoans()`, `allLoans()`, `overdueLoans()`, plus `accruedFee(loanId)` so views never compute fees themselves (D21) | 45 |
| `PenaltyService.java` | interface: `penaltiesForLoan(loanId)`, `allPenalties()`, `settle(penaltyId, outcome, reason)` (REQ-011) | 25 |
| `InventoryService.java` | interface: `add/update/decommission/flagMaintenance`; add/update act on the concrete `Equipment` the controller constructed (D19) | 25 |
| `DataStore.java` | interface: `load()`, `save()`, `resetToSeed()` plus read accessors over the live aggregate (REQ-012, D17) | 30 |
| `AppData.java` | the in-memory aggregate: four live lists (`users`, `equipment`, `loans`, `penalties`) that services mutate and `save()` persists (D17) | 50 |
| `ImageStore.java` | interface: `importPhoto(equipmentId, sourcePath)`, `photoFor(equipmentId)`, `deletePhoto(equipmentId)`; reset/orphan-sweep hooks (design §12) | 30 |
| `AuthServiceImp.java` | impl; authenticates against the users **loaded from `DataStore`**, not a hard-coded list (D23) | 60 |
| `CatalogServiceImpl.java` | impl | 80 |
| `LoanServiceImpl.java` | impl; enforces INV-3, INV-6 (3-loan cap) and the 14-day window via `LoanRecord.checkout`; on return, computes the fee for the **chosen return date**, skips a zero fee, freezes exactly one `Penalty` (FR-07, D20), and only calls `markAvailable()` when the item is `LOANED` (D15) | 150 |
| `PenaltyServiceImpl.java` | impl | 80 |
| `InventoryServiceImpl.java` | impl; goes through `Equipment` transition methods only (INV-5) | 100 |
| `DataStoreImpl.java` | impl; orchestrates load → seed fallback → save-on-mutation; fallback covers model-validation and referential failures too (D18); atomic move with a Windows replace-existing fallback | 80 |
| `ImageStoreImpl.java` | impl: validate extension (png/jpg/jpeg/gif/bmp) and ≤ 20 MB, decode via `ImageIO`, downscale to 1024 px longest edge, re-encode JPEG q0.85, atomic tmp+move write into `kitnapper-images/` (design §12) | 140 |
| `AppContext.java` | holds the single set of service instances (incl. `ImageStore`), the shared `LateFeeStrategy`, the `DataStore`, the current session `User`, and the theme toggle; simple `addChangeListener(Runnable)` for views (NFR-02, §5 D2) | 60 |

### 2.3 `kitnapper.service.persistence` (hand-written JSON — no libraries)

| File | Contents | Est. lines |
| :--- | :--- | --: |
| `JsonParser.java` | recursive-descent parser for **only the subset `JsonWriter` emits** (§3.2) | 120 |
| `JsonWriter.java` | writer: pretty-printed, 2-space indent; strings escaped (`\"`, `\\`, `\n`); numbers as plain decimals | 80 |
| `Json.java` | tiny helpers to build/read the JDK-native representation (`Map<String,Object>`, `List<Object>`, `String`, `Double`, `Boolean`, `null`) | 60 |
| `UserMapper.java` | `User` ⇄ JSON (role field selects `Borrower`/`Manager` on read) | 70 |
| `EquipmentMapper.java` | `Equipment` ⇄ JSON for all three concrete kinds (DIGITAL/MECHANICAL/BASE); writes `kind` via `instanceof`, restores status by replaying transition methods; optional `photoFile` (null-safe, D8); `Optional` getters via `orElse(null)` | 90 |
| `LoanMapper.java` | `LoanRecord` ⇄ JSON; `returnDate` as `null` when absent; normalizes a persisted `OVERDUE` to `ACTIVE` (never re-serializes it, D18) | 70 |
| `PenaltyMapper.java` | `Penalty` ⇄ JSON; `waiveReason` as `null` when not waived | 60 |
| `RootMapper.java` | top-level document ⇄ `AppData` (D17): the four arrays + `schemaVersion` + `savedAt` | 80 |
| `SeedData.java` | the PRD §9 dataset, computed **relative to the date seeding runs** (§5 D4); seed items carry no photos (D11); kinds default by category (D19) | 130 |

### 2.4 `kitnapper.controller` (thin wiring only)

| File | Contents | Est. lines |
| :--- | :--- | --: |
| `AppController.java` | session user, screen routing (sidebar → content swap), theme toggle, logout; owns `AppContext` | 120 |
| `CatalogController.java` | search/filter state, checkout launch | 70 |
| `BorrowingController.java` | checkout + return actions through `LoanService` | 80 |
| `InventoryController.java` | add/edit/decommission/maintenance actions | 70 |
| `PenaltyController.java` | settle actions (INV-3: manager-only, enforced here + service) | 50 |

### 2.5 `kitnapper.view` (Swing; no business logic)

Shared infrastructure:

| File | Contents | Est. lines |
| :--- | :--- | --: |
| `Theme.java` | all color tokens from design.md §3 (dark + light Win11 ramps, badge dots, tile gradient pairs), `apply()` to `UIManager`, dark/light switch | 170 |
| `Fonts.java` | probe `Segoe UI Variable Display/Text → Segoe UI` (design §4), register bundled Inter as fallback, probe Segoe Fluent Icons / MDL2 for glyphs (§10) | 80 |
| `UiFactory.java` | buttons (primary/secondary/subtle/destructive), inputs with the 2 px amber focus bottom, Win11 focus rectangle per design §6 | 130 |
| `Badges.java` | badge pill renderer: `ACCENT_TINT` fill + semantic dot + word, wrench glyph on `UNDER_MAINTENANCE` (design §3.3; status never color-alone) | 90 |
| `Formats.java` | money `₱1,234.00`, dates `yyyy-MM-dd` — the single place both are defined (design §8) | 40 |
| `Toasts.java` | bottom-right auto-dismiss toast overlay | 80 |
| `Dialogs.java` | modal dialog shell + themed destructive-confirmation helper (design §7) | 90 |
| `EmptyState.java` | one-line explanation + fixing action (design §6) | 50 |
| `WindowChrome.java` | undecorated-frame behavior: drag, double-click maximize, 8 px edge resize bands, min size, restore bounds (design §11) | 180 |
| `TitleBar.java` | 32 px title bar: wordmark, screen title, theme toggle, min/max/close painting + hit targets (design §5) | 140 |
| `Omnibar.java` | breadcrumb + contextual command row + search field + layout toggle (design §5) | 160 |
| `StatusBar.java` | counts, selection summary, monospace aggregate (design §5) | 70 |
| `PreviewPane.java` | right detail pane: 16:9 media, metadata rows, pinned primary action (design §6) | 150 |
| `PhotoTile.java` | off-EDT image decode + cache; skeleton placeholder; falls back to `GradientTile` (design §12) | 120 |
| `GradientTile.java` | category fallback tile — the single permitted gradient (design §3.4/§3.5) | 80 |
| `SegmentedControl.java` | segmented control + toggle switch per design §6 | 90 |
| `ThinScrollBarUI.java` | thin 4 px-thumb scrollbar UI (design §6) | 80 |
| `SkeletonPanel.java` | skeleton loading blocks — visual only, timing in `SkeletonGate` (design §13) | 110 |
| `SkeletonGate.java` | skeleton timing/plumbing: off-EDT work, 150/300/167 ms, 10 s watchdog, accessible loading state (design §13) | 120 |

Window & screens (design §5 inventory):

| File | Contents | Est. lines |
| :--- | :--- | --: |
| `LoginFrame.java` | email, password, sign-in, inline error area | 120 |
| `MainFrame.java` | hosts `WindowChrome`/`TitleBar`/`Omnibar`/`StatusBar`, sidebar + content `CardLayout` swap | 150 |
| `DashboardScreen.java` | manager KPIs (out now / overdue / unpaid penalties) + overdue table + recent activity | 150 |
| `CatalogScreen.java` | omnibar commands (search, filters, layout toggle); **details table + `PreviewPane` by default**, cards view via the toggle; checkout action | 170 |
| `EquipmentCard.java` | catalog card: `PhotoTile`/`GradientTile` media block, badge, value (design §6) | 100 |
| `MyLoansScreen.java` | borrower active/overdue/returned tabs + accrued fees + penalty list | 140 |
| `AllLoansScreen.java` | sortable loans table + return action | 130 |
| `OverdueScreen.java` | overdue table, longest-overdue first, days-late column, live accrued fee | 110 |
| `PenaltiesScreen.java` | per-loan penalty list + paid/waive actions | 120 |
| `InventoryScreen.java` | items table + `PreviewPane` (photo, metadata) + add/edit/decommission/maintenance actions | 160 |
| `SettingsScreen.java` | reset demo data + save-file and photo-folder locations + theme | 90 |

Table models (one per table keeps every file small and satisfies NFR-02):

| File | Contents | Est. lines |
| :--- | :--- | --: |
| `CatalogTableModel.java` | + `InventoryTableModel.java` | 80 each |
| `LoansTableModel.java` | shared by All Loans / Overdue / My Loans (column sets configured per screen) | 120 |
| `PenaltiesTableModel.java` | | 80 |

Dialogs (modal per design §5):

| File | Contents | Est. lines |
| :--- | :--- | --: |
| `CheckoutDialog.java` | item summary + due-date picker + inline validation (FR-04) | 120 |
| `ReturnDialog.java` | return-date entry, shows the fee that will freeze (FR-06/07) | 100 |
| `SettlePenaltyDialog.java` | paid/waive + reason field (FR-11) | 90 |
| `EquipmentDialog.java` | add/edit item form, category-specific subclass fields, photo picker | 170 |

### 2.6 Entry point

| File | Contents | Est. lines |
| :--- | :--- | --: |
| `Main.java` | exists as a placeholder; becomes: install `Theme`, register fonts, construct services + `AppContext`, show `LoginFrame`. Stays under 60 lines | 60 |

Total ≈ 70 files (18 model + 52 service/controller/view). Every file above stays under
the 200-line cap (`WindowChrome` is the tightest — keep behavior and painting split
across it and `TitleBar`).

## 3. JSON Persistence Spec (`kitnapper-data.json`)

### 3.1 Document shape

Written to the **application folder** (PRD §7.2), pretty-printed, on every mutation:

```json
{
  "schemaVersion": 1,
  "savedAt": "2026-11-14",
  "users": [
    {
      "userId": "u-admin",
      "fullName": "A. Manager",
      "email": "admin@institution.edu",
      "password": "admin123",
      "role": "MANAGER"
    }
  ],
  "equipment": [
    {
      "id": "e-001",
      "name": "Sony A7 IV",
      "category": "CAMERA",
      "status": "LOANED",
      "serialNumber": "SN-A7IV-001",
      "replacementValue": 150000.0,
      "kind": "DIGITAL",
      "warrantyContact": null,
      "photoFile": null
    }
  ],
  "loans": [
    {
      "loanId": "L-001",
      "borrowerId": "u-student1",
      "equipmentId": "e-001",
      "borrowDate": "2026-11-12",
      "dueDate": "2026-11-19",
      "returnDate": null,
      "loanStatus": "ACTIVE"
    }
  ],
  "penalties": [
    {
      "penaltyId": "p-001",
      "loanId": "L-003",
      "type": "LATE_FEE",
      "amount": 200.0,
      "status": "WAIVED",
      "assessedDate": "2026-11-14",
      "waiveReason": "First-offense courtesy"
    }
  ]
}
```

Field rules:

- **Dates** are ISO `yyyy-MM-dd` strings (matches `Formats.java`).
- **`returnDate`** is `null` while the loan is outstanding; a real date string once returned.
- **`loanStatus`** persists only `ACTIVE` and `RETURNED` — `OVERDUE` is derived at read time
  (§5 D1), so a loan overdue at save time is still stored as `ACTIVE`.
- **`kind`** discriminates the `Equipment` subclasses: `DIGITAL` (`DigitalEquipment`,
  `warrantyContact`) / `MECHANICAL` (`MechanicalTool`, `maintenanceIntervalDays`) / `BASE`
  (`GeneralEquipment`, no extra fields) — all three load back to the right concrete class
  (D14). **Field presence follows the kind:** `warrantyContact` is written only for
  `DIGITAL`, `maintenanceIntervalDays` only for `MECHANICAL`; mappers write neither for
  `BASE`. The parser yields all numbers as `double`, so the mapper validates that
  `maintenanceIntervalDays` is integral before converting (otherwise seed fallback).
- **Unknown `kind` or `role` values** are treated as corruption → seed fallback (D18).
- **`waiveReason`** is `null` unless the penalty was waived.
- **`photoFile`** is `null` when the item has no photo; otherwise the relative filename
  under `kitnapper-images/` (design.md §12). Additive and nullable, so `schemaVersion`
  stays 1 and older save files load unchanged (D8).
- **`schemaVersion`** starts at 1. If a future change alters the shape, `load()` detects an
  unexpected version and falls back to the seed dataset (NFR-07) rather than guessing a
  migration — resets are acceptable for this course project.

### 3.2 Parser scope (deliberately narrow)

`JsonParser` accepts **only what `JsonWriter` emits**, which keeps it ~120 lines with no
third-party code:

- Values: objects, arrays, strings, numbers (plain decimal, parsed as `double`), `true`,
  `false`, `null`. No comments, no exponent notation, no single quotes.
- Strings: the escape set the writer produces (`\"`, `\\`, `\n`). Other `\x` escapes are a
  parse error (which triggers the seed fallback — never a crash).
- Structure: UTF-8, whitespace between tokens tolerated, trailing commas rejected.

### 3.3 Write & error handling

1. Every mutation goes through a service; the service calls `DataStore.save()` last
   (single choke point, satisfies REQ-012's "saved on every mutation").
2. `save()` serializes to `kitnapper-data.json.tmp` then atomically moves it over
   `kitnapper-data.json` — a mid-write crash can never leave a truncated file. On Windows,
   `ATOMIC_MOVE` may be unsupported for a replace; try atomic first and fall back to
   `REPLACE_EXISTING` (same for `ImageStore`).
3. On startup: file present → parse. Parse error, unknown `schemaVersion`, unknown
   enum/`kind`/`role` values, or missing file → seed fresh (PRD §9) and save immediately.
   **Model failures count as corruption too** (D18): a `LoanRecord` that violates INV-2,
   a `WAIVED` penalty without a reason, or referential breakage (a loan whose borrower or
   equipment does not exist, a penalty whose loan does not exist, or two penalties on one
   loan) must also fall back to seed, never crash (NFR-07). Mapping builds into local
   structures and swaps into `AppData` only when the whole document is valid.
4. `resetToSeed()` (manager action, FR-12) regenerates the seed dataset relative to *today*
   (§5 D4) and overwrites the file. Deleting the file manually does the same on next launch.
5. Photos follow the same write discipline (`ImageStore`): tmp file, then atomic move into
   `kitnapper-images/`. `resetToSeed()` also wipes that folder — the seed has no photos
   (D11) — and a load-time orphan sweep deletes images not referenced by the dataset.

## 4. Milestones (build order — one at a time)

Each milestone compiles and (from M2) launches. Verify with the listed SRS §6.2 steps
before moving on. **Do not batch milestones.**

| # | Scope | Files | Verify (SRS §6.2) |
| :-- | :--- | :--- | :--- |
| M0 | Domain core | all `model` files | `ant clean jar` compiles; nothing runnable yet |
| M1 | JSON persistence + seed | `service.persistence` files, `AppData`, `DataStore` + impl, `LoanService` skeleton | scratch entry point (a temporary `main` in a persistence package file, **deleted after M1**) writes the seed, re-reads it, and the round-trip matches — the fixture must also include a `BASE`-kind item, a `RETURNED` loan, a `WAIVED` penalty and an `UNDER_MAINTENANCE` item to exercise every restore path (D14, D18); corrupt-file and model-validation fallbacks checked by hand |
| M2 | Shell + login (Win11 chrome) | `Main`, `Theme`, `Fonts`, `UiFactory`, `AppContext`, `AuthService(+Impl)`, `WindowChrome`, `TitleBar`, `Omnibar`, `StatusBar`, `LoginFrame`, `MainFrame`, `AppController`, `Formats`, `Dialogs` | steps 1–3: login screen, wrong password rejected, student login lands on catalog shell; chrome drag/resize/maximize checked |
| M3 | Catalog + checkout | `CatalogScreen`, `EquipmentCard`, `PreviewPane`, `PhotoTile`, `GradientTile`, `SegmentedControl`, `Badges`, `CatalogTableModel`, `CatalogController`, `CatalogService(+Impl)`, `BorrowingController`, `CheckoutDialog`, `LoanService(+Impl)`, `Toasts`, `SkeletonPanel`, `SkeletonGate` | steps 3–6: search/filter, checkout, availability lock, 15-day rejection; model exceptions surface as inline messages, never EDT crashes (D16, D21); first skeleton use — card/preview decode + gated catalog refresh (design §13) |
| M4 | Returns, fees, manager views | `DashboardScreen`, `AllLoansScreen`, `OverdueScreen`, `MyLoansScreen`, `LoansTableModel`, `PenaltiesScreen`, `PenaltiesTableModel`, `ReturnDialog`, `SettlePenaltyDialog`, `PenaltyController`, `PenaltyService(+Impl)`, `Badges` | steps 7–10: persistence round-trip (restart), overdue dashboard ₱200, return freezes penalty, waive; extra checks: a due-date return creates **no** penalty and a maintenance-flagged item stays `UNDER_MAINTENANCE` (D15, D20); tables use `effectiveStatus(now)`, fees come from `LoanService` (D21) |
| M5 | Inventory, photos + reset | `InventoryScreen`, `InventoryTableModel`, `InventoryController`, `InventoryService(+Impl)`, `ImageStore(+Impl)`, `EquipmentDialog`, `SettingsScreen`, `EmptyState` | steps 11–12: add item **with photo** + decommission, reset restores the §6.1 baseline and wipes `kitnapper-images/`; extra checks: kind preserved on category edit (D19), photo filename safety (D22); photo import skeleton + "Animate loading placeholders" switch land here (design §13.4) |
| M6 | Polish | `ThinScrollBarUI`, dark/light toggle, Fluent-motion fades (design §7), icon-codepoint verification (§10), HiDPI/text-scale pass, **skeleton audit per design §13** | full 12-step walkthrough + design §8 contrast/keyboard checks + gradient spot-check (§3.4) at 1080p and 1366×768; skeleton audit: reduced-motion, watchdog, no permanent skeletons |

M0 → M1 may be committed together; M2 onward commit per milestone (git rules in
`AGENTS.md`: explicit approval before every git write).

## 5. Implementation Decisions (resolved 2026-11-14 with the team)

These close the last open technical questions; they implement the docs, they don't change
them. Logged here so implementation sessions don't re-litigate them.

| # | Question | Decision |
| :-- | :--- | :--- |
| D1 | How does the `OVERDUE` auto-transition work in code? | Persist only `ACTIVE`/`RETURNED`. `LoanRecord.effectiveStatus(asOf)` derives `OVERDUE` when active and `asOf > dueDate`. All views and the overdue dashboard use the derived status; no stored status ever needs rewriting. Matches PRD §4.2 ("auto-computed"). |
| D2 | How do views learn about state changes (NFR-02)? | Services and `AppContext` expose `addChangeListener(Runnable)`. Views register on construction and re-query their service on event — plain listener lists, no event-bus machinery. |
| D3 | Does the dark/light theme toggle persist? | No — session-only, same as the catalog layout toggle (design §14.1 Q6). `kitnapper-data.json` holds lending state only. |
| D4 | What clock does the app use? | The real system date (`LocalDate.now()`). Seed dates are computed relative to the date **seeding runs** (first launch or reset). Persisted dates are absolute, so restarting later than T just makes seeded loans genuinely older — which the derived-overdue rule (D1) handles correctly. |
| D5 | `Equipment` abstract or concrete? | **Abstract**, with thin subclasses `DigitalEquipment` and `MechanicalTool` (each adding one optional field). PRD §7.3 expects `Equipment` to be abstract, and the subclasses strengthen the inheritance showcase beyond `User`. The `kind` discriminator in §3.1 round-trips them. |
| D6 | Where do role checks (INV-3) live? | In the **services/controllers**, which hold the session `User` from `AppContext`. Entities stay role-agnostic; they enforce INV-1, INV-2, and INV-4 themselves. A borrower session simply has no code path to `settle()` or `returnLoan()`, and the service re-checks anyway. |
| D7 | Where do item photos live and how are they imported? | `kitnapper-images/<equipmentId>.jpg` beside `kitnapper-data.json`; import = validate → decode → downscale 1024 px → JPEG q0.85 → atomic tmp+move write; reset wipes the folder; orphan sweep on load (design §12). |
| D8 | Does the photo field break existing saves? | No — `photoFile` is nullable/additive; `schemaVersion` stays 1. |
| D9 | Window chrome fidelity | Custom undecorated title bar, square outer corners; no Mica blur, no snap layouts — accepted Swing limitations (design §11). |
| D10 | Badge language | Amber-tinted pill + semantic dot + word; wrench glyph on `UNDER_MAINTENANCE`; never color-alone (design §3.3). |
| D11 | Seed photos | Seed dataset ships without photos; missing/corrupt files always fall back to the category gradient tile. |
| D12 | Tabs? | None. Sidebar is the only navigation; the omnibar breadcrumb shows location (design §5). |
| D13 | Gradients | Exactly one permitted gradient: the photo-fallback category tile (design §3.4). Everything else is flat; gradient spot-check in the M6 review. |
| D14 | What does JSON `kind: "BASE"` instantiate, given that `Equipment` is abstract (D5)? | A third concrete subclass `GeneralEquipment` (no extra fields). `Equipment` stays abstract; the `kind` discriminator round-trips all three concrete classes. |
| D15 | What happens on the return of an item that was flagged `UNDER_MAINTENANCE` while `LOANED`? | The return closes the loan, but the item stays `UNDER_MAINTENANCE` until a manager completes the repair — the return workflow must not call `markAvailable()` in that case. Clarified in PRD/SRS 3.1.1. |
| D16 | What exception policy does the model layer use? | Null arguments → `NullPointerException` (`Objects.requireNonNull`); invalid values (blank, negative, 15+-day window) → `IllegalArgumentException`; illegal status transitions or double settlement → `IllegalStateException`. The UI never surfaces these directly; services translate them into user messages. |
| D17 | Where does the in-memory dataset live? | In an `AppData` aggregate (four live lists) held by `DataStore`. Services obtain the lists through `DataStore` accessors, mutate entities, then call `save()`. `RootMapper` maps the JSON document ⇄ `AppData`. |
| D18 | What counts as a corrupt save file? | Parse error, unknown `schemaVersion`, unknown enum/`kind`/`role` values, **or any model failure during mapping** (INV-2 violation, `WAIVED` penalty without reason, NPE) plus referential breakage (loan → missing borrower/equipment, penalty → missing loan, two penalties on one loan). All trigger the seed fallback; a persisted `OVERDUE` normalizes to `ACTIVE`. |
| D19 | How does `kind` relate to `category`? | The concrete kind is chosen **at creation** (default by category: CAMERA/LENS/LAPTOP/AUDIO → `DIGITAL`, TOOL → `MECHANICAL`, OTHER → `BASE`) and is preserved on edit; changing the category never changes the concrete class. The edit dialog shows only the field the existing kind carries. |
| D20 | What are the return edge cases? | A computed fee of ₱0 creates **no** `Penalty` (SRS FR-07 acceptance); a return date in the future is rejected in the service/UI (the model stays date-agnostic by design); per D15, `markAvailable()` is called only when the item is `LOANED`. |
| D21 | How do views get derived status and fees? | `LoanService` exposes them (`accruedFee`, overdue listing) computed with `effectiveStatus(LocalDate.now())`. Views never construct a `FeeStrategy` and never call entity mutators directly — services are the only callers of transition/settlement methods. |
| D22 | How are IDs and photo filenames kept safe? | Equipment and loan IDs are **system-generated** (sequential `e-0NN`/`L-0NN`), never user-typed; `ImageStore` builds `<equipmentId>.jpg`; `Equipment.setPhotoFile` accepts bare file names only. |
| D23 | Does `AuthService` hard-code credentials? | No — it authenticates against the user list **loaded from `DataStore`** (which the seed and reset populate), so an edited or corrupted save changes what logs in, consistently with the dataset. |

## 6. Verification Notes

- **No test framework** (no-libraries rule). Milestone verification is: build, launch, and
  perform the listed §6.2 steps by hand.
- The only sanctioned scratch code is M1's temporary `main` for the JSON round-trip check;
  it is deleted before M1 is committed.
- After M6, run the complete SRS §6.2 walkthrough on 1080p, then once at 1366×768
  (design §14.1 Q9), and the design §8 checks (contrast, keyboard-only, badge wording).

## Appendix: Revision History

| Version | Date | Changes |
| :--- | :--- | :--- |
| 1.0.0 | 2026-11-14 | Initial implementation plan: class inventory, JSON spec, milestones M0–M6, decisions D1–D6 — written before any application code |
| 1.1.0 | 2026-11-14 | Retargeted to design.md v2.0 (Windows 11 / Fluent): chrome/omnibar/status-bar/preview-pane view classes, `ImageStore` + `photoFile` through model and JSON, decisions D7–D13, skeleton plan flagged as blocking |
| 1.2.0 | 2026-11-14 | M0 decisions: `GeneralEquipment` for the JSON `BASE` kind (D14), maintenance-flag return rule (D15), model exception policy (D16); `LoanStatus` clarified to hold all three constants with only `ACTIVE`/`RETURNED` persisted |
| 1.3.0 | 2026-11-14 | Post-M0 findings folded in: `AppData` aggregate + `DataStore` accessors (D17), load-fallback scope covers model/referential failures (D18), kind-vs-category rule (D19), return edge cases — zero fee, future dates (D20), derived status/fee display via `LoanService` (D21), ID/photo-filename safety (D22), auth against persisted users (D23); M1 round-trip fixture exercises every restore path |
| 1.4.0 | 2026-11-14 | design.md §13 resolved: `SkeletonPanel` + `SkeletonGate` added to the view inventory, first use in M3 (card/preview decode + gated catalog refresh), photo import + reduced-motion switch in M5, skeleton audit in M6 |
