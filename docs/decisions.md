# KitNapper — Decision Log

> Single source of truth for every decision made during the requirements interviews and repository audit. When this file and a design guess conflict, this file wins. Implementation progress and milestone status live in §9.

## 1. Project Identity

| Item | Decision |
| :--- | :--- |
| Product | **KitNapper** — desktop equipment-lending tracker |
| Problem | Replace physical sign-out logbooks for schools/studios |
| Team | **YAPPERS** (group project): Casiano, Cudia, Villacorza |
| Course | CS203 – Object-Oriented Programming |
| Current phase | Implementation — M4 delivered (returns, fees, manager views); M5 (inventory, photos, reset) next |

## 2. Product Decisions (from PRD interview)

| Topic | Decision |
| :--- | :--- |
| Core concept | A digital sign-out sheet for expensive gear — approved |
| Users | Two roles: **Borrower** (student) and **Equipment Manager** (staff) |
| Login | Real-looking login screen, but mock: seeded accounts + **fixed demo passwords**; no real authentication |
| Borrowing | **Instant checkout** — no manager approval, no pending state |
| Returns | Logged by the manager with a return date only; **no damage tracking**, no condition assessment |
| Fees | **Late fees only**, in **Philippine pesos**: ₱50/day, no grace period |
| Fee display | **Live accrual** while the loan is out; frozen into one `LATE_FEE` penalty on return |
| Overdue dashboard | Kept — manager-only, sorted by longest overdue first; shows live accrued fees |
| Manager loan view | Added **All Loans** view in addition to the overdue dashboard |
| Inventory | Manager can **add, edit, retire** gear; retired items stay visible for audit but cannot be borrowed |
| Cancellation / extension | **Not supported** — a loan ends only when returned |
| Borrow limits | **Max 3 active loans per borrower**, due date at most **14 days** after checkout |
| Persistence | **Required**, not a stretch goal: local JSON file |
| Save location | `kitnapper-data.json` in the **application folder**; deleting it or using "Reset demo data" restores the seed state |
| Platform | **Java 17+ / Swing** (JavaFX dropped) |
| Build | **NetBeans Ant application project**, matching Activity 7 |
| Libraries | **None** — JDK only; JSON and the Win11/Fluent theme are hand-written (system fonts are probed, Inter is bundled as an asset) |
| Tests | **None automated** — verification is a manual demo walkthrough |
| Click budget | Primary workflows reachable within **4 clicks** |
| OOP showcase | Pluggable `FeeStrategy` interface with `LateFeeStrategy` (even though only one rule ships) |

### Fixed demo accounts

| Role | Email | Password |
| :--- | :--- | :--- |
| Manager | `admin@institution.edu` | `admin123` |
| Borrower | `student1@institution.edu` | `student123` |
| Borrower | `student2@institution.edu` | `student123` |

### Seeded scenario (relative to startup date `T`)

- 10 equipment items (PHP replacement values), 3 users.
- Loans: L-001 active (Sony A7 IV), L-002 active (MacBook Pro M3), L-003 overdue by 4 days (Rode Wireless PRO).
- On first launch: overdue dashboard has exactly **1 row** showing **₱200.00** live-accrued; catalog shows **7 available / 3 loaned**.

## 3. Gaps Found During the Repository Audit

| Gap | Resolution |
| :--- | :--- |
| Course uses Swing, docs said JavaFX | Switched docs and future implementation to **Swing** |
| No project existed — only docs | Scaffolding deferred; see §9 |
| Build system unspecified | **NetBeans Ant** |
| Login behaviour undefined | Fixed demo passwords, invalid credentials rejected |
| Fee timing contradiction (accrued vs. created on return) | **Live accrual, frozen on return** |
| Day-counting rule undefined | **No grace period** |
| Borrow limits undefined | **3 active loans / 14-day window** |
| Manager visibility limited | **All Loans** view added |
| Cancel/extend unspecified | **Out of scope** |
| Save-file location undefined | **App folder**, `kitnapper-data.json` |
| UI design unspecified | Dark dashboard chosen, then **superseded 2026-11-14 by the Windows 11 / Fluent direction** (`design.md` v2.0, §14.2) |
| `OOP/AGENTS.md` contained wrong Next.js rules | **Rewritten** for Java/Swing + git rules |
| No `.gitignore` for NetBeans artifacts | **Added** at `OOP/.gitignore` |
| Slides not planned | Deferred to the very end, generated with the PDF skill |

## 4. Documentation Status

| File | Version | Contents |
| :--- | :--- | :--- |
| `docs/PRD.md` | 3.1.1 | Product requirements, domain model, state machines, seed data, optional per-item photo; clarified maintenance-flag return behavior |
| `docs/SRS.md` | 3.1.5 | Verifiable requirements, invariants, demo walkthrough, traceability, image-storage NFR; FR-06 return-picker acceptance; FR-11 settlement acceptance; §6.2 steps 8–10 aligned with the delivered M4 screens; §6.2 step 6 notes that the due-date picker makes an out-of-window date unreachable and the probe proves the rejection |
| `docs/design.md` | 2.0.0 | Windows 11 / Fluent direction (Files app): neutral ramp + amber accent, tokens, chrome/shell, components, photo uploads; §13 skeleton loading states resolved (150/300/167 ms, flat pulse, gated queries) |
| `docs/seed-data.md` | — | Points to PRD §9; save-file/reset behaviour |
| `docs/preferences.md` | — | Tracked, machine-independent working preferences (planning, one phase per turn, commit policy, verification) |
| `docs/decisions.md` | — | This file |
| `docs/implementation-plan.md` | 1.9.0 | Class/file inventory, JSON persistence spec, milestones M0–M6, implementation decisions D1–D31 (written 2026-11-14 before any code; 1.2.0 adds D14–D16 from M0; 1.3.0 adds D17–D23 post-M0 findings; 1.4.0 adds the skeleton inventory from design §13; 1.6.0 defers photos/skeletons to M5 (D27); 1.7.0 rebuilds the probe harness; 1.8.0 records M3 as delivered with D24–D26 and the actual 85-file inventory; 1.9.0 records M4 delivered, inventory 117 files / 10,788 lines, decisions D28–D31) |

## 5. Design Direction

**v2.0 rewrite 2026-11-14** — the visual direction is now **Windows 11 / Fluent** (Files
app as primary reference), superseding the warm dark-dashboard pass of the same date
(kept as history in `design.md` §14.1). What stands now:

- **Neutral Win11 surface ramp** (`#1F1F1F` app, `#272727` surfaces, `#323232` controls) with the **amber accent `#F0A32E` retained** as the single brand color; square outer window corners.
- **Typography:** Segoe UI Variable → Segoe UI → bundled Inter (fallback), Segoe Fluent Icons → MDL2 → Unicode glyph fallbacks.
- **Shell:** custom title bar (**no tabs**) + omnibar row (breadcrumb, commands, search, layout toggle), grouped sidebar, status bar with counts/selection/aggregate.
- **Managers get a read-only Catalog** (2026-11-16, D24): manager nav is Dashboard, Catalog, All Loans, Overdue, Penalties, Inventory; the catalog is the same screen with checkout withheld (INV-3), shown as an explicit caption rather than a missing control.
- **Catalog + Inventory default to details table + preview pane** (overrides the v1 "defaults to cards" answer); the layout toggle switches the left pane to cards.
- **Status badges:** amber-tinted pill + semantic dot + word (never color alone; wrench glyph on `UNDER_MAINTENANCE`).
- **Gradients are restricted to the single photo-fallback category tile** — every other surface is flat; no Mica blur, no glows, no shadows.
- **Image uploads are in scope** (explicit override of the PRD §8 non-goal): optional per-item photo, stored locally under `kitnapper-images/`, downscaled to ≤ 1024 px JPEG, 20 MB source cap, reset demo data wipes the folder, seed ships without photos.
- **Skeleton loading states are specified** (`design.md` §13, resolved 2026-11-14): 150 ms delay / 300 ms minimum / 167 ms fade, flat 4 px blocks + 1,000 ms pulse, no launch skeleton, query skeletons gated behind a measured ≥ 150 ms, 10 s watchdog; first use in M3.
- Microcopy still mixed by role (plain for borrowers, formal for manager surfaces); dashboard still KPIs + overdue list + recent activity; 1080p target with a 1366×768 sanity check; dark + light themes; motion retuned to Fluent durations (83/167/250 ms).
- Hard constraint unchanged: **no third-party libraries** → custom-painted Swing; system fonts (Segoe UI Variable, Segoe Fluent Icons) are probed, never bundled; the bundled Inter files remain font assets, not libraries.

## 6. Git & Attribution

**Authors**

- Author on every commit: `Ling Chen <lingling40hourspno@gmail.com>`
- Co-authors on every commit (GitHub noreply addresses):
  - `Aerovic K. Cudia <253015735+aerovickiesurcudia@users.noreply.github.com>`
  - `Chrisleen Deinese L. Villacorza <230848138+Leen-dotcom@users.noreply.github.com>`

**Group members**

- [Casiano, Lloyd Orlan A.](https://github.com/ling40hrs/) — the account used for pushes
- [Cudia, Aerovic K.](https://github.com/aerovickiesurcudia)
- [Villacorza, Chrisleen Deinese L.](https://github.com/Leen-dotcom)

**Rules**

- Remote: `https://github.com/ling40hrs/kitnapper`, branch `main`.
- Push with the authenticated `gh` account (`ling40hrs`).
- Commits stay **SSH-signed** (`commit.gpgsign=true`, `gpg.format=ssh`, `~/.ssh/id_ed25519.pub`) so GitHub shows the verified badge; never bypass signing.
- Conventional commit types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `style`.
- **Explicit approval is required before every git write operation** — stage, commit, push, tag, branch, revert.
- Never commit NetBeans build output (`build/`, `dist/`, `nbproject/private/`, `*.class`).

**Windows build machine setup (2026-11-14)**

The attribution rules above now run on the Windows machine, not just the original push machine:

- **GitHub CLI 2.104.0** installed (`C:\Program Files\GitHub CLI\gh.exe`), authenticated as `ling40hrs`; `gh auth setup-git` links it to Git pushes.
- **SSH signing** enabled: ed25519 key generated at `C:\Users\CIIT\.ssh\id_ed25519`, uploaded to the `ling40hrs` account as a **signing key** (title: "KitNapper build machine"). Repo-local config: `commit.gpgsign=true`, `gpg.format=ssh`, `user.signingkey` pointing at the public key — commits get the GitHub **Verified** badge.
- **Commit template** `C:\Users\CIIT\.gitmessage-kitnapper` (repo-local `commit.template`) pre-fills the two co-author trailers above. Note: `git commit -m` bypasses the template, so trailers must be added manually in that case.
- The stray `aercn <aercn222@gmail.com>` identity that briefly existed in this machine's global git config was removed; the repo-local identity is the author above.
- **History note:** two commits pushed 2026-11-11–14 under the wrong identity (`aercn`) were reverted from `main` and re-applied with correct attribution as `76e4739` (docs) and `1e98fbb` (fonts). No `aercn`-authored commits remain on the remote.

## 7. Deliverables

- **Required:** working desktop app + the documentation in `Finals/docs/` (on the current checkout that folder is the repository root's `docs/` — the `Finals/` prefix exists only on the NetBeans machine).
- **Slides:** produce at the very end, only once everything else is finished, using the PDF skill.
- UML diagrams: not required.

## 8. Scope Guardrails

**In scope:** mock login/logout, catalog search/filter, instant checkout with limits, My Loans, return logging, late fees with live accrual, penalty settlement (paid/waived), overdue dashboard, all-loans view, inventory CRUD, optional per-item photo upload (local, downscaled JPEG), JSON persistence, demo reset, seed data.

**Out of scope:** networking, real authentication, real payments, damage tracking, manual fee edits, loan cancellation/extension, reservations, barcode/QR, cloud image hosting (photos are local files only), notifications, mobile/web.

## 9. Implementation Status

- **M3 complete: catalog + instant checkout** (2026-11-16). The catalog is a
  real screen: page header, search + category/status filters, a details table
  with a 280 px preview pane by default, a cards toggle that shares one
  selection, an empty state with **Clear filters**, badges, and a 480 px
  checkout dialog whose due date is a non-editable `T … T+14` picker defaulting
  to `T+3`. Managers get the same screen read-only (D24); checkout persists and
  locks the item, and validation errors stay inline in the dialog.
  `CatalogPresenter` owns query/selection/status so `CatalogScreen` stays at
  199 lines (D26). The 15-day demo step is proven by probe, not by typing
  (D25), and `docs/SRS.md` 3.1.2 says so. Verified with `./build.sh`,
  `ant clean jar` (NetBeans-bundled Ant), and `test/run-all.sh` — 9/9 probes
  green, `CatalogUiProbe` `PASS=13 FAIL=0`, manual §6.2 walkthrough captured at
  1280×800 and 1024×640. Fixes made during that pass: the manager caption now
  renders on two lines instead of ellipsizing at 280 px. Known M6 polish: at
  1024×640 the preview's ID row and caption collide and table badges truncate;
  toasts overlap the preview action bar while visible. Inventory and docs:
  `docs/implementation-plan.md` 1.8.0 (D24–D26), `docs/design.md` §5,
  `docs/SRS.md` §6.2, `README.md`, `HANDOFF-M4.md`.
- **M4 complete: returns, late fees, penalty settlement, manager views** (2026-11-16, WP0–WP6 + width pass). Real screens for My Loans, All Loans, Overdue, Penalties and Dashboard; `PenaltyService(+Impl)`/settle (manager-only, one-way, reason-mandatory waive), `accruedFee(loanId, asOf)` overload, shared `LoansTable`/`LoansTableModel`/`LoanPreviewPane`, `ReturnDialog`/`SettlePenaltyDialog`, `ScreenMounts`/`CatalogActions` extracted from `AppController`. Verified with `./build.sh` (187 classes, zero warnings), `ant clean jar`, and `test/run-all.sh` — **12/12 green** including the registered `LoansUiProbe` (`PASS=105 FAIL=0`), plus a 33/33 manual pass through the real `AppController` on a temp seed (SRS §6.2 steps 7–10 and the §10.1 extras, eight 1280×800 screenshots). Seed scenario holds: one overdue row (L-003, ₱200.00), a return freezes exactly one penalty, a waive with reason persists across relaunch, a due-today return creates nothing, and a maintenance flag survives the return (D15). Two defects found and fixed during the close-out: the settle dialog opened on `Waive…` with the reason field disabled (listener registered after the initial pick), and a manager's first status line was dropped because `openMain` routed before showing the frame. Docs: `docs/implementation-plan.md` 1.9.0 (D28–D31), `docs/SRS.md` 3.1.5 (§6.2 steps 8–10 wording), `docs/design.md` §5, `README.md`, `HANDOFF-M4-PLAN.md`, `HANDOFF-M5.md`. Known M6 polish: the penalties table clips its Assessed column at 1280 (no width policy), 1024×640 preview clipping, toast overlap.
- **Scope: photos + skeleton loading moved to M5** (2026-11-15, D27). Seed items
  carry no photos (D11) and catalog queries are synchronous, so `PhotoTile`,
  `SkeletonPanel`, `SkeletonGate`, `SkeletonProbe`, and `PhotoTileProbe` ship
  with the inventory photo picker instead of M3. M3's catalog renders
  `GradientTile` only. Updated in `implementation-plan.md` (D27, M3/M5 rows,
  view inventory, revision 1.6.0), `design.md` §13/§14.2, and
  `HANDOFF-M3-PLAN.md` §6.2/§8.9/§9/§14. Also synced the plan's commit wording
  with `preferences.md` §3 (documented tasks ship without asking).
- **Probe harness rebuilt in-repo (WP0.5)** (2026-11-15, base `60df95d`). Eight
  probes under `desktop-app/test/kitnapper/` (`DataProbe` 26, `CatalogProbe` 28,
  `EdgeProbe` 4, `FrameProbe` 12, `SizeProbe` 2, `SkinProbe` 2, `FontProbe` 2,
  `CloseProbe` `EXITED_OK`) run by `test/run-all.sh` — headless for logic
  probes, `xvfb-run -a` for paint probes; `EdgeProbe`/`SizeProbe` live in
  `kitnapper.view` because `ResizeEdge`, `Sidebar`, and `ScreenCatalog` are
  package-private. Nothing under `test/` compiles into the app jar.
  `test/run-all.sh` is the verification gate for every remaining M3 work
  package; baselines recorded in `HANDOFF-M3-PLAN.md` §9.4/§17.
- **Debt cleanup + probe-rebuild plan** (2026-11-15, `77e900e` base). All
  line-cap violations cleared: `JsonParser` 202 → `JsonParser`/`JsonScanner`,
  `ResizeBands` 213 → `ResizeBands`/`ResizeEdge`, `MainFrame` 182 →
  `MainFrame`/`ScreenCatalog`/`PlaceholderScreen`, `LoanServiceImpl` 188 →
  `LoanServiceImpl`/`LoanQueries`, `LoginFrame` 191 → `LoginFrame`/`LoginCard`.
  Largest file is now `Theme.java` at 171 lines; nothing exceeds 200.
  `Json` lost its only `@SuppressWarnings("unchecked")`. `nbproject/project.properties`
  now builds with `-Xlint:all` and deprecation notes; `desktop-app/build.sh`
  gives Ant-less machines a `clean|build|run` path (92 classes, zero warnings).
  `.gitattributes` enforces LF; `docs/logout-tokens.md` deleted. The lost probe
  harness is planned for `desktop-app/test/kitnapper/` (WP0.5 in
  `HANDOFF-M3-PLAN.md`) — **planned, not yet built**, and blocking WP2
  verification. Existing `view/Dialogs` and `Theme.addThemeListener` stay.
- **M3 WP1 complete: catalog/loan services and controllers** (2026-11-14,
  `19a1e1b`). `CatalogService(+Impl)` (name/serial search, category and status
  filters, deterministic sort), `LoanServiceImpl` (role, availability, 3-loan cap
  and 14-day window checks; return freezes at most one `LATE_FEE`, rejects future
  return dates, preserves `UNDER_MAINTENANCE` per D15), `Ids` (max-suffix
  `L-`/`p-`/`e-` generation), `LoanMessages`, `CatalogController` and
  `BorrowingController`; `AppContext` gained catalog/loan getters, an injectable
  clock and a package-private `fireChanged()`. `LoanServiceImpl` was extracted at
  210 lines into 188 + `LoanMessages`, and again into `LoanQueries` in the
  2026-11-15 debt pass. Verified with `CatalogProbe` (28 checks) plus the full
  existing probe suite; no `kitnapper.view` file changed. **The catalog UI
  (WP2–WP6) is not written** — Catalog is still the M2 placeholder. See
  `HANDOFF-M3-PLAN.md` §17.
- **M2 complete: Win11 shell + mock login implemented** (2026-11-14). Two-frame
  flow (`LoginFrame` ↔ `MainFrame`) with custom-painted chrome (drag, resize
  bands, manual maximize to the work area), role-grouped sidebar, omnibar with
  inert search placeholder, status bar, inline login errors, instant
  dark/light theme toggle, Ctrl+F binding, and placeholder screens (real page
  header + "No records to show yet.") for everything M3+. Auth is mock via
  `AuthService(+Impl)` over `DataStore.users()` (D23). **Deviation:** the
  200-line cap forced a late split into 5 shared paint/chrome files
  (`FlatButton`, `FieldSkin`, `RoundedPanel`, `ResizeBands`, `Sidebar`), so the
  class inventory in `implementation-plan.md` undercounts the view package.
  **Verification caveat:** the pre-split build compiled, jarred, and passed a
  scripted UI walkthrough (login/error/shell/sign-out/manager/theme/maximize/
  drag/resize/close); the post-split rewrite was **not** re-compiled before the
  machine was retired — verify first (see `HANDOFF-M3.md` §2).
- **M1 complete: JSON persistence layer implemented.** `kitnapper.service` +
  `kitnapper.service.persistence` (14 files): hand-written JSON parser/writer (no
  libraries), entity mappers for all three equipment kinds, `AppData`/`DataStore(+Impl)`
  with seed fallback on missing or corrupt saves (NFR-07, D18), a `LoanService`
  skeleton interface, and the PRD 9 seed dataset computed relative to the seeding
  date (D4). Round-trip and corruption checks (16/16) ran through a scratch `main`
  deleted before the commit (`fe9ed34`). `design.md` §13 (skeleton loading states) was
  then resolved with the user. Next: M2 (shell + login) — see `HANDOFF-M2.md`.
- **M0 complete: domain core implemented.** The `kitnapper.model` layer (18 files per `implementation-plan.md` §2.1, including `GeneralEquipment` for the JSON `BASE` kind) compiled and enforced INV-1/2/4/5; at that point `Main.java` was still a blank placeholder and nothing was runnable. The scaffold is the NetBeans Ant project at `desktop-app/` (`C:\Users\CIIT\Downloads\kitnapper\desktop-app` on that retired machine).
- Future build order: model → service/persistence → Swing views → demo polish. **Detailed in `docs/implementation-plan.md` (milestones M0–M6) — follow it one milestone at a time; the class inventory, JSON schema, and implementation decisions D1–D6 live there.**
- Respect `OOP/AGENTS.md`: Java 17 + Swing, NetBeans Ant, no third-party libraries, 200-line file cap, MVC separation.
- Swift was considered during the scaffold decision and **explicitly rejected**; the repository is Java-only.
