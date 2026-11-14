# KitNapper — Design Specification

> **Status:** direction agreed (dark dashboard) and all open questions resolved 2026-11-14 (answers in §9, logged in §10). Implementation should follow the tokens below.

## 1. Brand

| Item | Value |
| :--- | :--- |
| Product name | **KitNapper** |
| Tagline | *Every kit, accounted for.* |
| Logo | TBD (text wordmark is the fallback) |
| Tone of voice | Direct, calm, staff-tool confidence — no exclamation marks, no cute errors |
| Team | YAPPERS — Casiano, Cudia, Villacorza |

## 2. Style Direction

**Dark dashboard.** A staff console, not a consumer app: dark layered surfaces, one accent, generous whitespace, information-dense tables where data matters (loans, inventory) and airy cards where scanning matters (catalog, dashboard KPIs).

Reference mood: linear.app, vercel dashboard, GitHub dark — all achievable in plain Swing.

**Hard constraint:** no third-party libraries (PRD §7.2), so **no FlatLaf, no theme packs, no icon fonts**. The dark look is custom-painted Swing: `UIManager` defaults, custom `TableCellRenderer`s, custom `JPanel` backgrounds, and Unicode glyphs for icons.

## 3. Color Tokens

Name the tokens in code (`Theme.java` constants), never hard-code hex in panels.

| Token | Purpose | Proposed value |
| :--- | :--- | :--- |
| `BG_BASE` | Window background | `#2C1E16` (warm, §9 Q2) |
| `BG_SURFACE` | Cards, panels, tables | `#3A281D` |
| `BG_ELEVATED` | Hovered rows, dialogs | `#4A3526` |
| `BORDER` | Dividers, input outlines | `#5C452F` |
| `TEXT_PRIMARY` | Headings, values | `#F2E8DC` |
| `TEXT_SECONDARY` | Labels, metadata | `#C4AE94` |
| `TEXT_MUTED` | Disabled, placeholders | `#937F69` |
| `ACCENT` | Primary buttons, selection, focus | `#F0A32E` (§9 Q1) |
| `ON_ACCENT` | Text on accent | `#2C1E16` |

**Warm ramp note (2026-11-14):** all dark tokens are warm-shifted to sit naturally on the `#2C1E16` base — do not mix in cool GitHub grays. The accent amber is orange-leaning (`#F0A32E`) so primary buttons stay visually distinct from the amber `UNDER_MAINTENANCE` badge (`#D29922`).

### Status colors (badges)

| Status | Color | Meaning |
| :--- | :--- | :--- |
| `AVAILABLE` | green `#3FB950` | Ready to borrow |
| `LOANED` | blue `#58A6FF` | Currently out |
| `OVERDUE` | red `#F85149` | Past due — needs attention |
| `UNDER_MAINTENANCE` | amber `#D29922` | Temporarily unavailable |
| `DECOMMISSIONED` | muted `#6E7681` | Retired |
| Penalty `UNPAID` | red-tinted | Owed |
| Penalty `PAID` | green | Settled |
| Penalty `WAIVED` | muted | Forgiven |

### Light theme (§9 Q11 — dark and light both)

| Token | Light value |
| :--- | :--- |
| `BG_BASE` | `#F7EFE2` (warm paper) |
| `BG_SURFACE` | `#FFFDF8` |
| `BG_ELEVATED` | `#FFFFFF` |
| `BORDER` | `#E0D2BC` |
| `TEXT_PRIMARY` | `#3B2A1C` |
| `TEXT_SECONDARY` | `#7A6A55` |
| `TEXT_MUTED` | `#A6937B` |
| `ACCENT` | `#C77E14` (darker amber for contrast) |
| `ON_ACCENT` | `#FFFDF8` |

Status colors switch to darker variants in light mode for contrast: green `#2E8B3D`, blue `#1E6BB8`, red `#D64545`, amber `#A87A0C`, muted `#8A7A66`. Every token pair must pass the §8 contrast check in both modes.

## 4. Typography

| Role | Size / weight |
| :--- | :--- |
| Screen title | 22–24 px, bold |
| Section heading | 16 px, semibold |
| Body / table | 13–14 px, regular |
| Label / caption | 11–12 px, regular, secondary color |
| KPI number | 28–32 px, bold |

Font stack (§9 Q3, resolved 2026-11-14): **Inter is bundled in-app** at `desktop-app/resources/fonts/` and registered at runtime via `GraphicsEnvironment.registerFont` — font files are assets, not libraries, so the no-third-party rule holds. Fallback if the files are missing: `Segoe UI`, else `SansSerif`. Monospace (`Monospaced`) for IDs, serial numbers, and money in tables.

## 5. Layout

Single main window (role-aware), fixed sidebar navigation.

```
┌──────────────┬────────────────────────────────────────────┐
│  KitNapper   │  Screen title            [role] [logout]   │
│              ├────────────────────────────────────────────┤
│  ● Catalog   │                                            │
│  ● My Loans  │            content area                    │
│              │   (cards / tables / forms / dashboard)     │
│              │                                            │
└──────────────┴────────────────────────────────────────────┘
```

- Sidebar: product wordmark, nav items, active item indicated by accent bar + elevated background.
- Top bar: current screen title, signed-in name, role badge, logout.
- Content: 8 px spacing grid; max content width not constrained (desktop).
- Dialogs: modal, dark `BG_SURFACE`, used for checkout, return, settle penalty, add/edit equipment.

### Screen inventory

| Screen | Role | Primary content |
| :--- | :--- | :--- |
| Login | both | Email, password, sign-in, error area |
| Dashboard | Manager | KPIs (out now, overdue, unpaid penalties), overdue table, recent activity |
| Catalog | both | Search, category/status filters, item cards or table, checkout action |
| My Loans | Borrower | Active/overdue/returned tabs, accrued fees, penalty list |
| All Loans | Manager | Sortable table, status badges, return action |
| Overdue | Manager | Sorted table with days-late column and live accrued fees |
| Penalties | Manager | Per-loan penalty list, paid/waive actions |
| Inventory | Manager | Add/edit/decommission, maintenance flag |
| Settings / Data | Manager | Reset demo data, save-file location |

## 6. Components

- **Status badge:** pill, tinted background (status color at ~15–20% alpha), solid status text.
- **Buttons:** primary = accent fill; secondary = transparent + border; destructive = red border/text. Never more than one primary per view.
- **Tables:** dark header row, `BORDER` separators, row hover `BG_ELEVATED`, right-aligned money, monospace IDs, zebra stripes optional.
- **Cards (catalog):** item name, category chip, status badge, replacement value, primary action.
- **Inputs:** `BG_SURFACE` fill, `BORDER` outline, accent outline on focus; inline validation text below the field.
- **Empty states:** one line of explanation + the action that fixes it ("No gear matches this filter. Clear filters.").
- **Toasts/feedback:** bottom-right, auto-dismiss, used for save/settle confirmations.

## 7. Motion & Interaction

- No animation library. If motion is used, keep it to Swing `Timer`-driven fades ≤ 150 ms.
- Hover states on rows and buttons; keyboard focus ring always visible (accessibility).
- Destructive actions (decommission, waive, reset) require confirmation dialogs.

## 8. Accessibility

- Text contrast ≥ 4.5:1 against its background (verify token pairs).
- Full keyboard operation: tab order follows visual order; `Enter` submits, `Esc` closes dialogs.
- Status must never be conveyed by color alone — badges always include the word.
- Money always formatted `₱1,234.00`; dates always `yyyy-MM-dd` or `MMM d, yyyy` (pick one, use everywhere).

## 9. Open Questions — RESOLVED 2026-11-14

All twelve questions were answered with the user in a single session; no guesses were made.

| # | Question | Answer |
| :-- | :--- | :--- |
| 1 | Accent color | **Amber `#F0A32E`** — orange-leaning so primary buttons stay distinct from the `#D29922` maintenance badge |
| 2 | Background depth | **Warm brown-black `#2C1E16`** base; the whole token ramp is warm-shifted (§3) |
| 3 | Font | **Inter, bundled in-app** (`desktop-app/resources/fonts/`, registered via `GraphicsEnvironment`) |
| 4 | Density | **Airy cards for catalog, dense tables** for loans/inventory |
| 5 | Dashboard widgets | **KPIs + overdue list + recent activity** (activity derived from loan/penalty records — no new persistence format) |
| 6 | Catalog layout | **Cards/table toggle** in the toolbar; defaults to cards; session preference only |
| 7 | Logo | **Custom icon**; text wordmark placeholder until the team's art lands |
| 8 | Microcopy tone | **Mixed by role** — plain for student-facing validation ("That email and password don't match."), formal for manager/audit surfaces ("Authentication failed.") |
| 9 | Target resolution | **1080p** (with a 1366×768 sanity check during the SRS §6.2 walkthrough) |
| 10 | Window model | **Single main window with modal dialogs** |
| 11 | Light mode | **Dark and light** — two token sets (§3), a toggle, per-pair contrast verification |
| 12 | Motion | **Fades on transitions only** (login→main, dialog open/close), ≤150 ms Swing `Timer` |

## 10. Decisions Log

| Date | Decision |
| :--- | :--- |
| 2026-11-11 | Dark dashboard direction approved |
| 2026-11-11 | No third-party libraries → custom Swing theme, Unicode icons |
| 2026-11-11 | Details deferred; questions captured in §9 |
| 2026-11-14 | All 12 §9 questions resolved with the user (answers in §9): amber accent `#F0A32E`, warm `#2C1E16` base with full warm ramp, Inter bundled in-app, cards+dense-tables, KPI/overdue/activity dashboard, catalog toggle, custom logo icon later, mixed-by-role microcopy, 1080p target, single window + dialogs, dark + light themes, transition-only fades |
