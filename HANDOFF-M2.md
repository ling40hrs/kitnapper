# Handoff — M2 Execution (KitNapper)

> **Working document for the next agent session — not a course deliverable.**
> Scope: milestone **M2** of `docs/implementation-plan.md` §4 (Windows 11 shell +
> mock login). Read `AGENTS.md`, then this file, then `docs/design.md` §3–§12 and
> `docs/implementation-plan.md` §2.2, §2.5–§2.6.
> `HANDOFF-M1.md` is kept for traceability — M1 already landed.

## 0. BLOCKER — write `design.md` §13 with the user FIRST

`design.md` §13 (skeleton loading states) is a flagged placeholder and a documented
blocker: it must be fully written **with the user** before any M2 UI code. Minimum
scope it must cover (from §13 itself):

- Which surfaces get skeletons: photo tiles / preview pane while `ImageIO` decodes;
  initial launch while the JSON loads; any screen query over ~100 ms.
- Timing thresholds, minimum display duration, transition out.
- Visual form under the §3.4 gradient ban: flat blocks + subtle opacity pulse —
  **no shimmer sweep** — 4 px radius, `BG_ELEVATED` at reduced alpha.
- Reduced-motion behavior (static blocks, no pulse).
- Interaction rules: never focusable, never announced as content; accessible
  "Loading" state instead.
- Failure/timeout fallback: skeleton → error or empty state, never permanent.
- Code placement (`SkeletonPanel`, per-component skeleton renderers) and the
  milestone that activates it.

Write it into `design.md` §13, commit it (`docs(design): specify skeleton loading
states`), and only then start the M2 code below.

## 1. Current state (as of 2026-11-14, after M1)

- Repo `https://github.com/ling40hrs/kitnapper`, branch `main`, in sync at `fe9ed34`.
- **M0 complete** (`7b0a178`): 18 model classes in `desktop-app/src/kitnapper/model/`,
  invariants INV-1/2/4/5 enforced.
- **M1 complete and pushed** (`fe9ed34`): 14 files — `kitnapper.service` (4) +
  `kitnapper.service.persistence` (10). Hand-written JSON parser/writer (no
  libraries), mappers for all three equipment kinds, `AppData`/`DataStore`(+Impl)
  with seed fallback, `LoanService` skeleton interface, PRD §9 seed dataset.
  Verified by a scratch round-trip `main` (16/16 checks) that was **deleted before
  the commit** — the jar contains no scratch class.
- `Main.java` is still a 0-byte placeholder. **M2 is the milestone that writes it.**
- **Git rule override in effect:** the user granted standing approval — commit and
  push freely without asking. Still keep: conventional commit types, subject
  ≤ 50 chars, body ≤ 72 cols, **both co-author trailers**, SSH signing (configured),
  never commit `build/`, `dist/`, `nbproject/private/`, `*.class`,
  `kitnapper-data.json`, `kitnapper-data.json.tmp`, `kitnapper-images/`.

## 2. M2 scope (implementation-plan §4, row M2)

**Build:** the app shell and login. From M2 on, the project must **launch**.

| File | Package | Contents | Est. lines |
| :--- | :--- | :--- | --: |
| `Main.java` | `kitnapper` | install `Theme`, register fonts, construct services + `AppContext`, show `LoginFrame` on the EDT | 60 |
| `Theme.java` | `kitnapper.view` | all color tokens from design §3 (dark + light ramps, badge dots, tile gradient pairs), `apply()` to `UIManager`, dark/light switch | 170 |
| `Fonts.java` | `kitnapper.view` | probe `Segoe UI Variable Display/Text → Segoe UI` (design §4), register bundled Inter (registered via `GraphicsEnvironment.registerFont`), probe Segoe Fluent Icons / MDL2 for glyphs (§10) | 80 |
| `UiFactory.java` | `kitnapper.view` | buttons (primary/secondary/subtle/destructive), inputs with the 2 px amber focus bottom, Win11 focus rectangle per design §6 | 130 |
| `AppContext.java` | `kitnapper.service` | single set of service instances, shared `LateFeeStrategy`, `DataStore`, session `User`, theme toggle; `addChangeListener(Runnable)` (NFR-02, D2) | 60 |
| `AuthService.java` | `kitnapper.service` | interface: `login(email, password): User` (REQ-003) | 20 |
| `AuthServiceImpl.java` | `kitnapper.service` | authenticates against the users **loaded from `DataStore`**, not a hard-coded list (D23) | 60 |
| `Formats.java` | `kitnapper.view` | money `₱1,234.00`, dates `yyyy-MM-dd` — the single place both are defined (design §8) | 40 |
| `Dialogs.java` | `kitnapper.view` | modal dialog shell (480 px default, `BG_ELEVATED`, scrim) + themed destructive-confirmation helper (design §6/§7) | 90 |
| `WindowChrome.java` | `kitnapper.view` | undecorated-frame behavior: drag, double-click maximize, 8 px edge resize bands, min size 1024×640, restore bounds (design §11). Tightest file — split behavior/painting with `TitleBar` | 180 |
| `TitleBar.java` | `kitnapper.view` | 32 px title bar: wordmark, screen title, theme toggle, min/max/close painting + 46×32 hit targets, close hover `CLOSE_HOVER` (design §5) | 140 |
| `Omnibar.java` | `kitnapper.view` | 48 px breadcrumb + contextual command row + search field + layout toggle (design §5). Breadcrumb only in M2; commands become functional in M3 | 160 |
| `StatusBar.java` | `kitnapper.view` | 32 px counts, selection summary, monospace aggregate (design §5) | 70 |
| `LoginFrame.java` | `kitnapper.view` | centered 420 px card on `BG_APP`: email, password, sign-in, inline error, collapsed demo-accounts hint; same title bar, omnibar/sidebar/status hidden | 120 |
| `MainFrame.java` | `kitnapper.view` | hosts chrome, sidebar + content `CardLayout` swap; content area is a placeholder panel until M3 fills Catalog | 150 |
| `AppController.java` | `kitnapper.controller` | session user, screen routing (sidebar → content swap), theme toggle, logout; owns `AppContext` | 120 |

Sidebar (240 px) is role-grouped (design §5): borrowers see Catalog + My Loans,
managers see Dashboard / All Loans / Overdue / Penalties / Inventory; footer holds
Settings/Data, user chip, Sign out. Non-M3 screens are placeholder panels — routing
skeleton is the deliverable, not the screens.

## 3. M0/M1 API the M2 code builds on (exact)

- **Model:** `User` (abstract, immutable) with public `Borrower`/`Manager` ctors;
  `getRole()` → `UserRole`; `getPassword()` is public by design (mock login +
  persistence). `LateFeeStrategy.RATE_PER_DAY = 50.00`.
- **DataStore (interface, `kitnapper.service`):**
  - `AppData load()` — loads `kitnapper-data.json` from the app folder; missing,
    unparseable, or semantically corrupt file → fresh seed **and saves it
    immediately**; never throws for a bad file (NFR-07, D18).
  - `void save()` — serialize to `.tmp`, then `ATOMIC_MOVE` over the target with a
    `REPLACE_EXISTING` fallback for Windows.
  - `void resetToSeed()` — seed relative to `LocalDate.now()` (D4) and overwrite.
  - Read accessors `users()`, `equipment()`, `loans()`, `penalties()` return the
    **live mutable lists** (interpretation of D17: services add/remove by list
    mutation, entity edits only via entity methods).
- **DataStoreImpl:** no-arg ctor uses the working directory as the app folder;
  `DataStoreImpl(Path appFolder)` exists so tests/scratch can use a scratch dir.
- **AppData:** holds the four lists; `RootMapper` builds a fresh instance and it is
  swapped in only when a document fully validates (D18).
- **Seeded accounts (what login must accept):** `admin@institution.edu` / `admin123`
  (MANAGER); `student1@institution.edu`, `student2@institution.edu` / `student123`
  (BORROWER). Auth must read these from `DataStore.users()`, not hard-code (D23).
- **M1 strictness choices (kept, don't loosen):** `Json.requireString` rejects blank
  strings; persisted `OVERDUE` normalizes to `ACTIVE` (D1); a persisted `ACTIVE`
  loan with a `returnDate` is corruption; `UNPAID`/`PAID` penalties carrying a
  `waiveReason` are corruption.

## 4. Design ground rules for M2 (do not reopen)

- Windows 11 / Fluent look, **custom-painted Swing**, no libraries, no Mica blur
  (flat `BG_APP`), square outer corners (D9). All hex values live only in `Theme`.
- Gradients are forbidden everywhere except the photo-fallback tile (§3.4) — which
  M2 does not need yet (no photo tiles until M3/M5).
- Dark + light token sets; the theme toggle is **session-only** (D3) — never
  persisted in the JSON.
- No tabs. Title bar (32 px) + omnibar (48 px) + sidebar (240 px) + status bar
  (32 px); spacing scale 4/8/12/16/24/32 px; radii: controls 4, cards/dialogs 8,
  badges 10, window corners 0 (§9).
- Focus visuals: 2 px `ACCENT` rectangle with 1 px inner gap, always visible (§6/§8).
- Status never color-alone (badges carry dot + word) — badges themselves are M3/M4.
- Microcopy mixed by role: plain for borrowers, formal for manager surfaces.

## 5. Verification (definition of done)

1. `ant clean jar` (from `desktop-app/`) → BUILD SUCCESSFUL.
2. Launch: `ant run` or `java -cp build\classes kitnapper.Main` from
   `desktop-app/` (JDK at `C:\Program Files\Java\jdk-21`).
3. SRS §6.2 steps 1–3: login screen appears; wrong password → inline error, no
   login; `student1@institution.edu` / `student123` lands on the shell with search
   placeholder (search itself is M3). Also log in as the manager once and sign out.
4. Chrome: drag, double-click maximize, 8 px edge resize bands, min 1024×640,
   restore bounds; window controls hover states; theme toggle switches dark/light
   within the session; logout returns to login.
5. Line-count audit: all new files ≤ 200 lines; LF endings, 4-space indent.
6. First launch writes `kitnapper-data.json` (in the run working directory —
   gitignored, **never commit**). Restart keeps seeded data.
7. Commit + push (message below).

## 6. Commit message (ready to use)

```text
feat(ui): add Win11 shell and mock login

Add the M2 presentation layer: Fluent color tokens and font
probing in hand-painted Swing, the custom undecorated window
chrome, the title bar / omnibar / status bar shell with a grouped
sidebar, and the login screen authenticating against the users
loaded from DataStore.

- Main installs the theme and fonts, builds AppContext, and shows
  LoginFrame on the EDT
- Custom chrome: drag, double-click maximize, 8 px edge resize
  bands, 1024x640 minimum, restore bounds (design 11, D9)
- Theme toggle is session-only (D3); non-M3 screens are
  placeholders behind the sidebar router

Co-authored-by: Aerovic K. Cudia <253015735+aerovickiesurcudia@users.noreply.github.com>
Co-authored-by: Chrisleen Deinese L. Villacorza <230848138+Leen-dotcom@users.noreply.github.com>
```

## 7. Do NOT in M2

- No catalog, checkout, return, fee, or penalty logic (M3/M4). No `LoanServiceImpl`.
- No `CatalogService`/`InventoryService`/`PenaltyService` interfaces yet.
- No photo tiles, badges, tables, dialogs beyond the `Dialogs` shell.
- No schema changes, no `schemaVersion` bump, no test framework.
- Do not commit runtime files (`kitnapper-data.json`, `kitnapper-images/`) or
  build output.

## 8. Gotchas

- `jar.exe` is not on PATH; it is at `C:\Program Files\Java\jdk-21\bin\jar.exe`.
- Swing code must run on the EDT (`EventQueue.invokeLater` in `Main`); painting
  code should avoid work on the EDT where design §11 says so (image decoding is a
  later milestone).
- Plan §2.2 lists `AuthServiceImp.java` — a typo. Use `AuthServiceImpl` for
  consistency with the other `*ServiceImpl` classes and fix that plan row when the
  docs are next touched.
- Machine-specific `OOP/` and `Finals/` path prefixes in docs belong to the other
  (NetBeans) machine; this checkout's root stands in for `OOP/Finals/` — strip the
  prefixes, don't edit the other machine's references.
- `AGENTS.md` itself is gitignored (local agent rules) — never commit it.

## 9. After M2

M3 = catalog + checkout (`CatalogScreen`, `EquipmentCard`, `PreviewPane`,
`PhotoTile`, `GradientTile`, `SegmentedControl`, `Badges`, `CatalogTableModel`,
`CatalogController`, `CatalogService(+Impl)`, `BorrowingController`,
`CheckoutDialog`, `LoanServiceImpl`, `Toasts`; SRS §6.2 steps 3–6). Write the next
handoff when M2 lands.
