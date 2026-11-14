# SESSION HANDOFF — 2026-11-14 (M2 re-audit + repo/tooling setup)

Supersedes the "NOT verified after final refactor" warning in
`HANDOFF-M3.md` §2: the post-split build has now been compiled, audited,
fixed, verified, committed, and pushed. Read this file first, then
`HANDOFF-M3.md` for the M3 plan.

---

## 1. TL;DR — current state

- **Branch:** `main`, clean working tree.
- **HEAD:** `745ae28` — `fix(ui): repair M2 field, resize, and exit bugs`
  (parent `4998bf4`), **pushed to `origin`** and **GitHub-verified**.
- **Build:** `ant clean jar` → BUILD SUCCESSFUL on JDK 17.
- **Audit:** 9 defects found and fixed (1 critical: typed login text was
  invisible). All probes green — see §5.
- **Machine is fully set up:** JDK 17, Ant 1.10.18, `gh` 2.104.0, git
  identity, SSH signing key registered on GitHub.
- **Next milestone:** M3 (catalog + loans). See §9.

---

## 2. What this session did

1. Re-audited all of M2 for runtime correctness (not just compilation).
2. Found and fixed 9 bugs across 11 files (all under
   `desktop-app/src/kitnapper/view/`).
3. Built a reusable probe harness to prove each fix with before/after
   evidence.
4. Committed the fixes with the repo's required author/co-author/signature
   rules.
5. Installed `gh`, completed device-flow login, registered the SSH signing
   key on GitHub, and pushed `main`.
6. Initialized local `AGENTS.md` with project notes and user preferences.

---

## 3. Machine / environment setup (already done)

### Toolchain

| Tool | Version | Path |
| :--- | :--- | :--- |
| JDK | Temurin 17.0.21 | `/Users/student/kitnapper-jdk/Contents/Home` |
| Ant | 1.10.18 | `/Users/student/ant-1.10.18` |
| gh | 2.104.0 | `/Users/student/tools/gh_2.104.0_macOS_arm64/bin/gh` |
| git | 2.54.0 (Apple) | system |

Build/run exports:

```sh
export JAVA_HOME=/Users/student/kitnapper-jdk/Contents/Home
export ANT_HOME=/Users/student/ant-1.10.18
export PATH="$JAVA_HOME/bin:$ANT_HOME/bin:$PATH"
```

### Git identity & signing (repo-local)

```sh
user.name=Ling Chen
user.email=lingling40hourspno@gmail.com
user.signingkey=/Users/student/.ssh/id_ed25519.pub
commit.gpgsign=true
gpg.format=ssh
gpg.ssh.allowedSignersFile=/Users/student/.ssh/allowed_signers
```

- Signing key: `~/.ssh/id_ed25519` (private), `~/.ssh/id_ed25519.pub`
  (public).
- Public key:
  `ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIEyxOgHTDSt8pWdvAIENF1dNO8GMjdQ4uLeXttSRU7tT KitNapper build machine`
- Local verification fingerprint:
  `SHA256:rm4XQUCeignM1Yl1I/30FKExURnFwYa2tKEE+yEfiaw`
- Registered on GitHub as an **SSH signing key**: ID `1332268`,
  title `KitNapper build machine`, account `ling40hrs`.
- `~/.ssh/allowed_signers` contains:
  `lingling40hourspno@gmail.com <public key>`.

### GitHub auth

- `gh` logged in as `ling40hrs` (token in macOS keyring).
- Token scopes: `admin:ssh_signing_key`, `gist`, `read:org`, `repo`.
- `gh auth setup-git` configured the HTTPS credential helper.
- The signing key is **not** registered as an auth key; HTTPS pushes use
  the `gh` token, not SSH.

> If auth is lost (new machine/keychain reset), run:
> `gh auth login -h github.com --git-protocol https --web`
> then `gh auth refresh -h github.com -s admin:ssh_signing_key`.

---

## 4. Bugs fixed in this session

All files are `desktop-app/src/kitnapper/view/`.

| # | Severity | File | Bug | Fix |
| :- | :--- | :--- | :--- | :--- |
| 1 | **Critical** | `FieldSkin.java` | Field background was painted from a `Border`. Swing paints borders *after* the component, so the fill covered typed email/password text. Rendering was a flat box (`light_pixels=0`). | Field paints its own rounded skin before `super.paintComponent`; border is now a plain `EmptyBorder` (insets only). |
| 2 | High | `ResizeBands.java` | `edgeAt()` compared strip-relative mouse coordinates against pane bounds, so south read as north and east as west (inverted resize). | Convert the event point with `SwingUtilities.convertPoint(...)`; probe maps every strip correctly. |
| 3 | High | `WindowChrome.java` | `close()` called `dispose()`, bypassing `EXIT_ON_CLOSE`; the AWT thread kept a zombie JVM alive. | Dispatch `WINDOW_CLOSING` so the frame's close operation runs. |
| 4 | Functional | `Fonts.java` | Installed families were snapshotted *before* registering the bundled Inter font, so Inter was never selected. | Call `registerInter()` first, then probe families. |
| 5 | Functional | `ResizeBands.java`, `WindowChrome.java` | Resize bands stayed active while maximized (design §11 says disable). | `ResizeBands` takes a `BooleanSupplier maximized` and ignores press/drag when true. |
| 6 | Functional | `UiFactory.java` | Theme toggle recolored `JLabel`s only; text fields kept stale text/caret color. | `refresh()` also recolors `JTextField` (covers password fields). |
| 7 | Build | `Sidebar.java` | Missing `Graphics2D` / `BasicStroke` imports — compile error after the late split. | Added imports. |
| 8 | Latent | `RoundedPanel.java`, `TitleBar.java`, `Omnibar.java`, `StatusBar.java` | `super.paintComponent(g)` called last (wrong Swing order). | Moved to first. |
| 9 | Latent | `MainFrame.java` | `showScreen` NPE on an unknown key. | Guard `meta == null`. |

---

## 5. Verification evidence

All results below are from the final clean build. Re-run everything with:

```sh
sh /Users/student/kitnapper-probes/run-all.sh
```

| Probe | What it proves | Result |
| :--- | :--- | :--- |
| `ant clean jar` | Full build | BUILD SUCCESSFUL |
| `DataProbe` | Seed counts, borrowable/loaned, auth (case/trim/blank/null), overdue fee, formats, JSON round-trip, corrupt-file fallback | `PASS=26 FAIL=0` |
| `SkinProbe` | Typed text is actually visible in a rendered field | `light_pixels=175` (was `0`) |
| `EdgeProbe` | All four resize strips resolve to the correct edge | `PASS=4 FAIL=0` |
| `FrameProbe` | Login/manager/borrower paint in dark + light, no exceptions | `PASS=5 FAIL=0` |
| `FontProbe` | Bundled Inter is selected for text/icons | `text=Inter`, `icon=Inter` |
| `SizeProbe` | Real layout bounds are sane | title bar 32, omnibar 48, sidebar 240, status 32, fields 32 |
| `CloseProbe` | Custom close button terminates the JVM | `EXITED_OK` |
| Launch smoke | `java -cp build/classes kitnapper.Main` starts cleanly | no errors |

Probe sources (durable, **outside** the repo):
`/Users/student/kitnapper-probes/`

- `probe/DataProbe.java` — data/auth/format/round-trip.
- `probe/FrameProbe.java` — paints both frames in both themes.
- `probe/FontProbe.java` — font family resolution.
- `probe/SizeProbe.java` — component layout dump.
- `probe/CloseProbe.java` — close-button JVM exit.
- `kitnapper/view/SkinProbe.java` — field text pixels (package-private access).
- `kitnapper/view/EdgeProbe.java` — resize-edge mapping via reflection.
- `run-all.sh` — builds, compiles probes, runs the suite.

> `DataProbe` intentionally writes a corrupt JSON file in a *temp* dir to
> test fallback; the warning it prints is expected.

---

## 6. Commit & push record

```
commit 745ae2899b894755d55740825e1dab5100b8ba3b
parent 4998bf4fd0220c33361c2d7489870a38fc48bee0
Author: Ling Chen <lingling40hourspno@gmail.com>
Subject: fix(ui): repair M2 field, resize, and exit bugs
Co-authored-by: Aerovic K. Cudia <...>
Co-authored-by: Chrisleen Deinese L. Villacorza <...>
```

- 11 files changed, 86 insertions(+), 57 deletions(-).
- SSH-signed (`gpgsig` header present).
- Local check: `Good "git" signature for lingling40hourspno@gmail.com`.
- Remote check: GitHub API returns `verified: true`, `reason: valid`.
- Push: `4998bf4..745ae28 main -> main`.
- `git ls-remote origin main` matches `745ae28...`.

---

## 7. Repository / local file state

- Working tree clean; no tracked build or runtime artifacts.
- Gitignored: `AGENTS.md`, `kitnapper-data.json`, `kitnapper-images/`,
  `build/`.
- `AGENTS.md` at repo root documents project, build, git rules, and user
  preferences. **Never commit it.**
- Probe harness lives at `/Users/student/kitnapper-probes/` (outside repo).

---

## 8. Known limitations / not verified

- **Real mouse drag/resize** and **OS DPI scaling** (the old 125% quirk)
  could not be validated by automation. Do a short manual pass in the demo.
- **`Dialogs.java`** is still compile-only (no M2 call site); statically
  reviewed, no defects found.
- Verification ran on **macOS / JDK 17**; the design target is Windows
  (Segoe UI / Fluent Icons fall back gracefully).
- No automated UI test framework exists (per M3 constraints) — probes are
  ad-hoc and live outside the repo.
- `FrameProbe` uses offscreen `paint()`; it catches painting exceptions and
  blank layouts but not input/focus behavior.

---

## 9. Next steps — M3 (catalog + loans)

Read `HANDOFF-M3.md` (especially §6 "What NOT to do") and
`docs/implementation-plan.md` first. Then:

1. Replace the M3 placeholder screens in `MainFrame` with real catalog and
   loans screens (manager + borrower variants per the docs).
2. Keep all hex colors in `Theme`; keep files **≤ 200 lines** (split early).
3. No third-party libraries, no JavaFX, no test framework; reuse the
   hand-written JSON layer and `DataStore`.
4. Wire list/selection state into the existing `statusText` callback
   (`10 items · 0 selected`) and the omnibar breadcrumb.
5. Add M3 probes (filtering, borrow/return transitions, limit enforcement,
   fee freeze on return) alongside the existing suite.
6. Update docs (`HANDOFF`, `docs/`) in the same change as behavior.
7. Build, run `run-all.sh`, then commit/push **after explicit approval**,
   following §3/`AGENTS.md` git rules.

---

## 10. Gotchas worth remembering

- **Never paint a component's background from a `Border`** — Swing draws
  the border after the component and will cover text.
- `SwingUtilities.convertPoint` is mandatory when mixing strip-local and
  pane coordinates.
- Close custom chrome with `WINDOW_CLOSING`, not `dispose()`.
- Register bundled fonts **before** checking `getAvailableFontFamilyNames()`.
- Call `super.paintComponent(g)` **first**, not last.
- `ant` may report "Total time: 0 seconds" on a warm daemon; trust
  `BUILD SUCCESSFUL`, not the timing.
- `git commit -m` bypasses any commit template — add trailers manually and
  verify with `git log -1 --format='%(trailers)'`.
- `git commit` fails without signing config; do **not** use
  `--no-gpg-sign` (repo forbids bypassing signing).
- `gh api user/ssh_signing_keys` needs `admin:ssh_signing_key`; plain
  `repo` scope returns 404.

---

## 11. Quick start for the next session

```sh
export JAVA_HOME=/Users/student/kitnapper-jdk/Contents/Home
export ANT_HOME=/Users/student/ant-1.10.18
export PATH="$JAVA_HOME/bin:$ANT_HOME/bin:$PATH"

cd /Users/student/kitnapper
git status
git log -3 --oneline

# Full build + probe suite
sh /Users/student/kitnapper-probes/run-all.sh

# Manual smoke
cd desktop-app
ant clean jar
java -cp build/classes kitnapper.Main
```

Seeded logins: `admin@institution.edu` / `admin123`,
`student1@institution.edu` / `student123`.
