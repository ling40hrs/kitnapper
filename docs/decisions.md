# KitNapper — Decision Log

> Single source of truth for every decision made during the requirements interviews and repository audit. When this file and a design guess conflict, this file wins. Application code is intentionally **not** written yet; see §9.

## 1. Project Identity

| Item | Decision |
| :--- | :--- |
| Product | **KitNapper** — desktop equipment-lending tracker |
| Problem | Replace physical sign-out logbooks for schools/studios |
| Team | **YAPPERS** (group project): Casiano, Cudia, Villacorza |
| Course | CS203 – Object-Oriented Programming |
| Current phase | Documentation only (PRD, SRS, design) |

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
| `docs/PRD.md` | 3.1.0 | Product requirements, domain model, state machines, seed data, optional per-item photo |
| `docs/SRS.md` | 3.1.0 | Verifiable requirements, invariants, demo walkthrough, traceability, image-storage NFR |
| `docs/design.md` | 2.0.0 | Windows 11 / Fluent direction (Files app): neutral ramp + amber accent, tokens, chrome/shell, components, photo uploads; §13 skeleton plan pending |
| `docs/seed-data.md` | — | Points to PRD §9; save-file/reset behaviour |
| `docs/logout-tokens.md` | — | Runbook for removing all stored GitHub credentials from the Windows build machine |
| `docs/decisions.md` | — | This file |
| `docs/implementation-plan.md` | 1.1.0 | Class/file inventory, JSON persistence spec, milestones M0–M6, implementation decisions D1–D13 (written 2026-11-14, before any code; v1.1.0 retargets the UI to design.md v2.0) |

## 5. Design Direction

**v2.0 rewrite 2026-11-14** — the visual direction is now **Windows 11 / Fluent** (Files
app as primary reference), superseding the warm dark-dashboard pass of the same date
(kept as history in `design.md` §14.1). What stands now:

- **Neutral Win11 surface ramp** (`#1F1F1F` app, `#272727` surfaces, `#323232` controls) with the **amber accent `#F0A32E` retained** as the single brand color; square outer window corners.
- **Typography:** Segoe UI Variable → Segoe UI → bundled Inter (fallback), Segoe Fluent Icons → MDL2 → Unicode glyph fallbacks.
- **Shell:** custom title bar (**no tabs**) + omnibar row (breadcrumb, commands, search, layout toggle), grouped sidebar, status bar with counts/selection/aggregate.
- **Catalog + Inventory default to details table + preview pane** (overrides the v1 "defaults to cards" answer); the layout toggle switches the left pane to cards.
- **Status badges:** amber-tinted pill + semantic dot + word (never color alone; wrench glyph on `UNDER_MAINTENANCE`).
- **Gradients are restricted to the single photo-fallback category tile** — every other surface is flat; no Mica blur, no glows, no shadows.
- **Image uploads are in scope** (explicit override of the PRD §8 non-goal): optional per-item photo, stored locally under `kitnapper-images/`, downscaled to ≤ 1024 px JPEG, 20 MB source cap, reset demo data wipes the folder, seed ships without photos.
- **Skeleton loading states are an open blocking plan** — `design.md` §13 must be written with the user before UI implementation (M2) starts.
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

- Remote: `https://github.com/ling40hrs/Schoolwork`, branch `main`.
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

- **Scaffold created; no application code.** `Finals/desktop-app/` is a NetBeans Ant project with `nbproject/`, `build.xml`, `manifest.mf`, empty package folders, and a blank `Main.java` placeholder. Nothing has been implemented. In the current checkout this project sits at `desktop-app/` on the repository root (`C:\Users\CIIT\Downloads\kitnapper\desktop-app`).
- Future build order: model → service/persistence → Swing views → demo polish. **Detailed in `docs/implementation-plan.md` (milestones M0–M6) — follow it one milestone at a time; the class inventory, JSON schema, and implementation decisions D1–D6 live there.**
- Respect `OOP/AGENTS.md`: Java 17 + Swing, NetBeans Ant, no third-party libraries, 200-line file cap, MVC separation.
- Swift was considered during the scaffold decision and **explicitly rejected**; the repository is Java-only.
