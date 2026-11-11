# KitNapper — Design Specification

> **Status:** direction agreed (dark dashboard), details pending. The open questions at the end are the agenda for the next design pass — do not invent answers before they are confirmed.

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
| `BG_BASE` | Window background | `#0D1117` |
| `BG_SURFACE` | Cards, panels, tables | `#161B22` |
| `BG_ELEVATED` | Hovered rows, dialogs | `#1C2128` |
| `BORDER` | Dividers, input outlines | `#30363D` |
| `TEXT_PRIMARY` | Headings, values | `#E6EDF3` |
| `TEXT_SECONDARY` | Labels, metadata | `#8B949E` |
| `TEXT_MUTED` | Disabled, placeholders | `#6E7681` |
| `ACCENT` | Primary buttons, selection, focus | TBD (§9) |
| `ON_ACCENT` | Text on accent | TBD |

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

## 4. Typography

| Role | Size / weight |
| :--- | :--- |
| Screen title | 22–24 px, bold |
| Section heading | 16 px, semibold |
| Body / table | 13–14 px, regular |
| Label / caption | 11–12 px, regular, secondary color |
| KPI number | 28–32 px, bold |

Font stack must be Swing-safe: `Inter` if installed, else `Segoe UI`, else `SansSerif`. Monospace (`Monospaced`) for IDs, serial numbers, and money in tables.

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

## 9. Open Questions — Next Design Pass

Ask the user these, one at a time, before building UI:

1. **Accent color** — blue, teal, amber, or something else?
2. **Background depth** — near-black (`#0D1117`) or softer slate (`#161B22` base)?
3. **Font** — Inter-style sans, or a system font for a more "native" feel?
4. **Density** — airy cards or compact, data-heavy tables for the catalog?
5. **Dashboard widgets** — which matter most: KPIs, overdue list, recent activity, inventory breakdown?
6. **Catalog layout** — cards, table, or a toggle between both?
7. **Logo** — text-only wordmark, or an icon + wordmark?
8. **Microcopy tone** — formal ("Authentication failed") or plain ("That email and password don't match")?
9. **Target resolution** — 1080p, laptop 1366×768, or larger?
10. **Window model** — single main window with dialogs, or separate windows per screen (Activity 7 style)?
11. **Light mode** — needed, or dark only?
12. **Motion** — none, or subtle fades on panel changes?

## 10. Decisions Log

| Date | Decision |
| :--- | :--- |
| 2026-11-11 | Dark dashboard direction approved |
| 2026-11-11 | No third-party libraries → custom Swing theme, Unicode icons |
| 2026-11-11 | Details deferred; questions captured in §9 |
