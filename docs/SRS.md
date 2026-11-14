---
project_name: "KitNapper"
course: "CS203 – Object-Oriented Programming"
artifact_type: "Software Requirements Specification (SRS)"
derived_from: "PRD.md v3.1.0"
version: "3.1.0"
---

# Software Requirements Specification — KitNapper

## 1. Introduction

### 1.1 Purpose
This document specifies the functional and non-functional requirements for **KitNapper**, a desktop asset-lending management system. It refines the product vision in `PRD.md` into verifiable requirements with priorities and acceptance criteria.

### 1.2 Scope
KitNapper replaces physical equipment logbooks for institutions (universities, studios, IT departments). It tracks asset availability, borrow/return lifecycles, and late fees via a Swing GUI. Borrowing is immediate (no approval step), returns involve no damage assessment, and all money is in Philippine pesos. Out of scope: networking, real authentication, payment processing, damage tracking, loan cancellation/extension, calendar reservations (see PRD §8).

### 1.3 Definitions
| Term | Definition |
| :--- | :--- |
| Asset / Equipment | Any lendable item (camera, lens, laptop, audio gear, tool) |
| LoanRecord | A single borrow transaction from checkout to return |
| Penalty | A late fee attached to a LoanRecord |
| Accrued fee | The live, not-yet-frozen late-fee total for an outstanding loan |
| Category tile | The placeholder image block shown when an item has no readable photo (design.md §3.5) |
| T | Application startup date (seed-data reference point) |
| ₱ | Philippine peso |

## 2. Overall Description

### 2.1 Actors
- **Borrower** (`ROLE_BORROWER`): browses catalog, checks out available items, views own loans and penalties.
- **Equipment Manager** (`ROLE_ADMIN`): views all loans, logs returns, settles penalties, manages inventory, monitors the overdue dashboard, resets demo data.

Authentication is a **mock login screen** with fixed demo passwords (PRD §9.1); real account verification is a strict non-goal.

### 2.2 Constraints
- C-1: Desktop only — Java 17+ / **Swing**, built as a **NetBeans Ant application project**.
- C-2: Local JSON file persistence (`kitnapper-data.json` in the app folder; survives restarts; manager-triggered reset).
- C-3: Strict MVC layering; no business logic in views; all entity attributes private.
- C-4: The four OOP pillars must be demonstrable: `User`/`Equipment` abstraction, `Borrower`/`Manager` inheritance, `FeeStrategy` polymorphism, encapsulated invariants.
- C-5: **No third-party libraries.** The application, the JSON save format, and all verification are implemented with the Java standard library and Swing only.

### 2.3 Dependencies
- JDK 17 (Swing is part of the JDK — no JavaFX SDK needed).
- NetBeans (Ant project support) for development.
- Seed dataset (PRD §9) required on first launch or after reset.

## 3. Functional Requirements

Priority scale: **H** = must-have, **M** = should-have, **L** = nice-to-have. All features below are must-have.

### 3.1 Catalog (FR-01 … FR-02)

| ID | Requirement | Priority |
| :--- | :--- | :--- |
| FR-01 | Search gear by name or serial number; filter by category and real-time status. | H |
| FR-02 | Items in `LOANED`, `UNDER_MAINTENANCE`, or `DECOMMISSIONED` status are not selectable for checkout (INV-1; enforced in the model, not the UI). | H |

**Acceptance (FR-01):** search narrows results on keystroke; filters compose; results reflect the latest status without manual refresh.

### 3.2 Authentication & Borrowing (FR-03 … FR-05)

| ID | Requirement | Priority |
| :--- | :--- | :--- |
| FR-03 | Login accepts only the seeded email/password pairs (`admin123`, `student123`); invalid credentials show an inline error. Logout returns to the login screen. | H |
| FR-04 | Borrower checks out an available item by choosing a due date. The loan is created immediately; the item becomes `LOANED`. Validation: `borrowDate <= dueDate <= borrowDate + 14 days` and at most **3 active/overdue loans** per borrower. | H |
| FR-05 | Borrower views "My Loans" with active, overdue, and returned loans plus live accrued and outstanding penalties. | H |

**Acceptance (FR-04):** attempting a 4th simultaneous loan, or a due date 15+ days out, is rejected with a clear message and the item stays `AVAILABLE`.

### 3.3 Return & Fees (FR-06 … FR-07)

| ID | Requirement | Priority |
| :--- | :--- | :--- |
| FR-06 | Return logging: a manager records the actual return date; the loan becomes `RETURNED` and the item returns to `AVAILABLE`. No condition assessment is performed. | H |
| FR-07 | Late fees computed exclusively via `FeeStrategy`: `₱50.00 × days overdue`, displayed live while the loan is outstanding and frozen into a single `LATE_FEE` penalty on return. No grace period; fees are never hand-editable. | H |

**Acceptance (FR-07):** an outstanding loan 4 days late displays ₱200.00 live; returning it creates exactly one ₱200.00 `LATE_FEE` penalty. A loan returned on its due date yields no penalty.

### 3.4 Monitoring, Inventory, Penalties, Persistence (FR-08 … FR-12)

| ID | Requirement | Priority |
| :--- | :--- | :--- |
| FR-08 | Overdue dashboard: all loans past their `dueDate` (including auto-`OVERDUE` transitions), sorted by longest overdue first, with live accrued fees. | H |
| FR-09 | All Loans view: every loan (active, overdue, returned) with borrower, item, dates, status, and fee state. | H |
| FR-10 | Inventory CRUD: managers add, update, and decommission items, and flag items for maintenance. `DECOMMISSIONED` is terminal; decommissioned items remain visible for audit but are not borrowable. When adding or editing an item, the manager may attach one local photo (≤ 20 MB source, downscaled to ≤ 1024 px JPEG per design.md §12); a missing or corrupt photo file falls back to the category tile without errors. | H |
| FR-11 | Penalty settlement: managers view all penalties per loan and mark each `PAID` or `WAIVED` (with reason). Borrowers see outstanding penalties in "My Loans". | H |
| FR-12 | Persistence: every state mutation is written to `kitnapper-data.json`; startup loads the file if present, otherwise seeds the demo dataset. Managers can reset to seed data. | H |

**Acceptance (FR-10):** adding an item with a photo shows the photo on the card/preview immediately; restarting keeps it; a corrupted or missing photo file shows the category tile instead; reset demo data removes uploaded photos (NFR-08).

**Acceptance (FR-12):** create a loan, close and reopen the app — the loan is still present. Trigger reset — the dataset returns to the PRD §9 baseline.

## 4. Non-Functional Requirements

| ID | Requirement | Metric / Verification |
| :--- | :--- | :--- |
| NFR-01 | Reachability: each primary workflow (checkout, return, settle penalty) is ≤ 4 clicks from the role's home screen. | Manual UI walkthrough |
| NFR-02 | Responsiveness: catalog, dashboard, and loan tables update immediately after mutations via Swing `TableModel`/`ListModel` change listeners. | Manual UI walkthrough |
| NFR-03 | Platform: Java 17+ / Swing, NetBeans Ant project. | Build & run on target |
| NFR-04 | Code quality: strict View / Controller-Service / Model separation; no business logic in views. | Code review |
| NFR-05 | Demonstrability: seed data reproduces the Overdue Dashboard scenario at startup with zero manual setup. | Startup inspection (see §6) |
| NFR-06 | Maintainability: fee rate and enums defined exactly once; adding a `FeeStrategy` requires no changes to `LoanRecord`. | Code review (Open/Closed) |
| NFR-07 | Durability: state survives restarts; a missing or corrupted save file falls back to seed data without crashing. | Delete/corrupt file test |
| NFR-08 | Image storage: photos are local files under `kitnapper-images/` (never networked), downscaled to ≤ 1024 px JPEG; a missing or corrupt image falls back to the category tile without crashing; reset demo data clears the folder. | Manual walkthrough + corrupt-file test |

## 5. Class Invariants (Model-Layer Enforced)

| ID | Invariant |
| :--- | :--- |
| INV-1 | Equipment in `LOANED` / `UNDER_MAINTENANCE` / `DECOMMISSIONED` can never be checked out. |
| INV-2 | `borrowDate <= dueDate <= borrowDate + 14 days` on every `LoanRecord`. |
| INV-3 | Only a `Borrower` can create a loan for themselves; only a `Manager` can finalize a return or settle a penalty. |
| INV-4 | All fees non-negative and always derived from `LateFeeStrategy`, never hand-entered. |
| INV-5 | State changes occur only through entity methods; the UI never mutates entity fields directly. |
| INV-6 | A borrower can hold at most 3 active/overdue loans at any time. |

## 6. Verification & Demo Criteria

Testing approach: **manual only.** No automated test suite is required; every requirement is verified by the demo walkthrough below plus the startup checks.

### 6.1 First-Launch Checks

On first launch (or after demo reset) with `T` = startup date, the system must show:
1. Overdue Dashboard with exactly **1 row** (loan L-003) and **₱200.00** accrued live.
2. Catalog with **7 AVAILABLE** and **3 LOANED** items (Sony A7 IV, MacBook Pro M3, Rode Wireless PRO).
3. No frozen `Penalty` records yet — L-003's fee is still accruing.

### 6.2 Demo Walkthrough

Run in order; each step lists the requirement it proves.

1. **Launch** the app → login screen appears (FR-03).
2. **Enter a wrong password** → inline error; the app does not log in (FR-03).
3. **Log in as `student1@institution.edu` / `student123`** → catalog appears with search and filters; type a gear name to filter (FR-01, FR-03).
4. **Check out an AVAILABLE item** (e.g., Canon EOS R6), due date `T + 3 days` → item immediately shows `LOANED` (FR-04).
5. **Confirm the lock**: the same item is no longer selectable (FR-02).
6. **Try a due date 15+ days out** → rejected with a validation message (FR-04, INV-2).
7. **Close and relaunch, then log in again** → the new loan is still present (FR-12, NFR-07).
8. **Log in as manager** → Overdue Dashboard shows L-003 first with **₱200.00 accrued** (FR-08); All Loans lists every loan (FR-09).
9. **Log the return of L-003** with return date `T` → loan becomes `RETURNED`; exactly one ₱200.00 `LATE_FEE` penalty is created; item returns to `AVAILABLE` (FR-06, FR-07).
10. **Open the penalty and mark it WAIVED** with a reason → status updates immediately (FR-11, NFR-02).
11. **Add a new inventory item with a photo, then decommission it** → the photo shows on the card/preview; the retired item remains listed but cannot be checked out (FR-10, NFR-08).
12. **Trigger "Reset demo data"** → state returns to the §6.1 baseline (FR-12).

Seed dataset details: PRD §9; rationale and file placement: `seed-data.md`.

## Appendix A: Requirements Traceability

| SRS | PRD | Feature |
| :--- | :--- | :--- |
| FR-01 | REQ-001 | Search & filter |
| FR-02 | REQ-002 | Availability lock |
| FR-03 | REQ-003 | Mock login |
| FR-04 | REQ-004 | Instant checkout + limits |
| FR-05 | REQ-005 | My Loans |
| FR-06 | REQ-006 | Return logging |
| FR-07 | REQ-007 | Automatic late fee |
| FR-08 | REQ-008 | Overdue dashboard |
| FR-09 | REQ-009 | All Loans |
| FR-10 | REQ-010 | Inventory CRUD |
| FR-11 | REQ-011 | Penalty settlement |
| FR-12 | REQ-012 | Local persistence |

## Appendix B: Revision History

| Version | Date | Changes |
| :--- | :--- | :--- |
| 1.0.0 | 2026-11-11 | Initial SRS derived from PRD.md v1.1.0 |
| 2.0.0 | 2026-11-11 | Regenerated for PRD v2.0.0: instant checkout, no damage tracking, PHP/₱50-per-day fees, required JSON persistence, Java-only stack |
| 2.1.0 | 2026-11-11 | Inventory CRUD promoted to must-have; click budget relaxed to ≤ 4; third-party libraries disallowed (hand-written JSON); automated tests removed in favour of manual verification; added full demo walkthrough and PRD traceability table |
| 3.0.0 | 2026-11-11 | Swing + NetBeans Ant (replacing JavaFX); fixed-password mock login (FR-03); live accrual + freeze-on-return (FR-07); borrow limits (FR-04, INV-2, INV-6); All Loans view (FR-09); save file pinned to app folder; cancellation/extension explicitly out of scope |
| 3.1.0 | 2026-11-14 | Windows 11 / Fluent direction (design.md v2.0); photo upload added to inventory scope (FR-10 + acceptance), image-storage NFR-08, walkthrough step 11 covers photo + decommission, category-tile definition in §1.3 |
