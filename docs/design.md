# KitNapper — Design Specification

> **Status:** v2.0 — visual language switched to **Windows 11 / Fluent** on 2026-11-14,
> superseding the warm dark-dashboard pass of the same date (both recorded in §14).
> All styling questions are resolved. **§13 (skeleton loading states) is a pending plan
> and blocks the start of UI implementation.**

## 1. Brand

| Item | Value |
| :--- | :--- |
| Product name | **KitNapper** |
| Tagline | *Every kit, accounted for.* |
| Logo | TBD (text wordmark is the fallback) |
| Visual language | Windows 11 / Fluent — native desktop app, Files-app shell |
| Tone of voice | Direct, calm, staff-tool confidence — no exclamation marks, no cute errors |
| Team | YAPPERS — Casiano, Cudia, Villacorza |

## 2. Style Direction

**Native Windows 11 desktop app.** KitNapper should look like it shipped with the OS: flat
neutral surfaces, exactly one accent (amber), Fluent geometry, Segoe UI. Primary visual
reference: the Files app (`files.community`) and its dark hero screenshot — custom title
bar, omnibar row, grouped sidebar, details list + preview pane, status bar. Secondary
reference: `shelf.nu` for asset-lending domain layouts (booking cards, availability badges,
dashboard KPIs).

A staff console, not a consumer app: dense tables where data matters (loans, inventory),
airy preview where scanning matters (catalog, dashboard KPIs).

**What we borrow from Files:** custom title bar (no tabs), omnibar/command-bar row,
sidebar with section grouping, details table + preview pane, status bar with counts, thin
scrollbars, 8 px-radius cards, WinUI-style badges, always-visible focus rectangles.

**What we do NOT borrow:** Mica/acrylic blur, tabs, dual pane, columns view, drag & drop,
color tags, Git pane, background images.

**Hard constraint:** no third-party libraries (PRD §7.2) — no FlatLaf, no theme packs,
nothing bundled as code. The look is custom-painted Swing: `UIManager` defaults, custom
`TableCellRenderer`s, custom `JPanel` backgrounds. System fonts are used where present
(§4, §10) — using a font that ships with Windows is not a library violation; nothing is
added to the app except the bundled Inter files (font assets, not libraries). Mica is
simulated as one flat color — there is no blur (§11).

## 3. Color Tokens

Named in code (`Theme.java` constants); hex values never appear in panels. Every pair
marked for contrast in §8 must be verified in **both** themes.

### 3.1 Dark theme (primary)

| Token | Value | Use |
| :--- | :--- | :--- |
| `BG_APP` | `#1F1F1F` | window background — the flat "Mica" stand-in |
| `BG_SURFACE` | `#272727` | sidebar, panes, cards |
| `BG_ELEVATED` | `#2D2D2D` | dialogs, hovered surfaces, preview pane |
| `BG_CONTROL` | `#323232` | inputs, secondary buttons, segmented control track |
| `BG_CONTROL_HOVER` | `#3A3A3A` | control hover |
| `BORDER_SUBTLE` | `#383838` | pane dividers, card outlines, row separators |
| `BORDER_STRONG` | `#4A4A4A` | input bottoms, focused/important outlines |
| `TEXT_PRIMARY` | `#F5F5F5` | headings, values, table text |
| `TEXT_SECONDARY` | `#C2C2C2` | labels, metadata, header row |
| `TEXT_MUTED` | `#9B9B9B` | captions, placeholders |
| `TEXT_DISABLED` | `#6E6E6E` | disabled |
| `ACCENT` | `#F0A32E` | primary fill, selection bar, focus ring |
| `ACCENT_HOVER` | `#F5B455` | primary button hover |
| `ACCENT_PRESSED` | `#D88F22` | primary button pressed |
| `ON_ACCENT` | `#1A1408` | text on amber |
| `ACCENT_TEXT` | `#F3B65B` | amber used *as text* (links, active breadcrumb crumb) |
| `ACCENT_TINT` | `rgba(240,163,46,0.12)` | selected nav item, selected table row |
| `HOVER_ROW` | `#2A2A2A` | table/list row hover |
| `DANGER` | `#E0644A` | destructive text/border, error text |
| `CLOSE_HOVER` | `#C42B1C` | window close-button hover (Win11 red) |

The amber accent is orange-leaning and stays the **only** saturated brand color. It never
becomes a gradient (§3.4) and never colors text — amber-as-text is always `ACCENT_TEXT`.

### 3.2 Light theme

| Token | Light value |
| :--- | :--- |
| `BG_APP` | `#F3F3F3` |
| `BG_SURFACE` | `#FBFBFB` |
| `BG_ELEVATED` | `#FFFFFF` |
| `BG_CONTROL` | `#FFFFFF` |
| `BG_CONTROL_HOVER` | `#F5F5F5` |
| `BORDER_SUBTLE` | `#E3E3E3` |
| `BORDER_STRONG` | `#D2D2D2` |
| `TEXT_PRIMARY` | `#1B1B1B` |
| `TEXT_SECONDARY` | `#5A5A5A` |
| `TEXT_MUTED` | `#767676` |
| `TEXT_DISABLED` | `#A6A6A6` |
| `ACCENT` | `#F0A32E` (fill) with `ON_ACCENT #1A1408` |
| `ACCENT_TEXT` | `#A35F00` |
| `ACCENT_TINT` | `rgba(240,163,46,0.16)` |
| `HOVER_ROW` | `#F0F0F0` |
| `DANGER` | `#B9432F` |
| `CLOSE_HOVER` | `#C42B1C` |

### 3.3 Status badges (amber-derived)

One badge family: a warm amber-tinted pill + a 6 px semantic dot + the status word
(always rendered — status is never color alone). Height 20 px, pill radius, 12 px
semibold word in `TEXT_PRIMARY`, fill = `ACCENT_TINT`.

| Status | Dot (dark) | Dot (light) |
| :--- | :--- | :--- |
| `AVAILABLE` | `#8FBF6A` | `#3E7A24` |
| `LOANED` | `#7EA6C8` | `#35679C` |
| `OVERDUE` | `#E06A4E` | `#B4472F` |
| `UNDER_MAINTENANCE` | `#D9A441` | `#8F6A1E` |
| `DECOMMISSIONED` | `#8C8378` | `#6B6259` |

- `UNDER_MAINTENANCE` additionally carries a small wrench glyph before the word so it can
  never be confused with the amber accent fill.
- Penalty badges reuse the same anatomy: `UNPAID` → Overdue dot, `PAID` → Available dot,
  `WAIVED` → Decommissioned dot.

### 3.4 Gradient policy

Gradients are a **single, audited exception**, not a styling tool.

| Surface | Rule |
| :--- | :--- |
| Window, title bar, omnibar, sidebar, status bar | **No gradient** — flat `BG_*` fills only. "Mica" is one solid `#1F1F1F`. |
| Buttons, inputs, toggles, badges, selected rows, tables, scrollbars, focus rings | **No gradient** — hover/press is a single flat step (`ACCENT_HOVER`, `BG_CONTROL_HOVER`). |
| Cards, dialogs, preview pane, toasts | **No gradient, no glow, no soft-shadow ramp.** Depth = 1 px border + one surface step; dialogs get a scrim, not a shadow. |
| Text, icons, logo | **No gradient, ever.** |
| Photo-fallback category tile (§12) | The **only** permitted gradient: one 2-stop, same-hue gradient, ≤ 12 % lightness delta, defined as `Theme` token pairs (`TILE_GRAD_A`/`TILE_GRAD_B` per category). Never inline. |

Also prohibited: animated, radial, multi-stop, and accent-colored gradients. The 1080p and
1366×768 review passes include a gradient spot-check so a stray one cannot ship.

### 3.5 Photo-fallback tile gradients (dark theme)

| Category | Stop A | Stop B |
| :--- | :--- | :--- |
| `CAMERA` | `#4E4636` | `#5E5544` |
| `LENS` | `#37424A` | `#43505A` |
| `LAPTOP` | `#453A45` | `#554855` |
| `AUDIO` | `#4A3530` | `#5A413B` |
| `TOOL` | `#3F4034` | `#4E5040` |
| `OTHER` | `#3B3B3B` | `#484848` |

Glyph on tile: the category glyph at 48 px, `TEXT_PRIMARY` at 55 % alpha, centered.
Light theme uses one neutral pair `#EDE6D9 → #F6F1E7` for all categories with the glyph in
`TEXT_PRIMARY` at 45 % alpha; the category is then carried by the badge/chip next to it.

## 4. Typography

| Role | Size / weight |
| :--- | :--- |
| Screen title | 20 px, semibold |
| Section heading | 16 px, semibold |
| Body / table | 14 px, regular |
| Label / caption | 12 px, regular, secondary color |
| KPI number | 28 px, semibold |
| Badge word | 12 px, semibold |

Font stack (resolved once at startup by `Fonts.java`, first available wins):

**Segoe UI Variable Display** (≥ 20 px) → **Segoe UI Variable Text** (≤ 16 px) →
**Segoe UI** → **Inter** (bundled at `desktop-app/resources/fonts/`, registered via
`GraphicsEnvironment.registerFont`) → `SansSerif`. Bundled Inter is the cross-machine
fallback so non-Windows-11 machines still render consistently. Monospace
(`Consolas` → `Monospaced`) for IDs, serial numbers, and money in tables — tabular
numerals only. Body line height 1.3–1.4; no all-caps styling anywhere.

## 5. Window Chrome & Shell

Single main window (role-aware), custom-drawn chrome, square outer corners (§11 D9).
The login screen uses the same title bar but hides omnibar, sidebar, and status bar.

```
┌──────────────────────────────────────────────────────────────┐  32 px  title bar
│ ▣ KitNapper   Catalog                        ☾   ─   ▢   ✕    │  drag region
├──────────────────────────────────────────────────────────────┤  48 px  omnibar
│ Home › Catalog        [+ New] [Filters ▾]    ⌕ Search   ▤|☰   │
├──────────────┬───────────────────────────────┬───────────────┤
│              │  Name        Category  Status │  preview pane │
│  Sidebar     │  Sony A7 IV    Camera   ●     │  [photo 16:9] │
│  240 px      │  Canon EOS R6  Camera   ●     │  Sony A7 IV   │
│              │  ...                          │  ₱150,000.00  │
│              │                               │  [Check out]  │
├──────────────┴───────────────────────────────┴───────────────┤  32 px  status bar
│ 12 items · 1 selected · ₱1,120,000.00 total                   │
└──────────────────────────────────────────────────────────────┘
```

- **Title bar (32 px):** 16 px app glyph + `KitNapper` wordmark (TEXT_PRIMARY), current
  screen name (TEXT_SECONDARY), spacer, theme toggle (sun/moon glyph button), then window
  controls at 46×32 hit targets — close hover `CLOSE_HOVER` with white glyph, min/max
  hover `BG_CONTROL`. Everything except the controls is a drag region; double-click
  toggles maximize. No tabs.
- **Omnibar (48 px):** breadcrumb `Home › Screen` (active crumb `ACCENT_TEXT`; `Home`
  returns to the role's default screen), contextual commands (max 3, then overflow),
  right-aligned search (240 px, expands to 320 px on focus) and, on screens that have
  both, the table/cards layout toggle as a segmented control.
- **Sidebar (240 px):** grouped nav — Home group (Catalog, My Loans) for borrowers;
  Manager group (Dashboard, All Loans, Overdue, Penalties, Inventory) for managers;
  footer holds Settings / Data, the signed-in user chip (name + role), and Sign out.
  Selected item: 3 px amber left bar + `ACCENT_TINT` fill; hover `BG_ELEVATED`.
- **Status bar (32 px):** left = item/loan counts, then selection summary; right =
  aggregate in monospace (total replacement value on catalog/inventory, accrued-fee total
  on loan screens, total owed on Overdue).
- **Content:** page header (screen title 20 px + one-line subtitle 12 px secondary),
  8 px spacing grid, max width unconstrained.
- **Dialogs:** modal, centered, default 480 px wide, `BG_ELEVATED`, 8 px radius, over a
  `rgba(0,0,0,0.4)` scrim.

### Screen inventory

| Screen | Role | Primary content |
| :--- | :--- | :--- |
| Login | both | Centered 420 px card on `BG_APP` (email, password, sign-in, inline error; collapsed "Demo accounts" hint) |
| Dashboard | Manager | 3 KPI cards (out now, overdue, unpaid penalties), overdue table, recent activity |
| Catalog | both | Omnibar (search, filters, layout toggle); **details table + preview pane by default**; cards view optional via the toggle |
| My Loans | Borrower | Active/Overdue/Returned segmented control; loans table + loan preview with fee breakdown |
| All Loans | Manager | Sortable table + loan preview; return action |
| Overdue | Manager | Overdue table, longest-overdue first, days-late column, live accrued fees |
| Penalties | Manager | Per-loan penalty list + paid/waive actions + preview |
| Inventory | Manager | Items table + preview (photo, metadata, Edit/Flag/Decommission); "Add item" command with photo picker |
| Settings / Data | Manager | Theme, save-file location, photo-folder location, reset demo data |

## 6. Components

- **Buttons:** primary = amber fill (`ON_ACCENT` text); secondary = `BG_CONTROL` fill +
  `BORDER_SUBTLE` outline; subtle = transparent, hover `BG_ELEVATED`; destructive =
  transparent + `DANGER` border/text. All 32 px (28 px compact in table rows), radius 4,
  8/12 px padding, icon+label optional. Never more than one primary per view.
- **Text field / search:** 32 px, radius 4, `BG_CONTROL` fill, 1 px `BORDER_SUBTLE`
  outline, bottom border `BORDER_STRONG`; on focus the bottom border becomes 2 px
  `ACCENT`. Placeholder `TEXT_MUTED`; inline validation text below in `DANGER`.
- **Checkbox:** 18 px, radius 3, `BG_CONTROL` fill, amber check. **Toggle switch:**
  40×20, checked = `ACCENT` track + `ON_ACCENT`-contrast knob; unchecked =
  `BORDER_STRONG` track + `TEXT_PRIMARY` knob.
- **Segmented control:** 32 px, radius 4 container `BG_CONTROL`, selected segment
  `BG_ELEVATED` + 1 px `BORDER_STRONG`, 2 px gap between segments.
- **Filter chips / combo:** pill, `BG_CONTROL` fill, chevron glyph; chips collapse into
  the omnibar "Filters" flyout on narrow windows.
- **Status badge:** per §3.3 (amber-tinted pill + dot + word).
- **Tables:** 40 px rows, 36 px header (TEXT_SECONDARY, 12 px, sort chevron on the active
  column), 1 px `BORDER_SUBTLE` row separators, no vertical gridlines, hover `HOVER_ROW`,
  selection `ACCENT_TINT`, resizable columns, right-aligned money, monospace IDs.
- **Cards (catalog "cards" view):** 8 px radius, `BG_SURFACE`, 1 px `BORDER_SUBTLE`,
  16:9 media block (uploaded photo or fallback tile), name 14 px semibold, category 12 px
  secondary, status badge, monospace value. Hover raises the border to `BORDER_STRONG`
  only — no shadow, no gradient.
- **Preview pane (280 px):** 16:9 media, title 16 px semibold, metadata rows (12 px
  secondary label / 14 px value), primary action pinned at the bottom with secondary
  actions beneath; empty state when nothing is selected.
- **Dialogs:** 480 px default, radius 8, title 20 px, body 14 px, footer right-aligned
  with the primary rightmost; `Enter` confirms, `Esc` cancels; scrim per §5.
- **Toasts:** bottom-right, 320 px, `BG_ELEVATED` + `BORDER_SUBTLE`, radius 8, icon +
  text, auto-dismiss after 4 s, max 3 stacked.
- **Empty states:** glyph (`TEXT_MUTED`), 14 px explanation, 12 px secondary line, and
  the action that fixes it ("No gear matches this filter. Clear filters.").
- **Scrollbars:** thin overlay style — transparent track, 4 px thumb (`#5A5A5A` dark /
  `#C6C6C6` light), radius 2, widening to 8 px on hover; no layout space reserved.
- **Tooltips:** 12 px text, `BG_ELEVATED`, 1 px `BORDER_SUBTLE`, radius 4, 500 ms delay.
- **Focus visuals:** 2 px `ACCENT` rectangle with a 1 px inner gap (Win11 focus rect),
  always visible on keyboard focus.

## 7. Motion & Interaction

Fluent durations: fast **83 ms**, normal **167 ms**, slow **250 ms**; decelerate easing
`cubic-bezier(0.1, 0.9, 0.2, 1.0)`, implemented with Swing `Timer`.

Allowed: dialog open/close fade + 98→100 % scale (fast), toast slide-up 8 px + fade,
screen transition fade (120 ms), theme crossfade (167 ms). Hover/press states change
instantly — no hover animation. No bounce, no overshoot, no animated gradients. Skeleton
pulse timing is specified in §13 when that plan is written. Destructive actions
(decommission, waive, reset, photo replace) require confirmation dialogs.

## 8. Accessibility

- Text contrast ≥ 4.5:1 in both themes for: `TEXT_PRIMARY`/`TEXT_SECONDARY` on every
  `BG_*`, `ACCENT_TEXT` on `BG_APP`/`BG_SURFACE`, `ON_ACCENT` on `ACCENT`. `TEXT_MUTED`
  is reserved for ≥ 12 px captions and targets ≥ 3:1 (documented large-text exception).
- Full keyboard operation: tab order follows visual order; `Enter` submits, `Esc` closes
  dialogs and flyouts; `Space` toggles; arrow keys navigate tables/combos; `Ctrl+F`
  focuses search. Window controls are reachable via keyboard.
- The §6 focus rectangle is always visible; never removed for aesthetics.
- Status must never be conveyed by color alone — badges always carry the dot **and** the
  word (plus the wrench glyph for maintenance).
- Money always `₱1,234.00`; dates always `yyyy-MM-dd` — both defined once in
  `Formats.java`.
- Custom controls (title bar buttons, toggle, segmented control) expose proper
  `AccessibleContext` names and roles.
- Verify at 100 % / 125 % / 150 % Windows display scaling and at 1366×768.

## 9. Geometry & Density

- **Radii:** controls 4 px; cards, dialogs, preview pane, tiles 8 px; badges 10 px (pill);
  window outer corners 0 px (square).
- **Strokes:** 1 px everywhere; 2 px only for focus rectangles and focused input bottoms.
- **Spacing scale:** 4 / 8 / 12 / 16 / 24 / 32 px.
- **Heights & widths:** title bar 32, omnibar 48, status bar 32, sidebar 240, preview 280,
  buttons/inputs 32 (28 compact), table row 40, table header 36, badge 20, KPI card ≥ 96.
- **Window:** minimum 1024×640; opens at 1280×800 (user-resizable, maximizable).
- **Elevation:** border + one surface step; no drop shadows anywhere (scrim + border for
  dialogs, border for toasts).

## 10. Iconography

Resolution order (probed once by `Fonts.java`): **Segoe Fluent Icons** (Windows 11) →
**Segoe MDL2 Assets** (Windows 10) → bundled Inter / Unicode fallback glyphs. System icon
fonts are not a library violation; nothing is shipped beyond the Inter files. Default
size 16 px, command bar 20 px, fallback-tile glyph 48 px, color `TEXT_SECONDARY` (never
gradient-filled).

| Action | Fluent codepoint | Unicode fallback |
| :--- | :--- | :--- |
| Home / nav | `U+E80F` | `⌂` |
| Search | `U+E721` | `⌕` |
| Add / New | `U+E710` | `＋` |
| Settings | `U+E713` | `⚙` |
| Filter | `U+E71C` | `▽` |
| Sort | `U+E8CB` | `↕` |
| Chevron down | `U+E70D` | `⌄` |
| Chevron right | `U+E76C` | `›` |
| Back | `U+E72B` | `←` |
| Checkmark | `U+E73E` | `✓` |
| Cancel / close | `U+E711` | `✕` |
| Edit | `U+E70F` | `✎` |
| Delete | `U+E74D` | `✕` |
| Warning | `U+E7BA` | `⚠` |
| Repair (maintenance) | `U+E90F` | `⚒` |
| Theme toggle | `U+E706` / `U+E793` | `☀` / `☾` |
| Window minimize / maximize / restore | `U+E921` / `U+E922` / `U+E923` | `─` / `▢` / `❐` |

Category glyphs (fallback column is authoritative if a codepoint renders wrong):
`CAMERA` ▣, `LENS` ◎, `LAPTOP` ▭, `AUDIO` ♪, `TOOL` ⚒, `OTHER` ▦.

Codepoints are best-effort from Microsoft's Segoe Fluent Icons list and must be verified
against rendered output during M2; if a codepoint is wrong or missing on a machine, the
fallback glyph column wins.

## 11. Swing Feasibility Notes

| Windows 11 feature | Swing reality | Our approach |
| :--- | :--- | :--- |
| Mica / acrylic blur | Not reachable (no DWM APIs) | Flat `BG_APP` — one solid color |
| Rounded window corners + native shadow | Undecorated frames get neither | Square outer corners + 1 px `BORDER_SUBTLE` (decision D9) |
| Snap layouts / `Win+←/→` | Lost with custom chrome | Accepted limitation, documented here and in the README |
| Segoe UI Variable / Fluent Icons | System fonts on Win11 / Win10 | Probe at startup; documented fallback chains (§4, §10) |
| Thin overlay scrollbars | Swing scrollbars are blocky | `BasicScrollBarUI` subclass, always-thin, no overlay animation |
| Per-pixel translucency | Supported but unnecessary | Not used |
| Window drag / resize / maximize | Manual | `WindowChrome` owns: drag, double-click maximize, 8 px edge resize bands, min size, restore bounds |
| Multi-monitor / DPI change mid-session | Resize quirks | Accept; verify at 100 / 125 / 150 % |

Painting notes: custom surfaces use `setOpaque(false)` with cached `RoundRectangle2D`
shapes; repaint only dirty rows/regions; images decode off the EDT (`SwingWorker`) with
skeleton placeholders (§13); `UIManager` defaults are set once in `Theme.apply()`.
`WindowChrome` is split from `TitleBar` painting to respect the 200-line file cap.

## 12. Photo Uploads

**Scope change (2026-11-14):** the PRD §8 non-goal "Image/file uploads" is **overridden**
— optional per-item photos are now in scope (see §14 and `decisions.md` §8).

- **Storage:** images live in `kitnapper-images/` next to `kitnapper-data.json`, in the
  application folder at runtime. Never inside the repo; never committed. Filename
  `<equipmentId>.jpg` — one photo per item; re-uploading replaces it.
- **Import:** `JFileChooser` → validate extension (`png`, `jpg`, `jpeg`, `gif`, `bmp`)
  and source size (≤ 20 MB) → decode via `ImageIO` → downscale to 1024 px longest edge
  (`Graphics2D`, bilinear) → re-encode JPEG quality 0.85, alpha flattened onto
  `BG_SURFACE` → written atomically (tmp file + move, same pattern as the JSON save).
- **Model:** optional nullable `photoFile` on `Equipment`; JSON field `photoFile`.
  `schemaVersion` stays 1 — the field is additive, so existing save files load unchanged
  (decision D8).
- **Display:** catalog cards use a 160 px thumbnail, the preview pane 640 px; both decode
  off the EDT and cache. A missing or corrupt file falls back to the category tile
  (§3.5) with no error surface.
- **Lifecycle:** decommissioned items keep their photo (audit history). Reset demo data
  wipes `kitnapper-images/` entirely — the seed dataset ships with no photos (D11). A
  load-time orphan sweep deletes images whose id is not in the dataset.
- **Privacy:** photos never leave the machine (networking is out of scope).

## 13. Skeleton Loading States — PLAN PENDING (VERY IMPORTANT)

> ⚠ **BLOCKING TODO — this section is a placeholder, not a specification. It must be
> fully written with the user before UI implementation (M2) starts. It is not optional
> polish.**
>
> Minimum scope the plan must cover:
>
> - Which surfaces get skeletons: photo tiles / preview pane while `ImageIO` decodes;
>   initial launch while the JSON loads; any screen query that can exceed ~100 ms.
> - Timing thresholds, minimum display duration, and the transition out.
> - Visual form under the §3.4 gradient ban: flat blocks + subtle opacity pulse —
>   **no shimmer sweep** — 4 px radius, `BG_ELEVATED` at reduced alpha.
> - Reduced-motion behavior (static blocks, no pulse).
> - Interaction rules: skeletons are never focusable, never announced as content; an
>   accessible "Loading" state is exposed instead.
> - Failure/timeout fallback: skeleton → error or empty state, never a permanent skeleton.
> - Code placement (`SkeletonPanel`, per-component skeleton renderers) and the milestone
>   that activates it.

## 14. Decisions Log

### 14.1 v1.0 pass — warm dark dashboard (history, superseded)

Resolved 2026-11-14 with the user; retained for traceability. Superseded by §14.2 except
where v2.0 explicitly keeps the answer.

| # | Question | Answer (v1, superseded) |
| :-- | :--- | :--- |
| 1 | Accent color | Amber `#F0A32E` — **kept in v2** |
| 2 | Background depth | Warm brown-black `#2C1E16` base — **replaced by the neutral Win11 ramp** |
| 3 | Font | Inter, bundled in-app — **now the fallback behind Segoe UI Variable** |
| 4 | Density | Airy cards for catalog, dense tables for loans/inventory — kept |
| 5 | Dashboard widgets | KPIs + overdue list + recent activity — kept |
| 6 | Catalog layout | Cards/table toggle, defaults to cards — **default is now table + preview pane** |
| 7 | Logo | Custom icon; wordmark placeholder until team art lands — kept |
| 8 | Microcopy tone | Mixed by role — plain for borrowers, formal for manager surfaces — kept |
| 9 | Target resolution | 1080p, 1366×768 sanity check — kept |
| 10 | Window model | Single main window with modal dialogs — kept (chrome is now custom) |
| 11 | Light mode | Dark and light, two token sets, toggle — kept |
| 12 | Motion | Fades on transitions only, ≤ 150 ms — **retuned to Fluent durations (§7)** |

Earlier rows: 2026-11-11 dark-dashboard direction approved; no third-party libraries →
custom Swing theme, Unicode icons; details deferred to §9; 2026-11-14 design inspirations
added (files.community, shelf.nu); implementation plan written (`M0–M6`, D1–D6).

### 14.2 v2.0 — Windows 11 / Fluent direction (2026-11-14)

| Decision | Detail |
| :--- | :--- |
| **D-Style** | Visual language is Windows 11 / Fluent (Files app): neutral surface ramp (§3), amber accent retained, Segoe UI Variable type stack (§4), custom title bar + omnibar + sidebar + status bar shell (§5). Supersedes the warm `#2C1E16` ramp and the Inter-first stack; all other v1 answers stand unless listed below. |
| **D-Chrome** | Custom title bar, **no tabs**; square outer corners; no Mica blur, no snap layouts (§11). Sidebar is the only navigation; the omnibar breadcrumb shows location. |
| **D-Catalog** | Catalog and Inventory default to **details table + preview pane**; the layout toggle switches the left pane to cards (overrides v1 Q6's cards default). |
| **D-Badges** | Amber-derived badge system: amber-tinted pill + semantic dot + word, wrench glyph on maintenance (§3.3). |
| **D-Gradient** | Gradients restricted to the single photo-fallback category tile; every other surface is flat (§3.4). |
| **D-Photos** | Image uploads moved **into scope**, reversing the PRD §8 non-goal: `kitnapper-images/` beside the save file, 1024 px JPEG q0.85, 20 MB source cap, png/jpg/jpeg/gif/bmp, reset wipes the folder, seed ships without photos (§12). |
| **D-Schema** | `schemaVersion` stays 1; `photoFile` is a nullable additive field so old saves load unchanged. |
| **D-Skeleton** | Skeleton loading states are a **blocking open plan** (§13) — must be written before M2 begins. |
