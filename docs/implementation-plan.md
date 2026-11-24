---
project_name: "KitNapper"
team_name: "YAPPERS"
course: "CS203 – Object-Oriented Programming"
artifact_type: "Implementation Plan (structure, persistence format, build order)"
derived_from: "PRD.md v3.1.1, SRS.md v3.1.5, design.md"
version: "1.9.0"
---

# KitNapper — Implementation Plan

> **Status:** Written 2026-11-14 as the structure reference, and still the map from
> PRD/SRS requirements and design tokens onto concrete Java files: it defines the
> hand-written JSON format and fixes the build order. It adds no new features and changes
> no behavior — if it ever disagrees with `PRD.md`/`SRS.md`, those win and this file is
> updated.
>
> **Implementation progress:** M0 (model), M1 (persistence + seed), M2 (shell + login),
> M3 (catalog + instant checkout, manager read-only) and **M4 (returns, late fees,
> penalty settlement, My Loans / All Loans / Overdue / Penalties screens, manager
> Dashboard)** are implemented, probed, and documented. Inventory and Settings are still
> placeholders (M5). Follow the milestones in §4 one at a time; do not implement
> everything at once. Each milestone must end with the project compiling (`ant clean jar`
> or the `javac` fallback) and launching, with `desktop-app/test/run-all.sh` green from M3 on.
>
> **Deviations from this inventory** are tracked in `docs/decisions.md` §9 and
> `HANDOFF-M3-PLAN.md` §6/§17 (the 200-line cap forced extra `view` files: `FlatButton`,
> `FieldSkin`, `RoundedPanel`, `ResizeBands`, `Sidebar`, `CatalogPresenter`,
> `CatalogFilters`, `CatalogCards`, `PageHeader`, `EmptyState`, `DatePicker`, and the
> M3 screen splits), so the per-package counts below are a floor, not a fixed contract.
>
> **design.md is v2.0 (Windows 11 / Fluent), §13 resolved.** The M2–M6 UI work
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
| `InventoryService.java` | interface: `peekNextId`, `add`, `update`, `setPhoto`, `removePhoto`, `flagMaintenance`, `clearMaintenance`, `decommission`, `resetDemoData`; throws like `LoanService` so the controller translates (D16); manager-only and loan-aware guards (D36) | 60 |
| `DataStore.java` | interface: `load()`, `save()`, `resetToSeed()`, `folder()` (the app folder holding the save file and photos) plus read accessors over the live aggregate (REQ-012, D17) | 35 |
| `AppData.java` | the in-memory aggregate: four live lists (`users`, `equipment`, `loans`, `penalties`) that services mutate and `save()` persists (D17) | 50 |
| `ImageStore.java` | interface: `stage`, `promote`, `load`, `delete`, `deleteAll`, `sweep`; imports stage before promotion (D35), reads never throw (design §12) | 55 |
| `ImageRules.java` | every photo limit in one place: folder, staging prefix, extensions, 20 MB, 1024 px, JPEG q0.85 (design §12) | 45 |
| `ImageMessages.java` | package-private photo rejection/cleanup messages | 25 |
| `PhotoCodec.java` | stepped bicubic downscale, alpha flatten onto the caller's matte (D34), JPEG q0.85 write, atomic move | 132 |
| `ItemDraft.java` | record of the add/edit fields; the concrete kind is not a field (D19) | 25 |
| `EquipmentFactory.java` | category → concrete kind at creation only (D19) | 40 |
| `AuthServiceImpl.java` | impl; authenticates against the users **loaded from `DataStore`**, not a hard-coded list (D23) | 60 |
| `CatalogServiceImpl.java` | impl | 80 |
| `LoanServiceImpl.java` | impl; enforces INV-3, INV-6 (3-loan cap) and the 14-day window via `LoanRecord.checkout`; on return, computes the fee for the **chosen return date**, skips a zero fee, freezes exactly one `Penalty` (FR-07, D20), and only calls `markAvailable()` when the item is `LOANED` (D15); delegates reads/lookups to `LoanQueries` | 150 |
| `LoanQueries.java` | package-private read side of `LoanService`: clock, entity/loan lookup, `myLoans`/`allLoans`/`overdueLoans` sorting, `accruedFee`; keeps `LoanServiceImpl` under the cap | 90 |
| `LoanMessages.java` | package-private user-facing strings for checkout/return failures and the due-date window | 35 |
| `Ids.java` | sequential system IDs from the highest numeric suffix: `L-NNN`, `p-NNN`, `e-NNN`; malformed suffixes ignored (D22) | 55 |
| `PenaltyServiceImpl.java` | impl | 80 |
| `InventoryServiceImpl.java` | impl; goes through `Equipment` transition methods only (INV-5), refuses clear/decommission while the item is out (D36) | 177 |
| `InventoryMessages.java` | package-private inventory messages, `LoanMessages` pattern | 25 |
| `DataStoreImpl.java` | impl; orchestrates load → seed fallback → save-on-mutation; fallback covers model-validation and referential failures too (D18); atomic move with a Windows replace-existing fallback | 80 |
| `ImageStoreImpl.java` | impl: validate extension (png/jpg/jpeg/gif/bmp) and ≤ 20 MB, decode via `ImageIO`, downscale to 1024 px longest edge, re-encode JPEG q0.85, atomic tmp+move write into `kitnapper-images/`; tolerant reads and the load-time orphan sweep (design §12) | 191 |
| `SeedPhotos.java` | installs the ten bundled seed photos into `kitnapper-images/` — classpath `/photos` first, `resources/photos` as the `Fonts`-style fallback; idempotent, never overwrites, a missing bundled file is logged and skipped (D50) | 98 |
| `AppContext.java` | holds the single set of service instances (incl. `CatalogService`, `LoanService`, `ImageStore`, `InventoryService`), the shared `LateFeeStrategy`, the `DataStore`, an injectable `Supplier<LocalDate>` clock, the current session `User`, and the theme toggle; builds the photo store, installs the bundled seed photos, sweeps orphans on load, re-binds the session after a reseed (D33); simple `addChangeListener(Runnable)` for views (NFR-02, §5 D2) | 150 |

### 2.3 `kitnapper.service.persistence` (hand-written JSON — no libraries)

| File | Contents | Est. lines |
| :--- | :--- | --: |
| `JsonParser.java` | recursive-descent parser for **only the subset `JsonWriter` emits** (§3.2); document structure only | 110 |
| `JsonScanner.java` | character layer for the parser: strings with the three escapes, plain-decimal numbers, `true`/`false`/`null`, whitespace, position-based errors | 120 |
| `JsonWriter.java` | writer: pretty-printed, 2-space indent; strings escaped (`\"`, `\\`, `\n`); numbers as plain decimals | 80 |
| `Json.java` | tiny helpers to build/read the JDK-native representation (`Map<String,Object>`, `List<Object>`, `String`, `Double`, `Boolean`, `null`); returns defensive copies so no unchecked casts are needed | 100 |
| `UserMapper.java` | `User` ⇄ JSON (role field selects `Borrower`/`Manager` on read) | 70 |
| `EquipmentMapper.java` | `Equipment` ⇄ JSON for all three concrete kinds (DIGITAL/MECHANICAL/BASE); writes `kind` via `instanceof`, restores status by replaying transition methods; optional `photoFile` (null-safe, D8); `Optional` getters via `orElse(null)` | 90 |
| `LoanMapper.java` | `LoanRecord` ⇄ JSON; `returnDate` as `null` when absent; normalizes a persisted `OVERDUE` to `ACTIVE` (never re-serializes it, D18) | 70 |
| `PenaltyMapper.java` | `Penalty` ⇄ JSON; `waiveReason` as `null` when not waived | 60 |
| `RootMapper.java` | top-level document ⇄ `AppData` (D17): the four arrays + `schemaVersion` + `savedAt` | 80 |
| `SeedData.java` | the PRD §9 dataset, computed **relative to the date seeding runs** (§5 D4); seed items reference their bundled photo, `<id>.jpg` (D50); kinds default by category (D19) | 130 |

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
| `ResizeEdge.java` | package-private enum: hit-test geometry (`at`) and drag math (`dragTo`, min-size clamp) for the resize bands | 100 |
| `TitleBar.java` | 32 px title bar: wordmark, screen title, theme toggle, min/max/close painting + hit targets (design §5) | 140 |
| `Omnibar.java` | breadcrumb + contextual command row + search field + layout toggle (design §5) | 160 |
| `StatusBar.java` | counts, selection summary, monospace aggregate (design §5) | 70 |
| `PreviewPane.java` | right detail pane: 16:9 media, metadata rows, pinned primary action (design §6) | 150 |
| `PhotoTile.java` | **(M5, D27)** off-EDT image decode + cache; skeleton placeholder; falls back to `GradientTile` (design §12) | 120 |
| `GradientTile.java` | category fallback tile — the single permitted gradient (design §3.4/§3.5) | 80 |
| `SegmentedControl.java` | segmented control + toggle switch per design §6 | 90 |
| `ThinScrollBarUI.java` | thin 4 px-thumb scrollbar UI (design §6) | 80 |
| `SkeletonPanel.java` | **(M5, D27)** skeleton loading blocks — visual only, timing in `SkeletonGate` (design §13) | 110 |
| `SkeletonGate.java` | **(M5, D27)** skeleton timing/plumbing: off-EDT work, 150/300/167 ms, 10 s watchdog, accessible loading state (design §13) | 120 |

Window & screens (design §5 inventory):

| File | Contents | Est. lines |
| :--- | :--- | --: |
| `LoginFrame.java` | window shell: shared title bar + centered card, inline error, sign-in callback | 100 |
| `LoginCard.java` | package-private sign-in card layout: wordmark, fields, error line, primary button, collapsible demo hint | 150 |
| `MainFrame.java` | hosts `WindowChrome`/`TitleBar`/`Omnibar`/`StatusBar`, sidebar + content `CardLayout` swap | 150 |
| `ScreenCatalog.java` | package-private route metadata: titles/subtitles, `navKeys`, `defaultKey`, sidebar titles | 70 |
| `PlaceholderScreen.java` | package-private real page header + muted "No records to show yet." surface for unimplemented routes | 40 |
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
| `Main.java` | entry point (41 lines, M2): installs `Theme`, registers fonts, constructs services + `AppContext`, shows `LoginFrame` on the EDT. Stays under 60 lines | 60 |

M3 also delivered files the estimate above did not name (the cap and the screen
split forced them). The pre-estimated M3 files above are now at their real
sizes: `CatalogScreen` 199, `PreviewPane` 197, `EquipmentCard` 144, `Badges` 130,
`GradientTile` 78, `SegmentedControl` 189, `EmptyState` 53.

| File | Contents | Lines |
| :--- | :--- | --: |
| `view/CatalogPresenter.java` | query/selection/status state for the catalog; keeps `CatalogScreen` layout-only | 118 |
| `view/CatalogFilters.java` | category + status combos and the table/cards segmented control (omnibar commands) | 118 |
| `view/CatalogTable.java` / `CatalogTableModel.java` | non-opaque hover-painted table + typed model | 183 / 87 |
| `view/CatalogCards.java` | 3-column card grid in a width-tracking scroll pane | 125 |
| `view/CheckoutDialog.java` / `DatePicker.java` | `T … T+14` picker + modal checkout body | 77 / 54 |
| `view/PageHeader.java` | page title + subtitle row shared by every screen | 24 |
| `controller/CatalogController.java` / `BorrowingController.java` | query state + booking actions with user-facing messages | 89 / 67 |
| `service/Ids.java` | sequential `e-0NN` / `L-0NN` / `p-0NN` generation (D22) | 53 |
| `service/CatalogService(+Impl)` | name/serial search, category + status filters, deterministic sort | 27 + 54 |
| `service/LoanMessages.java` / `LoanQueries.java` / `LoanServiceImpl.java` | user-facing failure text, derived status/fee queries, checkout/return rules | 32 / 84 / 147 |

M4 also delivered the loan/penalty files this plan predicted, plus the splits
the cap and the controller extraction forced (M4 = 2026-11-16):

| File | Contents | Lines |
| :--- | :--- | --: |
| `controller/LoansController.java` | my/all/overdue queries, return-date list, return logging, fee on a date | 103 |
| `controller/DashboardController.java` | KPI counts, overdue rows, derived activity (D28) | 76 |
| `controller/LoanRows.java` | the single row builder for loans and penalties (D29) | 144 |
| `controller/LoansActions.java` / `CatalogActions.java` | view-facing callback contracts | 58 / 70 |
| `controller/ScreenMounts.java` | screen construction, mounting and theme refresh, extracted from `AppController` (183 → 116) | 116 |
| `service/PenaltyService(+Impl)` / `PenaltyMessages.java` | settlement interface, one-way rules, user-facing failures (D31) | 36 + 128 / 13 |
| `view/LoansTable.java` / `LoansTableModel.java` | typed, non-opaque loan table + per-mode columns | 178 / 151 |
| `view/LoansTableSection.java` / `LoansWidthPolicy.java` | table + empty state in one section; scroll-until-fits resize policy | 77 / 52 |
| `view/LoanPreviewPane.java` / `PenaltyPreviewPane.java` | 280 px loan and penalty previews with the pinned action | 194 / 195 |
| `view/MyLoansScreen.java` / `AllLoansScreen.java` / `OverdueScreen.java` / `PenaltiesScreen.java` | the four loan screens | 156 / 166 / 166 / 176 |
| `view/DashboardScreen.java` / `KpiCard.java` / `ActivityList.java` | KPI row, overdue table, activity rail | 135 / 61 / 105 |
| `view/ReturnDialog.java` / `SettlePenaltyDialog.java` | §8.8 return and §8.9 settlement dialogs | 127 / 145 |
| `view/LoanRow.java` / `PenaltyRow.java` / `ActivityEntry.java` | view records built by controllers (D29) | 22 / 18 / 14 |
| `view/PenaltiesTable.java` / `PenaltiesTableModel.java` | penalty table + typed model | 171 / 105 |

Total 117 files after M4 + the width pass (18 model, 16 service, 10 persistence,
10 controller, **62 view**, `Main`; 10,788 lines), plus 25 probe files
(2,811 lines, 12 registered probes) under `desktop-app/test/` that never compile
into the app jar.
Every file stays under the 200-line cap; the current maximum is
`CatalogScreen.java` at 199 lines, so split it before adding to it.

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
| M3 | Catalog + checkout | `CatalogScreen` + `CatalogPresenter`, `CatalogFilters`, `CatalogTable(Model)`, `CatalogCards`, `EquipmentCard`, `PreviewPane`, `GradientTile`, `SegmentedControl`, `Badges`, `EmptyState`, `PageHeader`, `CheckoutDialog`, `DatePicker`, `CatalogController`, `CatalogService(+Impl)`, `BorrowingController`, `LoanService(+Impl)`, `Ids`, `Toasts` | steps 3–6: search/filter, checkout, availability lock, 15-day rejection; exceptions never reach the EDT (D16, D21); managers get a read-only catalog and no checkout action (D24); the due-date picker makes an out-of-window date unreachable, so step 6 is proven by `CatalogProbe`/`CatalogUiProbe` (D25); media is the gradient tile only — photos and skeletons are M5 (D27); `./build.sh` + `test/run-all.sh` (9 probes) are the gate |
| M4 | Returns, fees, manager views | `DashboardScreen`, `AllLoansScreen`, `OverdueScreen`, `MyLoansScreen`, `LoansTableModel`, `PenaltiesScreen`, `PenaltiesTableModel`, `ReturnDialog`, `SettlePenaltyDialog`, `PenaltyController`, `PenaltyService(+Impl)`, `Badges` | **Delivered 2026-11-16 (WP0–WP5 + width pass).** steps 7–10: persistence round-trip (restart), overdue dashboard ₱200, return freezes penalty, waive; extra checks: a due-date return creates **no** penalty and a maintenance-flagged item stays `UNDER_MAINTENANCE` (D15, D20); tables use `effectiveStatus(now)`, fees come from `LoanService` (D21); `./build.sh` + `test/run-all.sh` (12 probes, `LoansUiProbe` registered) are the gate |
| M5 | Inventory, photos + reset | `InventoryScreen` + `InventoryBody` + `InventoryTable(Model)` + `InventoryPreviewPane` + `InventoryStateActions(+Runner)`, `InventoryController`, `InventoryActions`, `InventoryService(+Impl)`, `ImageStore(+Impl)`, `ImageRules`, `PhotoCodec`, `EquipmentFactory`, `ItemDraft`, `EquipmentDialog` + `EquipmentFields`, `PhotoWell`, `InventoryEditor`, `PhotoTile`, `PhotoCache`, `PhotoSource`, `SkeletonPanel`/`SkeletonGate`, `SettingsScreen` + `SettingsSections`, `ToggleSwitch`, `SettingsController`, `ItemPhotos`, `PageHeader` action slot | **Delivered 2026-11-16 (WP0–WP5 + close-out).** steps 11–12: add item **with photo** + decommission, reset restores the §6.1 baseline and wipes `kitnapper-images/`; extra checks: kind preserved on category edit (D19), photo filename safety and staging (D22, D35), clear/decommission refused while the item is out (D36); skeletons cover photo decode and import only (D32); the "Animate loading placeholders" switch ships (design §13.4); `./build.sh` + `test/run-all.sh` (**18 probes**, five new) are the gate |
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
| D24 | Does the manager get a Catalog screen, and can it check items out? | **Yes to the screen, no to checkout.** Manager nav is Dashboard, Catalog, All Loans, Overdue, Penalties, Inventory; the catalog mounts the same `CatalogScreen` with `Actions.isManager()` true, so the preview shows the caption "Manager view — checkout is borrower-only." and no button (INV-3). Borrowers keep Catalog, My Loans. |
| D25 | How is INV-2's 14-day window verified when the UI cannot express a 15-day date? | The due date is a non-editable picker over `T … T+14` (`DatePicker`), so the invalid date is unreachable in the demo. `LoanServiceImpl` still rejects out-of-window dates defensively, and `CatalogProbe` asserts the rejection; SRS §6.2 step 6 cites the probe instead of manual entry. |
| D26 | How is a screen mounted, and how did the cap shape M3? | `MainFrame.mount(key, screen, commands, onShown)` + `showScreen(key)`; views stay layout-only by delegating state to a presenter (`CatalogPresenter`) and callbacks to the controller, and the split files listed in §2.6 are the result. M4 reuses the same pattern for every loan screen. |
| D27 | Do photos and skeleton loading ship in M3? | **No — deferred to M5.** Seed items carry no photos (D11), so a decode path and a loading skeleton have nothing to exercise until the inventory photo picker exists. M3's catalog uses `GradientTile` for every card and preview and keeps queries synchronous (design §13.1); `PhotoTile`, `SkeletonPanel`, `SkeletonGate` and their two probes ship with M5. |
| D28 | What is Dashboard "recent activity"? | Last six borrow/return events derived from loans (date desc, loan id desc; a loan's return sits above its checkout on the same day). No persistence, no schema change. |
| D29 | How are loan/penalty rows built? | View records (`LoanRow`, `PenaltyRow`, `ActivityEntry`) built from a single `controller/LoanRows` helper; controllers resolve ids→names/fees, views only format. Views never import services. |
| D30 | Is return reachable from Overdue as well as All Loans? | **Yes.** Same preview `Log return` action on both screens; documented deviation from design §5's single mention of All Loans. |
| D31 | Penalty settlement rules? | One-way, manager-only, reason-mandatory for waive; `PenaltyService` is the only caller of `Penalty.markPaid`/`waive`. Blank reason → rejected, fee stays UNPAID. |
| D32 | How far do the loading skeletons go in M5? | **Photos only** — card/preview decode and photo import. The measured ≥ 150 ms gate for catalog/loan queries stays specified in design §13.1 and is audited in M6; gating locked M3/M4 screens adds risk without a demo step. **Closed 2026-11-16 (M6 WP4):** measured, not gated — see D40. |
| D33 | What happens to the session after "Reset demo data"? | **It stays signed in.** A reseed replaces every user instance, so `AppContext.rebindCurrentUser()` re-resolves the session user by email afterwards; without it the manager would hold a `User` that is no longer in the store and every role check would read a stale object. |
| D34 | Where does the photo alpha matte come from? | `ImageStore.stage(source, matteRgb)`; the **controller** passes `Theme.bgSurface.getRGB()`. Design §12's "flatten onto `BG_SURFACE`" is honoured while every token stays in `view/Theme` (a view constant in the service would invert the layering rule). |
| D35 | How does an import become an item photo? | Import runs on file choice (the dialog's confirm handler is synchronous on the EDT), writes `kitnapper-images/staging-<n>.jpg`, and the save promotes it to `<equipmentId>.jpg`. Cancel or replace deletes the staged file; the load-time sweep removes stray staged files and photos whose stem is not a live id. |
| D36 | Can inventory actions break an open loan? | **No.** `clearMaintenance` and `decommission` are refused with "This item is still on loan." while an unreturned loan references the item: clearing would call `markAvailable()` and make an out item borrowable again, and decommissioning would make `LoanServiceImpl.returnLoan` reject its own return. |
| D37 | How does Inventory filter, and where do Add/Edit live? | The omnibar text filters name and serial through `CatalogService` (same ordering as the catalog); no category/status chips and no layout toggle in M5. **Add item** sits in the `PageHeader` action slot and **Edit item** is the preview's primary action, so a category edit never has to go through the table. |
| D38 | Thin scrollbars? | **Yes.** `ThinScrollBarUI` replaces the stock bar through one `UIManager` registration in `Theme.installDefaults()`: transparent track, 4 px thumb widening to the full lane on hover, no arrow buttons. The 8 px lane keeps its layout space — a true overlay would need a custom `JScrollPane` fighting selection, wheel and focus on all five scroll surfaces (design §6, §14.2). |
| D39 | Which Fluent motion ships? | Dialog open fade + 98→100 % grow and close fade (83 ms), the 120 ms screen fade on navigation, and the existing 167 ms toast slide + fade, all through one `Motion` facade with decelerate easing. The **theme crossfade is cut** — the switch stays instant. `-Dkitnapper.motion=off` is a probe-only switch, not a user setting; dialog fades fall back to the instant path without per-pixel translucency (design §7, §14.2). |
| D40 | Do catalog/dashboard/loan queries get skeletons? | **No code change; close D32 by measurement.** Per-query timings at seed scale are asserted far below the 150 ms gate; gating stays measurement-driven (design §13.1). A 10-item scan is microseconds, so a worker would only add risk to locked screens. **Closed 2026-11-16 (M6 WP4):** eight queries timed over 100 runs after a 20-run warmup — catalog all/text/filter, loans mine/all/overdue, dashboard activity/overdue — slowest single run **0.46 ms**, medians 0.01–0.12 ms. No gating. |
| D41 | Which tables get the width policy? | **All four**: catalog, inventory, penalties and loans share `TableWidthPolicy`. A column never shrinks below its preferred width; a narrow viewport sets `AUTO_RESIZE_OFF` and scrolls horizontally, a wide one restores the preferred widths and stretches to fill. Replaces the loans-only `LoansWidthPolicy` (design §9, §14.2 `D-Width`). |
| D42 | How does the preview survive 1024×640? | **Compact density plus an internal scroll guard**, after measurement corrected the estimate: the shortfall at 1024×640 is 48 px (catalog) and 174 px (inventory), not ~28 px. `PreviewLayout` owns the row factory, gaps and caption lines; below 560 px of preview height the gaps tighten (12→8, 2→1, 10→6) and the manager hint drops to one line. The metadata area — never the action bar — sits in a borderless thin-scrollbar guard that appears only when the rows still do not fit. At 1280×800+ nothing scrolls. **Amended by D51:** the guard is `PreviewScroll`, a `Scrollable` view that tracks the viewport's width but not its height, so the column keeps its preferred height and the media block is not stretched. |
| D43 | Where do toasts sit? | **Above the status bar and clear of the preview column.** 16 px side / 96 px bottom normally (the 96 px clears the 32 px status bar but measured 1 px short of the preview's action bar at 1024×640), and a 320 px right inset while a preview pane is showing, because the inventory preview's three pinned actions make its bar 120 px tall. Settings keeps the plain bottom-right corner. |
| D44 | How do long toasts fit? | **Wrap once before ellipsising.** The WP5 pass showed checkout/return messages losing the due date or the frozen fee. `ToastView` wraps the message to the text budget over at most two lines (the second ellipsises), reports a 48 px or 66 px preferred height, and `Toasts` stacks from the measured heights. The accessible name keeps the full text. |
| D45 | How do focused combos look? | **Tokens plus the accent underline, never the LAF selection fill.** Metal paints a focused closed combo with the list-selection colours (blue fill, white text) and no focus cue. `ComboSkin extends MetalComboBoxUI`, fills `BG_CONTROL`, paints the value as unfocused, and draws the 2 px `ACCENT` bottom border a text field shows; `DatePicker`'s cell sets the same tokens. `SegmentedControl` takes focus on click, so the §6 focus rectangle is reachable by mouse as well as keyboard. |
| D46 | How wide is a status column? | **From the widest badge, not a magic number.** Catalog 125 px / inventory 140 px clipped "Decommissioned". Both tables now size the column from `Badges.widestPillWidth()` + 2 × `CELL_PAD` + 4 px; `GeometryChecks` asserts the delivered width, and the D41 policy scrolls rather than shrinking it. |
| D47 | Which screens show the search box, and who owns the status bar? | **Only Catalog and Inventory search; each route owns its status line.** `ScreenCatalog.Meta.searchable` drives `Omnibar.setSearchVisible` (Ctrl+F is a no-op elsewhere; the typed query survives navigation). `MainFrame` remembers the left/right text per route and restores it on routing, and `ScreenMounts.statusFor(key)` drops writes from a screen that is not routed — a hidden screen reloading after a reset can no longer post "10 items · 0/1 selected · ₱372,000.00" onto Settings. |
| D48 | Why did dialog copy clip? | **Wrap to the content width before the card pins its size.** Long reset/decommission sentences and the return dialog's fee note were cut at the card edge. `DialogSkin.setBodyText` wraps to 480 − 48 px (4 lines max); `ReturnDialog.feeNote` wraps to the form's 360 px label column. `DialogTextChecks` asserts the full sentence and the absence of an ellipsis. |
| D49 | When does the catalog status line update on selection? | **Immediately.** Clicking a row refreshed the preview but left "0/1 selected" until the next reload. `CatalogScreen.select` now re-publishes the status after `CatalogPresenter.select`. |
| D50 | Should the demo ship with photos? | **Yes — ten bundled photos.** The catalog preview, cards and inventory rows all showed the §3.5 category tile because the seed shipped without photos (D11) and reset wipes the folder. Ten freely licensed Wikimedia Commons JPEGs now live in `desktop-app/resources/photos`, pre-encoded at 1024 px / q0.85 so `SeedPhotos.install` is a byte copy (no first-launch or reset decode cost). Install runs in `AppContext` before the orphan sweep and again after `store.resetToSeed()` in `InventoryServiceImpl.resetDemoData`; it never overwrites, so a manager's replacement survives a restart, and an item with no `photoFile` is skipped. No view/controller/`ImageStore` change, `schemaVersion` unchanged, and the app still makes zero network calls. Saves seeded before this change need one "Reset demo data". |
| D51 | How does the preview media block keep its 16:9 shape? | **The slot lays out its tile at the measured width, never a fixed height.** The §12 media slot used to be a plain `BorderLayout` panel with a 0.5 `alignmentX` and no maximum size inside the `BoxLayout` info column. `BoxLayout` aligns the cross axis from the alignment offsets and grows each child to its maximum, so the centered slot was widened to 361 px inside a 247 px pane and the tile stretched to the full column height — the placeholder and any photo were clipped right and bottom. Three changes at the root: the slot is left-aligned and capped (`PreviewMedia`, `PreviewLayout.mediaSlot`), the tiles are left-aligned and advertise no stretching, and `MediaRatioLayout` derives the tile height from the slot's real width so the block stays 16:9 when the pane grows its vertical scrollbar (the info column narrows by the bar width at 1024×640). `PreviewScroll` stops the viewport from stretching the column vertically, which `D42` already assumed. `GeometryChecks` asserts `media-inside` and `media-ratio` at all four sizes. |
| D52 | Why did form fields keep an accent underline and a placeholder after focus moved, and why was the category arrow an empty square? | **Two painting defects with one common theme: state read at paint time, never repainted.** (1) `FieldSkin` draws the 2 px `ACCENT` bottom border and the muted placeholder from `hasFocus()`, but nothing repainted the field on a focus change — Swing's text UI repaints only the caret's own strip — so an unfocused field kept the previous frame's accent underline and a newly focused field kept its placeholder *under* the caret. `FieldSkin.style` now installs a focus listener that repaints the whole field. (2) Metal paints the combo arrow in `MetalLookAndFeel.getControlInfo()` = `#333333`, a single shade from the `#323232` `BG_CONTROL` fill, so on the dark theme the arrow vanished and only the LAF button border remained, as an empty square. `ComboSkin` now sets its own `Chevron` icon in `TEXT_SECONDARY` and drops the LAF button border, keeping Metal's button so the popup click still works. `FocusRepaintChecks` (run from `FocusCueChecks`) asserts `field-repaints-on-focus`, `field-no-stale-underline`, `field-bottom-border-still-present`, `combo-chevron-visible` and `combo-chevron-not-laf-colour`; the chevron and listener assertions fail on the pre-fix code (2 failures) and pass after (5 passes). |
| D53 | Why did card photos paint over the header while the cards view scrolled? | **The tiles replaced the clip Swing handed them.** `PhotoTile.paintComponent` and `GradientTile.paintComponent` called `g2.setClip(roundedShape)`, which discards the incoming damage clip instead of intersecting it. A scroll repaint clips to the newly exposed strip inside the viewport; a tile that straddles the viewport's top edge then painted its full 16:9 block from its own origin, so the photo (or the fallback gradient) appeared above the scroll pane, over the page header. Both now call `g2.clip(shape)`, which intersects with the incoming clip and confines the tile to the area Swing asked it to repaint. The defect was reproduced on the real cards tree scrolled under a 60 px banner — 47,160 and 18,831 painted pixels above the pane at two offsets, 0 after the change — and is pinned by `PhotoTileChecks` (`a photo tile honours the inherited clip`, `a fallback tile honours the inherited clip`), which fail on the pre-fix code and pass after. |
| D54 | Why were catalog card photos blurry? | **The thumbnail was decoded smaller than it is painted, and shrunk in one pass.** (1) `PhotoTile.CARD_EDGE_PX` was 160 while the card's 16:9 media block paints at 200 logical px at the default window — and the 3-column grid stretches it wider when the window grows — so `PhotoTile` bilinear-upscaled every card; at the reported `uiScale` 1.25 that was a 160 px source drawn into ~234 device px. The target is now **400**: 200 × 1.5 covers the card at every supported UI scale, with headroom for a wider grid. (2) `PhotoCodec.downscale` did the whole 1024 → 160 reduction in a single bilinear pass, which averages a large source neighbourhood into each output pixel; it now halves while the step still stays above the target and finishes with one bicubic pass, so the card thumbnail and the 640 px preview keep their edges (a 1024 → 640 preview is one stepped pass, unchanged in cost). `PhotoTile.paintComponent` paints the residual cover-scale bicubically. `PhotoCache` still keys by `file@edge` and keeps its 32-entry LRU (~430 KB per 400 px card copy). Pinned by `PhotoTileChecks`: `the card decode target carries 1.5x device pixels` (fails on 160) and `the preview target stays above the card target`. |
| D55 | How does the app reach a demo machine with no JDK? | **A portable app-image built with the JDK's own `jpackage`, not an installer.** `package.bat` compiles with `build.bat`, jars the classes plus `resources\` (Inter and the bundled seed photos, so `/fonts` and `/photos` resolve from the classpath exactly as `Fonts` and `SeedPhotos` already look for them), then runs `jpackage --type app-image --add-modules java.desktop` into `build\dist\KitNapper`: `KitNapper.exe`, `app\kitnapper.jar`, `runtime\` (~77 MB, ~26 MB zipped, JDK 17 or newer, no third-party tool). An **installer** was rejected for a product reason, not a build one: it would land in `Program Files`, where a standard user cannot write, and the save file is defined to live in the application folder — so the folder stays portable and must be extracted somewhere writable. `kitnapper-data.json` and `kitnapper-images\` therefore follow the working directory as before (measured: launching the exe with `Documents` as cwd wrote the save there), which Explorer sets to the exe's folder on a double-click; the README tells the user to start it from the exe and to leave a shortcut's "Start in" alone. No source change, `schemaVersion` unchanged, Ant stays canonical, and the launcher has no console window. Unsigned, so a downloaded copy may raise SmartScreen; a USB copy usually does not. |

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
| 1.5.0 | 2026-11-15 | Debt cleanup: line-cap splits recorded in the inventory (`JsonScanner`, `ResizeEdge`, `ScreenCatalog`, `PlaceholderScreen`, `LoginCard`, `LoanQueries`), `Json` documented as unchecked-cast free, `-Xlint:all` build, 65-file count; probe harness relocated to `desktop-app/test/kitnapper/` (WP0.5, planned) |
| 1.6.0 | 2026-11-15 | Photos and skeleton loading deferred from M3 to M5 (D27): the M3 catalog renders `GradientTile` only; M3/M5 milestone rows and the view inventory adjusted |
| 1.7.0 | 2026-11-15 | WP0.5 executed: eight probes rebuilt under `desktop-app/test/kitnapper/` (+ `run-all.sh`; `EdgeProbe`/`SizeProbe` in `kitnapper.view` for package-private surfaces), suite green, baselines re-recorded in `HANDOFF-M3-PLAN.md` §9.4 |
| 1.8.0 | 2026-11-16 | M3 delivered and recorded: catalog screen + preview + cards + filters + checkout dialog, manager read-only catalog (D24), picker/probe verification of INV-2 (D25), mounting + split strategy (D26); §2.5/§2.6 inventory updated to the delivered 85 files / 7,285 lines with `CatalogScreen` at the 199-line maximum; milestone row and revision numbering corrected |
| 2.1.4 | 2026-11-24 | Post-M6 defect: in the catalog cards view, scrolling painted the card photos above the scroll pane, over the page header. Root cause was painting, not layout — `PhotoTile` and `GradientTile` replaced the inherited damage clip with `setClip` instead of intersecting it, so a tile straddling the viewport's top edge painted its full 16:9 block. Both now use `clip`, D53; `PhotoTileChecks` gained the inherited-clip assertions for a photo and a fallback tile (they fail on the pre-fix code); 21/21 probes green at UI scale 1.0/1.25/1.5 |
| 2.1.3 | 2026-11-24 | Post-M6 defect: the preview's §12 media block was clipped (placeholder and photos cropped right and bottom). Root cause was layout, not painting — the centered, uncapped media slot in the `BoxLayout` info column was widened and stretched by `BoxLayout`, and a fixed-height tile went off-ratio once the pane grew its scrollbar. Fixed by `MediaRatioLayout` (height from the measured width), left-aligned capped slots, and `PreviewScroll` (the D42 guard tracks the viewport's width but not its height), D51; `GeometryChecks` gained `media-inside` and `media-ratio`, asserted at 1024×640/1280×800/1366×768/1920×1080; 21/21 probes green at UI scale 1.0/1.25/1.5 |
| 2.1.2 | 2026-11-17 | M6 WP5 interactive pass recorded: SRS §6.2 steps 1–12 executed on a live display at 1024×640/1366×768/1920×1080 in both themes and by keyboard; defects F2–F7 and F9–F16 fixed at the root (D44–D49) with probes (toast wrapping, combo/date-picker focus, status-column width, route search/status, dialog copy, selection status); 20/20 probes green; demo data restored to the §6.1 baseline |
| 2.1.1 | 2026-11-16 | M6 WP4 audits recorded: the §10 icon resolution fixed (`Fonts.symbol()`; fallback glyphs were drawn with the physical Inter font and rendered as missing-glyph boxes on Linux), glyph table asserted by `GlyphChecks`, suite green at UI scale 1.0/1.25/1.5, D32/D40 closed with measured query timings (max 0.46 ms), skeleton §13 list closed with `SkeletonAuditChecks` (real 10 s watchdog, focus opt-out, accessible name once), both-theme sweep via `ThemeSweepChecks` |
| 2.1.0 | 2026-11-16 | M6 in progress (WP0–WP3): thin scrollbars (D38), Fluent motion fades (D39), the skeleton audit closed by measurement (D40), one width policy for all four tables (D41), compact preview density + scroll guard (D42), preview-aware toast placement (D43); `GeometryProbe` added (20 registered probes) |
| 2.0.0 | 2026-11-16 | M5 delivered: inventory table/preview/actions, add-edit dialog with photo staging, JDK-only photo pipeline (`ImageStore`, `PhotoCodec`, `ImageRules`), photo tiles with the first loading skeletons, Settings + demo reset; decisions D32–D37 added; inventory now 154 source files / 14,450 lines with 38 probe files / 4,287 lines and 18 registered probes (242 classes); `HANDOFF-M5-PLAN.md` holds the record |
| 1.9.0 | 2026-11-16 | M4 delivered: returns, late fees, penalty settlement, My Loans / All Loans / Overdue / Penalties screens, manager Dashboard, `PenaltyService(+Impl)`, `PenaltyController`, `LoansController`, `DashboardController`, `LoanRows`, `ScreenMounts`, `CatalogActions`, width pass; inventory 117 files / 10,788 lines, 25 probe files / 2,811 lines; decisions D28–D31 added; 12 probes registered (`LoansUiProbe` joins the suite) |
| 2.2.1 | 2026-11-24 | Post-M6 defect: form fields kept a stale accent underline and a placeholder after focus moved, and the category combo's arrow rendered as an empty square. Root causes were painting, not layout — `FieldSkin` reads `hasFocus()` at paint time but never repainted on a focus change (the text UI repaints only the caret strip), and Metal's arrow is `#333333` on the `#323232` control fill. Fixed by a focus listener that repaints the field and a `ComboSkin.Chevron` icon in `TEXT_SECONDARY`, D52; `FocusRepaintChecks` added (5 assertions, 2 of which fail pre-fix); 21/21 probes green at UI scale 1.0/1.25/1.5 |
| 2.2.0 | 2026-11-24 | D50 recorded and `SeedPhotos` added to the class inventory; seed items now reference bundled photos restored on first launch and after a reset; `SeedPhotosProbe` registered (21 probes) |
| 2.2.2 | 2026-11-24 | Post-M6 defect: catalog card photos rendered blurry. Root cause was the decode path, not the theme — the card tile decoded a 160 px thumbnail and painted it into a 200–234 device px media block, and `PhotoCodec.downscale` did the whole 1024 → 160 reduction in one bilinear pass. The card target is now 400 px, `downscale` steps (bicubic halving) before its final pass, and `PhotoTile` paints the residual cover-scale bicubically, D54; design §12/§13 and the display constants updated; `PhotoTileChecks` gained the 300-device-pixel floor assertion; 21/21 probes green at UI scale 1.0/1.25/1.5 |
| 2.3.0 | 2026-11-24 | Demo delivery: `build.bat`, `run.bat`, `reset-demo.bat` (Windows, JDK only) and `package.bat`, which builds a portable app-image with `jpackage` — exe + private `java.desktop` runtime + jar carrying `resources\`, ~77 MB (~26 MB zipped), D55; `.gitattributes` pins `*.bat`/`*.cmd` to CRLF; no source change, `schemaVersion` unchanged, probes still 21/21 green |
