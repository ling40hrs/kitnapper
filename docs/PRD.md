---
project_name: "KitNapper"
team_name: "YAPPERS"
course: "CS203 – Object-Oriented Programming"
artifact_type: "Product Requirements Document (PRD) / AI Context File"
target_platforms:
  - "Desktop (Java 17+ / Swing)"
architecture_style: "OOP / MVC / Local JSON Persistence"
version: "3.1.1"
---

# Project Specification: KitNapper

## 1. Context & Executive Summary
**KitNapper** is a desktop asset-lending management system designed for institutions (universities, media production studios, IT departments, engineering firms). It replaces error-prone physical logbooks by tracking asset availability, borrow/return lifecycles, and penalty fees via a GUI.

Primary academic requirement: Demonstrate the four core **OOP principles** — Encapsulation, Inheritance, Polymorphism, and Abstraction — through a strict layered architecture (MVC) and a pluggable **fee-rule** design.

Borrowing is **instant**: a borrower checks an item out directly, with no manager approval step. Returns are logged without a damage assessment; the only monetary penalty is a **late fee in Philippine pesos (₱50/day)**.

---

## 2. System Actors & Roles

| Role | Identifier | Permissions & Capabilities |
| :--- | :--- | :--- |
| **Borrower** | `ROLE_BORROWER` | Browse catalog, view live availability, check out available items, view "My Loans" and outstanding penalties. |
| **Equipment Manager** | `ROLE_ADMIN` | View all loans, log returns, settle penalties, adjust inventory, inspect overdue dashboard, reset demo data. |

*Note: Authentication is mocked **by design**. The login screen recognises the seeded accounts with fixed demo passwords (see §9.1); real account verification is a strict non-goal (see §8).*

---

## 3. Domain Model & Class Structure

### 3.0 Relationships Overview

```
                        ┌─────────────────────────┐
                        │     User  (abstract)    │
                        │─────────────────────────│
                        │ userId: String          │
                        │ fullName: String        │
                        │ email: String           │
                        │─────────────────────────│
                        │ + getRole(): UserRole   │  ← abstract
                        └───────┬─────────┬───────┘
                                │         │
                     inherits   │         │   inherits
                                │         │
               ┌────────────────▼──┐   ┌──▼───────────────┐
               │     Borrower      │   │     Manager      │
               │───────────────────│   │──────────────────│
               │ + checkout(...)   │   │ + processReturn()│
               │ + viewMyLoans()   │   │ + settlePenalty()│
               └─────────┬─────────┘   └────────┬─────────┘
                         │ 1                    │ 1
              creates    │                      │  processes
                         ▼ *                    ▼ *
┌────────────┐ *     1 ┌─────────────────┐ 1    0..1 ┌────────────┐
│ Equipment  ├─────────┤   LoanRecord    ├───────────┤  Penalty   │
└────────────┘         └─────────────────┘           └────────────┘
```

- `User` is **abstract**; `Borrower` and `Manager` inherit from it (*Inheritance*), and role-dependent behavior is resolved by dynamic dispatch (*Polymorphism*).
- **Equipment 1 → \* LoanRecord:** one item accumulates many loan records over its lifetime; each `LoanRecord` references exactly one item.
- **LoanRecord 1 → 0..1 Penalty:** a loan generates at most one penalty (a single `LATE_FEE`; there is no damage fee).
- **Borrower checks items out**; **Manager logs returns and settles penalties**.

### 3.1 Entities

#### `Equipment` (Abstract or Base Class)
- **Attributes:**
  - `id: String` (UUID / Unique Tag)
  - `name: String`
  - `category: EquipmentCategory` (`CAMERA`, `LENS`, `LAPTOP`, `AUDIO`, `TOOL`, `OTHER`)
  - `status: EquipmentStatus` (`AVAILABLE`, `LOANED`, `UNDER_MAINTENANCE`, `DECOMMISSIONED`)
  - `serialNumber: String`
  - `replacementValue: Double` (in PHP)
  - `photoFile: String` *(optional; relative filename under `kitnapper-images/` — see §7.2 and design.md §12)*
- **Methods:**
  - `markAvailable()`, `markLoaned()`, `markUnderMaintenance()`, `decommission()`
- **Rule:** every method validates the legal transition for the current status; illegal transitions are rejected in the model layer (see INV-1, §6).

#### `LoanRecord`
- **Attributes:**
  - `loanId: String`
  - `borrowerId: String`
  - `equipmentId: String`
  - `borrowDate: LocalDate`
  - `dueDate: LocalDate`
  - `returnDate: Optional<LocalDate>`
  - `loanStatus: LoanStatus` (`ACTIVE`, `OVERDUE`, `RETURNED`)
- **Methods:**
  - `checkout(...)` (constructor/factory validation), `completeReturn(returnDate: LocalDate)`, `accruedFee(asOf: LocalDate): Double`, `isOverdue(asOf: LocalDate): Boolean`

#### `User` (Abstract Base Class)
- **Attributes:**
  - `userId: String`
  - `fullName: String`
  - `email: String`
  - `password: String` *(mock credential, compared at login)*
- **Methods:**
  - `getRole(): UserRole` *(abstract — implemented by each subclass)*

#### `Borrower extends User`
- **Methods:**
  - `checkout(equipmentId: String, dueDate: LocalDate): LoanRecord`
  - `viewMyLoans(): List<LoanRecord>`
  - `viewOutstandingPenalties(): List<Penalty>`

#### `Manager extends User`
- **Methods:**
  - `viewAllLoans(): List<LoanRecord>`
  - `processReturn(loanId: String, returnDate: LocalDate)`
  - `settlePenalty(penaltyId: String, outcome: PenaltyOutcome)` (`PAID` | `WAIVED`)
  - `addEquipment(...)`, `updateEquipment(...)`, `decommissionEquipment(...)`
  - `resetDemoData()`

#### `Penalty`
- **Attributes:**
  - `penaltyId: String`
  - `loanId: String`
  - `type: PenaltyType` (`LATE_FEE`)
  - `amount: Double` in PHP *(non-negative invariant, INV-4)*
  - `status: PenaltyStatus` (`UNPAID`, `PAID`, `WAIVED`)
  - `assessedDate: LocalDate`
- **Methods:**
  - `markPaid()`, `waive(reason: String)`

### 3.2 Pluggable Late-Fee Rule (Design-Pattern Showcase)

```
                     <<interface>>
               ┌──────────────────────┐
               │     FeeStrategy      │
               │──────────────────────│
               │ + calculate(ctx): Double
               └──────────▲───────────┘
                          │ implements
                ┌─────────┴──────────────┐
                │     LateFeeStrategy    │
                │────────────────────────│
                │ - ratePerDay: Double   │
                │   (default 50.00 PHP)  │
                └────────────────────────┘
```

- `LoanRecord` depends on the `FeeStrategy` **interface**, never on the concrete class (Open/Closed: adding another rule requires no changes to `LoanRecord`).
- Only one rule ships in v3: **₱50/day for every overdue day**. The interface exists so the rule can be swapped or extended without touching entities.
- **Live accrual:** while a loan is out, the UI displays `accruedFee(today)` (growing daily). On return, the amount is frozen into a `Penalty` record.
- The rate is defined once, as a constant (§5 REQ-007).

---

## 4. State Transitions

### 4.1 Equipment Lifecycle

```
 [AVAILABLE] ──(Borrower checks out)──► [LOANED]
      ▲ ▲                                  │
      │ │                                  │
      │ └──────(Returned)──────────────────┘
      │
      ├────(Repaired)──── [UNDER_MAINTENANCE] ◄──(Manager flags item)
      │                          │
      │                   (Beyond repair)
      │                          ▼
      │                  [DECOMMISSIONED]  ← terminal
      │
      └──(Manager decommissions idle item)──► [DECOMMISSIONED]
```

| From | Trigger | To |
| :--- | :--- | :--- |
| `AVAILABLE` | Borrower checks out | `LOANED` |
| `LOANED` | Return logged | `AVAILABLE` |
| `AVAILABLE` / `LOANED` | Manager flags for repair | `UNDER_MAINTENANCE` |
| `UNDER_MAINTENANCE` | Repair completed | `AVAILABLE` |
| `UNDER_MAINTENANCE` | Beyond economic repair | `DECOMMISSIONED` |
| `AVAILABLE` | Manager decommissions idle item | `DECOMMISSIONED` |

*`DECOMMISSIONED` is terminal — no transitions out. `DECOMMISSIONED` items remain in the inventory list for audit history but are never requestable. `UNDER_MAINTENANCE` items are also not borrowable. An item flagged for maintenance while it is `LOANED` keeps that status when its loan is returned: the return closes the loan, but the item stays `UNDER_MAINTENANCE` until a manager completes the repair.*

### 4.2 Loan Lifecycle

```
 [ACTIVE] ──(CurrentDate > DueDate, auto)──► [OVERDUE]
    │                                            │
    └──────────(Return logged)───────────────────┘
                         ▼
                 [RETURNED] (terminal)
```

| From | Trigger | To |
| :--- | :--- | :--- |
| — | Borrower checks out available item | `ACTIVE` |
| `ACTIVE` | Auto-computed when `CurrentDate > DueDate` | `OVERDUE` |
| `ACTIVE` | Return logged | `RETURNED` *(terminal)* |
| `OVERDUE` | Return logged | `RETURNED` *(terminal)* |

---

## 5. Functional Requirements (User Stories)

### 5.1 Catalog & Availability (Borrower & Admin)
- **REQ-001 (Search & Filter):** Users can search gear by name/serial number and filter by category and real-time status.
- **REQ-002 (Availability Lock):** Items marked `LOANED`, `UNDER_MAINTENANCE`, or `DECOMMISSIONED` cannot be checked out (prevents double-booking; enforced per INV-1, §6).

### 5.2 Authentication & Borrowing Workflow
- **REQ-003 (Mock Login):** A login screen accepts the seeded email/password pairs (fixed demo passwords, §9.1). Invalid credentials are rejected with an error message. Logging out returns to the login screen.
- **REQ-004 (Instant Checkout):** A borrower selects an available item and a requested `DueDate`, and the loan is created immediately: item → `LOANED`, loan → `ACTIVE`. Validation: `BorrowDate <= DueDate <= BorrowDate + 14 days`, and the borrower holds at most **3 active loans**. No manager approval step exists.
- **REQ-005 (My Loans):** Borrowers view their own active, overdue, and returned loans, including live accrued and outstanding penalties.

### 5.3 Return & Fee Processing
- **REQ-006 (Return Logging):** A return is finalized by entering the actual return date. The item returns to `AVAILABLE` and the loan becomes `RETURNED`. No condition assessment or damage survey is performed.
- **REQ-007 (Automatic Late Fee):**
  - **Late Fee:** `LATE_FEE_PER_DAY × days overdue`, where `LATE_FEE_PER_DAY = ₱50.00` (default, defined once as a constant and varied only through `LateFeeStrategy`).
  - **Live accrual:** while `ACTIVE`/`OVERDUE`, the dashboard and loan views display the running total so far; on return, the total is frozen.
  - **No grace period:** a return on the due date costs nothing; the first day past it costs ₱50.
  - Fees are never entered by hand. The frozen amount becomes a single `Penalty` (`LATE_FEE`) attached to the `LoanRecord`.

### 5.4 Monitoring, Inventory & Penalties
- **REQ-008 (Overdue Dashboard):** Dedicated Manager view highlighting all loans past their `dueDate` (including those auto-transitioned to status `OVERDUE` per §4.2), sorted by longest overdue first, with the live accrued fee.
- **REQ-009 (All Loans):** Manager view listing every loan (active, overdue, returned) with borrower, item, dates, status, and fee state.
- **REQ-010 (Inventory CRUD):** Managers can add, update, or decommission items from the pool, and flag items for maintenance. When adding or editing an item, the manager may attach one photo (optional; local upload, downscaled and stored per design.md §12).
- **REQ-011 (Penalty Settlement):** Managers can view all penalties attached to a loan and mark each `PAID` or `WAIVED` (with reason). Borrowers see outstanding penalties in "My Loans".
- **REQ-012 (Local Persistence):** All state (equipment, loans, penalties, users) is saved to `kitnapper-data.json` in the application folder on every mutation. On startup, the app loads that file if present; otherwise it seeds the demo dataset (§9). The manager may reset to the seed dataset at any time.

### 5.5 Out-of-Scope Workflow Rules
- Loans cannot be cancelled after checkout.
- Due dates cannot be extended; a loan ends only when returned.

---

## 6. Non-Functional Requirements

### 6.1 Quality Attributes
- **NFR-01 (Usability):** Each primary workflow — checkout, return, settle penalty — is reachable within four clicks from the role's home screen.
- **NFR-02 (Responsiveness):** Catalog, dashboard, and loan tables reflect state changes immediately via Swing `TableModel`/`ListModel` with change listeners; no perceptible lag at seed-data scale.
- **NFR-03 (Platform):** Desktop only — Java 17+ with Swing, built as a NetBeans Ant project.
- **NFR-04 (Code Quality):** Strict View / Controller-Service / Model separation; no business logic in views; all entity attributes private.
- **NFR-05 (Demonstrability):** Seed data must reproduce the Overdue Dashboard scenario immediately on first launch, with no manual setup (see §9).
- **NFR-06 (Maintainability):** Fee rate, category enums, and status enums are each defined in exactly one place; adding a new `FeeStrategy` implementation must not require changes to `LoanRecord` (Open/Closed principle).
- **NFR-07 (Durability):** State survives application restarts; a corrupted or missing save file falls back to the seed dataset without crashing.

### 6.2 Class Invariants (Enforced in the Model Layer)
- **INV-1:** An `Equipment` in status `LOANED`, `UNDER_MAINTENANCE`, or `DECOMMISSIONED` can never be checked out — enforced in the model, not merely hidden in the UI.
- **INV-2:** `borrowDate <= dueDate <= borrowDate + 14 days` on every `LoanRecord`.
- **INV-3:** Only a `Borrower` can create a loan for themselves; only a `Manager` can finalize a return or settle a penalty.
- **INV-4:** All fees are non-negative and always derived from `LateFeeStrategy`, never typed in by hand.
- **INV-5:** State changes happen only through entity methods (`completeReturn()`, `markLoaned()`, `markPaid()`, …) — the UI never mutates entity fields directly.
- **INV-6:** A borrower can hold at most **3** `ACTIVE`/`OVERDUE` loans at any time.

---

## 7. Technical Stack & Architecture Constraints

### 7.1 Stack
1. **Java Track (required):** Java 17+, Swing, built and run as a **NetBeans Ant application project** (matching the course's other activities).

### 7.2 Data Persistence
- **Required:** Local JSON file `kitnapper-data.json` in the application folder, written on every state mutation and read at startup.
- **Constraint:** **No third-party libraries.** The JSON format is written and read using the Java standard library only (no Gson/Jackson or equivalent).
- **Reset:** The Manager's `resetDemoData()` (also exposed via UI) overwrites the save file with the seed dataset. Deleting the file manually has the same effect on next launch.
- **Images:** optional item photos (§3.1 `photoFile`) are JPEG files under `kitnapper-images/` beside the JSON save file (downscaled on import per design.md §12); the JSON stores the relative filename only. Reset demo data clears the folder.

### 7.3 OOP Design Pattern Expectations
- **Abstraction:** `User` and `Equipment` are abstract; the View layer depends only on service interfaces, never on concrete repositories.
- **Encapsulation:** All attributes private with validated accessors/mutators; invariants (§6.2) enforced inside the entities themselves.
- **Inheritance:** `Borrower` / `Manager` extend `User`; optional category specializations (`DigitalEquipment`, `MechanicalTool`) may extend `Equipment`.
- **Polymorphism:** Fee computation via the `FeeStrategy` interface (`LateFeeStrategy`); role-dependent behavior resolved by dynamic dispatch.
- **Separation of Concerns:** Strict isolation between View (Swing frames/panels), Controller/Service, and Data Models.

---

## 8. Scope Boundaries (Guardrails)

### In-Scope (Deliverables)
- [x] Mock login screen with fixed demo passwords (§9.1) and logout.
- [x] Gear catalog with search, filter, and real-time state updates.
- [x] Instant checkout form with validation rules (due-date window, 3-loan cap).
- [x] "My Loans" borrower view (including live accrued and outstanding penalties).
- [x] Equipment return logging (date only; no condition assessment).
- [x] Automatic ₱50/day late fees, live-accrued and frozen on return.
- [x] Penalty settlement by Managers (`PAID` / `WAIVED`).
- [x] Overdue dashboard + All Loans view.
- [x] Inventory CRUD (+ maintenance flag).
- [x] Local JSON persistence with manager-triggered demo reset.
- [x] Optional per-item photo upload (local file, downscaled; §3.1, §7.2).
- [x] Seed data (pre-loaded with at least 10 items, 3 users, 2 active loans, 1 overdue loan).

### Out-of-Scope (Strict Non-Goals)
- [ ] Network client-server communication (No REST APIs / Sockets).
- [ ] Real payment processing/gateways (penalties are recorded and settled in-app only).
- [ ] Real authentication / password hashing / account management.
- [ ] Damage tracking / condition assessment / damage fees.
- [ ] Manual editing of assessed fees (fees are computed only).
- [ ] Loan cancellation or due-date extension.
- [ ] Future calendar reservations (Immediate checkout only).
- [ ] Barcode / QR scanning.
- [ ] Cloud/hosted image storage (photos are local files only; the "image/file uploads" non-goal was reversed on 2026-11-14 — see design.md §12).
- [ ] Push/email notifications.
- [ ] Mobile/Web targets (Desktop only).

---

## 9. Demo Seed Data Requirements

On first application startup (with `T` = the startup date), populate the system state with:

### 9.1 Users (3 total, fixed demo passwords)

| Role | Email | Password | Name |
| :--- | :--- | :--- | :--- |
| Equipment Manager | `admin@institution.edu` | `admin123` | A. Manager |
| Borrower | `student1@institution.edu` | `student123` | Student One |
| Borrower | `student2@institution.edu` | `student123` | Student Two |

### 9.2 Equipment (10 total — replacement values are example seed values in PHP, kept configurable; seed items ship **without photos**, the UI falls back to the category tile per design.md §3.5)

| # | Item | Category | Replacement Value |
| :-- | :--- | :--- | :--- |
| 1 | Sony A7 IV | `CAMERA` | ₱150,000 |
| 2 | Canon EOS R6 | `CAMERA` | ₱145,000 |
| 3 | Sony FE 24-70mm f/2.8 GM | `LENS` | ₱120,000 |
| 4 | Canon RF 24-70mm f/2.8 L | `LENS` | ₱115,000 |
| 5 | MacBook Pro M3 (14") | `LAPTOP` | ₱120,000 |
| 6 | Lenovo ThinkPad P1 Gen 6 | `LAPTOP` | ₱130,000 |
| 7 | Rode Wireless PRO kit | `AUDIO` | ₱28,000 |
| 8 | Zoom H6 recorder | `AUDIO` | ₱20,000 |
| 9 | Manfrotto 504X fluid-head tripod | `TOOL` | ₱35,000 |
| 10 | DeWalt 20V cordless drill kit | `TOOL` | ₱9,000 |

### 9.3 Pre-existing Loans (3 total: 2 ACTIVE, 1 OVERDUE)

| Loan | Borrower | Item | Borrowed | Due | Status | Demo Purpose |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| L-001 | student1 | Sony A7 IV | T − 2 days | T + 5 days | `ACTIVE` | Tests "within due date" view |
| L-002 | student2 | MacBook Pro M3 | T − 6 days | T + 1 day | `ACTIVE` | Tests due-soon sorting on dashboard |
| L-003 | student1 | Rode Wireless PRO | T − 11 days | T − 4 days | `OVERDUE` | 4 days late → ₱200.00 accrued; tests Overdue Dashboard + fee engine |

*Optional demo aid: one additional `ACTIVE` loan may be seeded if the demo script needs another row; it is in addition to, not part of, the required seed set.*

### 9.4 Expected State on First Launch
- Overdue Dashboard shows exactly **1 row** (L-003) with **₱200.00** accrued live.
- Catalog shows **7 `AVAILABLE`** items and **3 `LOANED`** items (Sony A7 IV, MacBook Pro M3, Rode Wireless PRO).
- No `Penalty` records exist yet; L-003's fee appears live and is frozen into a penalty only when the loan is returned.

---

## Appendix A: Revision History

| Version | Date | Changes |
| :--- | :--- | :--- |
| 1.0.0 | — | Initial draft |
| 1.1.0 | 2026-11-11 | Defined `Penalty` entity + settlement flow (REQ-009); reworked `User` into an abstract base with `Borrower`/`Manager` subclasses; fixed equipment state diagram (approval arrow, `DECOMMISSIONED` terminal state); reconciled seed-data counts (10 items; 2 `ACTIVE` + 1 `OVERDUE` loans); added Non-Functional Requirements and class invariants; added concrete fee tables; restricted stretch goals to persistence only (real authentication removed) |
| 2.0.0 | 2026-11-11 | Instant checkout — removed manager approval queue, `PENDING`/`PENDING_APPROVAL` states and the approve/reject flow; removed damage tracking, condition assessment, damage fees and maintenance-on-return routing; all amounts converted to Philippine pesos with a ₱50/day late fee (seed late fee now ₱200); local JSON persistence promoted to required scope (with manager-triggered demo reset); Swift track dropped (Java/JavaFX only); fee-rule demo retained as a pluggable single-rule `FeeStrategy` |
| 2.0.1 | 2026-11-11 | Third-party libraries disallowed — JSON save/load implemented with the Java standard library; click budget relaxed from two to four |
| 3.0.0 | 2026-11-11 | UI toolkit switched from JavaFX to **Swing** (NetBeans Ant project, matching course activities); added mock login with fixed demo passwords (REQ-003); live fee accrual frozen into a penalty on return; added borrow limits (3 active loans, 14-day window) with invariants; added All Loans manager view (REQ-009); save file pinned to `kitnapper-data.json` in the app folder; cancellation/extension explicitly out of scope |
| 3.1.0 | 2026-11-14 | Windows 11 / Fluent visual direction (design.md v2.0); image uploads moved into scope — optional per-item `photoFile` (§3.1), stored under `kitnapper-images/` (§7.2), seed ships without photos; JSON persistence unchanged (`schemaVersion` stays 1, field is nullable/additive) |
| 3.1.1 | 2026-11-14 | Clarified §4.1: a return of an item flagged `UNDER_MAINTENANCE` while loaned closes the loan but leaves the item `UNDER_MAINTENANCE`; no scope change |
