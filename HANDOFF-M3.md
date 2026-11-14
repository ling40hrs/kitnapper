# HANDOFF — M2 done, next: M3 (Catalog + Loans)

Written 2026-11-14 on the Windows CIIT machine, which is being retired. Read
this whole file before touching code.

## 1. What M2 delivered (committed)

Win11 shell + mock login, 21 source files, all under `desktop-app/src/`:

- `kitnapper.Main` — entry point, launches `LoginFrame` on the EDT.
- `kitnapper.controller.AppController` — owns the two-frame flow
  (login ↔ main), routing, sign-out; views hold no logic.
- `kitnapper.service` — `AppContext` (wires DataStore + AuthService),
  `AuthService(+Impl)`. `login` returns `Optional<User>`; auth reads
  `DataStore.users()` (D23).
- `kitnapper.view` — `Theme` (all hex lives here), `Fonts` (probes Segoe UI
  Variable / Fluent Icons, falls back gracefully), `Formats`, `UiFactory`
  (builds + theme-refreshes controls), `FlatButton`, `FieldSkin`,
  `RoundedPanel`, `Dialogs` (modal shell, unused until M3/M5), `WindowChrome`
  (undecorated frame, drag, manual maximize w/ work-area bounds),
  `ResizeBands` (8 px edge/corner bands), `TitleBar`, `Omnibar` (breadcrumb +
  inert search placeholder), `StatusBar`, `Sidebar` (role-grouped, chip +
  sign-out), `MainFrame` (shell + placeholder screens), `LoginFrame`.

Seeded credentials: `admin@institution.edu`/`admin123` (MANAGER),
`student1@institution.edu`/`student123` (BORROWER).

## 2. Verification status — READ CAREFULLY

**Verified green (pre-split build):** compile, `ant clean jar` (BUILD
SUCCESSFUL), and a scripted UI walkthrough with screenshots: login renders,
wrong password → inline DANGER error under the field, student login → shell
(breadcrumb, sidebar, `10 items · 0 selected`), sign-out → login, manager
login → Dashboard + manager nav + Settings, dark↔light theme toggle fully
recolors (incl. content panel fix in `MainFrame.refreshTheme`), maximize
fills the work area (2560×1392) and restores, title-bar drag moved the window
exactly +100/+50, south-edge resize changed bounds, Alt+F4 closed the
process, demo-accounts disclosure expands, initial focus lands on the
default nav button / login email field.

**NOT verified after the final refactor:** the 200-line cap forced a late
split into 5 new shared files (`FlatButton`, `FieldSkin`, `RoundedPanel`,
`ResizeBands`, `Sidebar`) and rewrites of `UiFactory`, `WindowChrome`,
`MainFrame`, `LoginFrame`, `Dialogs`. The **last successful compile predates
these rewrites.** The machine's shell then became unstable (see §5) and a
clean post-refactor compile could not be captured.

**First action on any machine:** compile and smoke-test before anything else:

```
cd desktop-app
ant clean jar        # or: javac -encoding UTF-8 -d build/classes @sources.txt
java -cp build/classes kitnapper.Main
```

Then walk SRS §6.2 steps 1–3 plus: theme toggle (light content panel must be
light — `MainFrame.refreshTheme` sets it), sign out, manager nav, Ctrl+F
(exists in `MainFrame`, was never explicitly verified), sidebar navigation.
Likely failure modes are dumb ones: missing imports after the extraction
(`BorderFactory` was forgotten twice), a typo, or `LoginFrame`'s BoxLayout
`stretch()` helper interacting with the swapped-in `RoundedPanel`.

## 3. Known deviations from the docs

1. **File split (sanctioned):** 5 files exceed nothing now, but the split
   itself is a deviation from the class inventory in
   `implementation-plan.md` — recorded there and in `decisions.md` §9.
2. **`Dialogs` has no call site in M2** — compile-only shared shell for
   M3/M5 dialogs.
3. **Resize feel unverified** (DPI quirk, §5): mouse-driven resize changed
   bounds but scripted drags inverted direction at 125% scaling. Real-user
   resize must be checked by hand in the demo.

## 4. Machine-specific notes (CIIT machine — now removed)

- JDK 21 at `C:\Program Files\Java\jdk-21\bin` (satisfies Java 17+);
  Ant 1.10.18 at `C:\Users\CIIT\tools\apache-ant-1.10.18`.
- This checkout root = the other machine's `OOP/Finals/`; strip those
  prefixes from doc/README paths.
- `desktop-app/kitnapper-data.json` + `kitnapper-images/` are created at
  runtime and gitignored — never commit them.
- Git: SSH signing + `ling40hrs` author + both `Co-authored-by:` trailers
  configured (see `docs/decisions.md` §6). `git commit -m` bypasses the
  template — trailers were added manually in both M2 commits; verify with
  `git log --format="%(trailers)" -1`.

## 5. Session findings / gotchas worth remembering

- **Scripted UI automation recipe that worked** (PowerShell + Add-Type
  P/Invoke): `EnumWindows` by PID to find windows, `AttachThreadInput` +
  `SetForegroundWindow` + `BringWindowToTop` for focus (verify
  `GetForegroundWindow() == hWnd` before ANY SendKeys), `PrintWindow(h, hdc,
  2)` for screenshots (works for undecorated Swing), `mouse_event` for
  clicks/drags. Unguarded SendKeys once typed into the user's Word doc —
  **always check focus first**.
- **DPI:** screen is 2560×1600 at 125% with DPI virtualization; scripted
  mouse coordinates are only reliable for window-relative positions computed
  from `GetWindowRect`, not absolute desktop targets.
- **BoxLayout gotcha:** children center at preferred size — every card child
  needs `alignmentX=0` + max-width; `LoginFrame.stretch()` does this. Set a
  container's max size **after** adding its children, not before.
- **Theme refresh:** custom-painted components repaint fine, but plain
  `JPanel`s keep the stale `UIManager` background — `refreshTheme` must set
  them explicitly (fixed for content pane in `MainFrame`).
- **Late-session machine instability:** PowerShell `Get-ChildItem` /
  `cmd` redirects intermittently returned empty results in the final hour
  (same commands succeeded minutes earlier). If this machine (or its clone)
  misbehaves like that, restart the shell rather than suspecting the code.

## 6. What NOT to do in M3

- No third-party libraries, no JavaFX, no test framework. JSON is the
  hand-written layer from M1.
- Milestone M3 = catalog + loans per `implementation-plan.md`; do not pull
  M4/M5 work forward. Non-M3 screens keep the placeholder pattern
  (`MainFrame.placeholder`).
- All hex colors only in `Theme`; files ≤ 200 lines (split early, don't
  repeat M2's end-of-milestone crunch).
- Docs are the source of truth; update code + docs together.
