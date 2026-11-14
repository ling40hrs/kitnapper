# Handoff — M1 Execution (KitNapper)

> **Working document for the next agent session — not a course deliverable.**
> Scope: execute milestone **M1** of `docs/implementation-plan.md` §4 (JSON persistence +
> seed data). Read `AGENTS.md`, then this file, then `docs/implementation-plan.md` §3–§5.
> Delete this file (or keep for traceability) after M1 lands.

## 1. Current state (as of 2026-11-14)

- Repo `https://github.com/ling40hrs/kitnapper`, branch `main`, clean, in sync at `f825c63`.
- **M0 complete and pushed** (`7b0a178`): 18 model classes in
  `desktop-app/src/kitnapper/model/` — all invariants INV-1/2/4/5 enforced, `ant clean jar`
  builds, smoke-checked.
- Post-M0 findings folded into the plan (1.3.0, decisions D17–D23).
- `Main.java` is still a 0-byte placeholder; nothing launches yet. That is correct for M1.
- **Git rule override in effect:** the user has granted standing approval — commit and push
  freely without asking. Still keep: conventional commit types, subject ≤ 50 chars, body
  ≤ 72 cols, **both co-author trailers**, SSH signing (already configured), never commit
  `build/`, `dist/`, `nbproject/private/`, `*.class`, `kitnapper-data.json`, or
  `kitnapper-images/`.

## 2. M1 scope (implementation-plan §4, row M1)

**Build:** `kitnapper.service.persistence` + `AppData`, `DataStore`, `DataStoreImpl`,
`LoanService` skeleton interface. A temporary round-trip `main` is sanctioned scratch code
and **must be deleted before the M1 commit**.

| File | Package | Contents | Est. lines |
| :--- | :--- | :--- | --: |
| `AppData.java` | `kitnapper.service` | aggregate: four live `List`s (users, equipment, loans, penalties); services mutate, `save()` persists (D17) | 50 |
| `DataStore.java` | `kitnapper.service` | interface: `load()`, `save()`, `resetToSeed()` + read accessors over the aggregate (D17) | 30 |
| `DataStoreImpl.java` | `kitnapper.service` | load → seed fallback → save-on-mutation orchestration (D18) | 80 |
| `LoanService.java` | `kitnapper.service` | skeleton interface only — signatures per plan §2.2 (borrower/date derive from session/`LocalDate.now()`, `accruedFee(loanId)` for D21) | 30 |
| `JsonParser.java` | `kitnapper.service.persistence` | recursive-descent parser for **only the subset `JsonWriter` emits** (plan §3.2) | 120 |
| `JsonWriter.java` | same | pretty-printed 2-space, escapes `\" \\ \n`, numbers as plain decimals | 80 |
| `Json.java` | same | helpers over `Map<String,Object>`, `List<Object>`, `String`, `Double`, `Boolean`, `null` | 60 |
| `UserMapper.java` | same | `User` ⇄ JSON; `role` selects `Borrower`/`Manager` | 70 |
| `EquipmentMapper.java` | same | all three kinds ⇄ JSON; restore via transition replay; per-kind fields (D14/D19) | 90 |
| `LoanMapper.java` | same | `LoanRecord` ⇄ JSON; `returnDate` null when absent; never re-serialize `OVERDUE` | 70 |
| `PenaltyMapper.java` | same | `Penalty` ⇄ JSON; `waiveReason` null unless waived | 60 |
| `RootMapper.java` | same | document ⇄ `AppData`: four arrays + `schemaVersion` + `savedAt` | 80 |
| `SeedData.java` | same | PRD §9 dataset relative to the date seeding runs (D4) | 130 |
| *(scratch)* round-trip `main` | same | temporary; deleted before commit | — |

Every file ≤ 200 lines (target 100–150), 4-space indent, LF, UTF-8. No third-party
libraries — JDK only. Total after M1 ≈ 70 files (statement in plan §2.6).

## 3. M0 model API the mappers must use (exact)

- **Users:** `protected User(userId, fullName, email, password)`; public
  `Borrower(...)` / `Manager(...)`; immutable; `getRole()` → `UserRole`.
- **Equipment:** `protected Equipment(id, name, category, serialNumber, replacementValue)`
  starts `AVAILABLE`. Public ctors: `DigitalEquipment(..., String warrantyContact)`,
  `MechanicalTool(..., int maintenanceIntervalDays)`, `GeneralEquipment(...)`.
  **Restore recipe:** construct the subclass at `AVAILABLE`, then replay the persisted
  status — `markLoaned()` / `markUnderMaintenance()` / `decommission()` (all legal from
  `AVAILABLE`). Then `setPhotoFile(...)`. No backdoor constructor needed.
- **LoanRecord:** only `LoanRecord.checkout(loanId, borrowerId, equipmentId, borrowDate,
  dueDate)` (throws IAE if `due < borrow` or window > 14 days). Restore recipe:
  `checkout(...)` then `completeReturn(returnDate)` when a return date exists.
  `getReturnDate()` → `Optional<LocalDate>`; `getLoanStatus()` → `ACTIVE`/`RETURNED` only.
- **Penalty:** public ctor `(penaltyId, loanId, type, amount, assessedDate)` → `UNPAID`.
  Restore recipe: construct, then `markPaid()` or `waive(reason)` per persisted status.
- **Optional getters:** `getReturnDate()`, `getPhotoFile()`, `getWaiveReason()` return
  `Optional<T>` — mappers write `orElse(null)`.
- **Exception policy (D16):** NPE for null args, IAE for invalid values, ISE for illegal
  transitions. The loader must catch these as corruption (see §5).

## 4. JSON spec highlights (plan §3 — full source of truth)

- File: `kitnapper-data.json` in the **application folder**; `schemaVersion: 1`,
  `savedAt` = ISO date of the save.
- **Per-kind fields:** `warrantyContact` written only for `DIGITAL`;
  `maintenanceIntervalDays` only for `MECHANICAL`; `BASE` writes neither. Parser yields
  numbers as `double` → validate `maintenanceIntervalDays` is integral before converting.
- `loanStatus` persists only `ACTIVE`/`RETURNED`; a persisted `OVERDUE` normalizes to
  `ACTIVE`; unknown `kind`/`role`/enum values = corruption → seed fallback.
- Save discipline: serialize to `.tmp`, then move over the target. Try `ATOMIC_MOVE`,
  fall back to `REPLACE_EXISTING` on Windows (`AtomicMoveNotSupportedException`).

## 5. Load/seed rules (D18) — the core of M1 correctness

`load()` falls back to a fresh seed (and saves immediately) on: missing file, parse error,
unknown `schemaVersion`, unknown enum/`kind`/`role`, **or any model failure during
mapping** (INV-2 violation, `WAIVED` penalty without reason, NPE), plus referential
breakage: loan → missing borrower/equipment, penalty → missing loan, two penalties on one
loan. Build into local structures and swap into `AppData` only when the whole document is
valid. **Never crash** (NFR-07).

## 6. Seed data (PRD §9)

- Users: `u-admin` `admin@institution.edu`/`admin123` (MANAGER, "A. Manager");
  `u-student1`/`u-student2` `student123` (BORROWER, "Student One"/"Student Two").
- Equipment `e-001`…`e-010` in PRD order (Sony A7 IV → DeWalt drill) with replacement
  values per PRD §9.2; serials free-form (suggest `SN-XXX-001…`); kinds by category:
  CAMERA/LENS/LAPTOP/AUDIO → `DIGITAL` (`warrantyContact` null), TOOL → `MECHANICAL`
  (`maintenanceIntervalDays` 0). **No `BASE` item in the seed** (D19).
- Loans (T = seeding date): `L-001` student1 + e-001, T−2 → T+5, ACTIVE;
  `L-002` student2 + e-005, T−6 → T+1, ACTIVE; `L-003` student1 + e-007, T−11 → T−4,
  stored ACTIVE (derives OVERDUE). Mark the three items `markLoaned()`. No penalties.
- Expected first-launch state: 7 AVAILABLE / 3 LOANED; overdue dashboard would show
  exactly L-003 with ₱200.00 accrued (verified live in M4).

## 7. Verification (definition of done)

1. Scratch `main` writes the seed (+ one extra `GeneralEquipment` BASE item), re-reads it,
   and the round-trip matches on every field. The fixture must also exercise a `RETURNED`
   loan, a `WAIVED` penalty with reason, and an `UNDER_MAINTENANCE` item (every restore
   path, D14/D18).
2. Corrupt-file checks by hand: truncated JSON, unknown `schemaVersion`, unknown `kind`
   and `role`, INV-2-violating loan, penalty referencing a missing loan, two penalties on
   one loan — each seeds fresh, never crashes.
3. `ant clean jar` (from `desktop-app/`) still BUILD SUCCESSFUL.
4. Line-count audit: all new files ≤ 200 lines.
5. **Delete the scratch `main`**, rebuild, confirm the jar still builds and contains no
   scratch class.
6. Commit + push (message below). M1 may be its own commit — the "M0+M1 together" option
   is moot since M0 already landed.

## 8. Commit message (ready to use)

```text
feat(persistence): add JSON persistence and seed data

Add the M1 persistence layer: hand-written JSON parser/writer with
no third-party libraries, entity mappers for all three equipment
kinds, the AppData aggregate behind DataStore, the PRD 9 seed
dataset computed relative to the seeding date, and the LoanService
skeleton interface.

- Load falls back to a fresh seed on missing, unparseable, or
  semantically corrupt files (NFR-07, D18), including model
  validation and referential failures
- Save serializes to a tmp file and atomically moves it over the
  target, with a Windows replace-existing fallback
- loanStatus persists only ACTIVE/RETURNED; OVERDUE stays derived (D1)

Co-authored-by: Aerovic K. Cudia <253015735+aerovickiesurcudia@users.noreply.github.com>
Co-authored-by: Chrisleen Deinese L. Villacorza <230848138+Leen-dotcom@users.noreply.github.com>
```

## 9. Do NOT in M1

- No Swing/UI, no `Main` changes, no controllers.
- No service implementations beyond the `LoanService` interface skeleton.
- No checkout/return logic, no INV-6 enforcement yet (that is M3+).
- No schema changes, no `schemaVersion` bump, no test framework.
- Do not commit the scratch `main` or any runtime save file.

## 10. After M1

Write `design.md` §13 (skeleton loading states) **with the user** before starting M2
(shell + login) — it is a documented blocker. Then M2.
