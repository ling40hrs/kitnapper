# Building a deck as HTML + CSS → PDF

A portable playbook for the pipeline behind `assets/kitnapper-presentation.pdf`.
Nothing here is KitNapper-specific except the worked example in `deck/`; the
commands work for any document that has to come out as an exact-size PDF on a
machine with no InDesign, no LaTeX and no third-party runtime.

---

## 1. Tech stack

Everything below was measured on Ubuntu 24.04.4, 2026-11-24. The deck pipeline
shares **no** dependency with the application it documents.

| Layer | Tool | Version | Role | Needed for |
| :-- | :-- | :-- | :-- | :-- |
| Document model | HTML5 + CSS 2.1/3 | — | the deck is a stylesheet, not a slide program | everything |
| Renderer | **WeasyPrint** | 70.0 (pip) | HTML/CSS → PDF, exact `pt` page size, font embedding | the build |
| Runtime | Python | 3.12.3 | WeasyPrint is a Python library | the build |
| System libs | Pango ≥ 1.44, GDK-Pixbuf | distro | text shaping and image decoding, behind WeasyPrint | the build |
| Build driver | POSIX `sh` | — | concatenates fragments, calls WeasyPrint (§4) | the build |
| Font tooling | **fontTools** | 4.67.0 (pip) | variable→static instancing, subsetting, coverage checks (§6) | one-off, when preparing fonts |
| Image tooling | **Pillow** | pip | luminance keying the logo to alpha (§7.1) | one-off, when preparing art |
| PDF inspection | **poppler-utils** | distro | `pdfinfo`, `pdffonts`, `pdftoppm`, `pdftotext` (§10) | every verification pass |

Optional substitutes: **Chromium via Playwright** for the renderer when a design
needs real flexbox/grid, and `pyftsubset`'s CLI in place of the fontTools API —
both are drop-in swaps, see §12.

**Stack of the project the deck belongs to**, for contrast: Java 17 + Swing,
JDK-only, NetBeans Ant, **no third-party libraries at all**. The deck generator
deliberately lives outside that build — it adds no Java dependency to the app, and
the app's own "no third-party library" rule stays intact because a PDF deliverable
is not shipped code.

---

## 2. Why this pipeline

| Option | Why not |
| :-- | :-- |
| LaTeX / Beamer | Excellent output, but every design change is a fight with packages; no CSS. |
| PowerPoint via python-pptx | Fragile layout math, and the PDF export depends on Office. |
| Headless Chrome (Playwright `page.pdf`) | Full CSS support — but Chrome's print engine rewrites font names, ignores some `@page` sizing, and adds its own header/footer defaults; output is less predictable page-for-page. Use it when you need real flexbox/grid (see §8). |
| **WeasyPrint** | Real CSS 2.1 + a useful slice of CSS 3, exact `pt` page sizes, embeds fonts by default, pure Python, one command. The price: partial flexbox (§8). |

WeasyPrint is the right default when the design is *absolute-positioned
compositions on a fixed canvas* — which is what a slide is.

---

## 3. Install

```bash
sudo apt install -y libpango-1.0-0 libpangoft2-1.0-0 libgdk-pixbuf-2.0-0 poppler-utils
python3 -m pip install --user weasyprint fonttools pillow
```

`fonttools` and `pillow` are only needed for the font and image steps (§6, §7).
The PDF build itself needs only WeasyPrint.

---

## 4. Repository layout

One HTML file per page, one stylesheet set, one glue script. Splitting per page
keeps every file small enough to reason about and makes a single slide cheap to
re-render.

```
deck/
├── build.sh              glue + render (27 lines)
├── base.css              page frame, palette, type scale
├── components.css        cream cards, numbered steps
├── panels.css            cream panels, tables, inline class names
├── slides.css            per-slide coordinates
├── slide-01-title.html
├── slide-02-desk.html
├── …
├── art/                  logo, SVG marks
└── fonts/                static TTFs + OFL licence texts
```

### The glue script

WeasyPrint takes exactly one document, so the script concatenates. `printf` is
used for the wrapper so no stray newline ever lands before `<!DOCTYPE html>`.

```sh
#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"

OUT=../assets/deck.pdf
TMP=build.tmp.html
trap 'rm -f "$TMP"' EXIT

{
    printf '<!DOCTYPE html>\n<html lang="en">\n<head>\n<meta charset="utf-8" />\n'
    printf '<title>Title</title>\n<style>\n'
    cat base.css components.css panels.css slides.css
    printf '</style>\n</head>\n<body>\n'
    for slide in slide-*.html; do cat "$slide"; done
    printf '</body>\n</html>\n'
} > "$TMP"

python3 -m weasyprint "$TMP" "$OUT"
```

Slide files are **fragments** (`<section class="slide"> … </section>`), not full
documents — no repeated `<head>` to drift out of sync.

---

## 5. Page geometry

```css
@page { size: 960pt 540pt; margin: 0; }
html, body { margin: 0; padding: 0; }
.slide {
    position: relative;
    width: 960pt; height: 540pt;
    overflow: hidden;          /* one stray pt must never create page 12 */
    page-break-after: always;
    background: linear-gradient(163deg, #2e2216 0%, …);
}
.slide:last-child { page-break-after: auto; }
```

- **Use `pt`, not `px`.** A 16:9 deck is `960 × 540 pt`; PowerPoint widescreen is
  the same ratio in points, so annotation and reviews match. `px` in WeasyPrint
  is 1/96 in, which is a different grid and invites rounding drift.
- **Fixed height + `overflow: hidden`.** Every page must be exactly one page.
  Overflow silently becomes an extra page in the middle of the deck, and the
  page count is the first thing to check.
- Inside a slide, position everything absolutely (`left/top/width` in `pt`).
  Absolute coordinates are the only layout model that survives renderer
  differences intact.
- The footer page number is written by hand in each fragment. Automatic
  `counter(page)` works in `@page`, but it cannot be styled per slide and cannot
  be suppressed on the title page.

---

## 6. The font pipeline

### 6.1 Never ship variable fonts or woff2

WeasyPrint 70 handles variable axes badly (one instance wins, weights collapse)
and woff2 support is conditional. Ship **static TTFs, already at the weights you
use**, with `@font-face` per weight.

### 6.2 Get static instances from a variable font

Google Fonts ships `Montserrat[wght].ttf` as a variable font. Instantiate each
weight, which rewrites the name table too (`updateFontNames=True`) so the two
instances can coexist:

```python
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

for weight, name in ((400, "Regular"), (700, "Bold")):
    f = TTFont("MontserratVar.ttf")
    instantiateVariableFont(f, {"wght": weight}, updateFontNames=True)
    f.save(f"Montserrat-{name}.ttf")
```

Static builds published by the type foundry work too and are simpler — Open Sauce
One ships `fonts/ttf/OpenSauceOne-{Light,Regular,Bold}.ttf` in its repo. Prefer
those when they exist; instance only when the upstream is variable-only.

### 6.3 Subset, but keep the glyphs you actually print

A full Montserrat is ~374 KB per weight. Subsetting to Latin + punctuation + the
marks the deck uses brings it to ~83 KB:

```bash
pyftsubset Montserrat-Regular.ttf \
  --unicodes="U+0020-007E,U+00A0-00FF,U+2010-2027,U+20AC,U+20B1,U+2190-2193,U+2212" \
  --layout-features="kern,liga,tnum" \
  --output-file=Montserrat-Regular.ttf
```

`U+20B1` is the peso sign; `U+2190-2193` are the arrows; `U+2212` is the true
minus. **Check your own copy for the characters you rely on before subsetting** —
dropping a codepoint does not error, it silently substitutes another font.

```python
# Does this font actually have the glyph?
from fontTools.ttLib import TTFont
cmap = TTFont("Montserrat-Regular.ttf").getBestCmap()
print(0x20B1 in cmap)     # → True / False
```

### 6.4 Declare them

```css
@font-face { font-family: "Mont"; src: url("fonts/Montserrat-Regular.ttf"); font-weight: 400; }
@font-face { font-family: "Mont"; src: url("fonts/Montserrat-Bold.ttf"); font-weight: 700; }
@font-face { font-family: "Sauce"; src: url("fonts/OpenSauceOne-Light.ttf"); font-weight: 300; }
body { font-family: "Sauce", "Mont", sans-serif; }
```

### 6.5 The fallback-chain trick for missing glyphs

Google Fonts' "latin" subsets are aggressively trimmed: no `₱`, no arrows. If
your body font lacks a glyph, the renderer falls through to the *last* family —
often DejaVu, whose metrics wreck a tight layout and whose look betrays the whole
page.

Fix: make the **display font** the fallback, because it has full coverage for the
few characters at issue.

```css
body { font-family: "Sauce", "Mont", sans-serif; }   /* ₱ falls through to Mont */
```

Then prove it:

```bash
pdffonts deck.pdf
# NAME                        TYPE        ENC        EMB SUB UNI
# CHUNGA+Sauce-Light          CID TrueType Identity-H yes yes yes
# CQKKTC+Mont-Bold            CID TrueType Identity-H yes yes yes
# IHLHSW+Sauce                CID TrueType Identity-H yes yes yes
# SLNYRK+Mont                 CID TrueType Identity-H yes yes yes
# OIWASN+Sauce-Bold           CID TrueType Identity-H yes yes yes
```

**Any font in that list that you did not declare is a bug.** `DejaVuSans` there
means a glyph was missing and got silently substituted.

### 6.6 Licences

OFL fonts may be redistributed, but the licence text must travel with them. Commit
the OFL next to the TTFs (`deck/fonts/OFL-*.txt`); it costs 4 KB and it is the
whole compliance story.

---

## 7. Images

### 7.1 Lifting a logo out of a source PDF

Reference decks usually contain the logo as a vector or a huge raster on a coloured
field. Render the page large, crop, then key the background to alpha:

```bash
# 300 dpi, page 1 only → a 6000 px wide image
pdftoppm -r 300 -f 1 -l 1 -png reference.pdf page
```

```python
from PIL import Image

im = Image.open("page-01.png").convert("RGBA")
im = im.crop((x, y, x + w, y + h))          # bounds of the wordmark
px = im.load()

# Luminance key: anything brighter than `cut` becomes opaque white,
# anything darker becomes transparent. Keeps antialiased edges soft.
cut, soft = 90, 40
for j in range(im.height):
    for i in range(im.width):
        r, g, b, _ = px[i, j]
        lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
        a = int(max(0, min(255, (lum - cut) / soft * 255)))
        px[i, j] = (255, 255, 255, a)

im.save("logo-white.png", optimize=True)     # 945×146, ~26 KB
```

Always test the key against **both** background colours it will sit on — a white
wordmark over amber and over chocolate exposes halo fringing that a single test
misses.

### 7.2 Icons as SVG, generated by script

WeasyPrint has no `clip-path` and no blur, so shapes must be real SVG files.
Generating them from a few lines of Python beats hand-typing polygon points:

```python
import math
def hexpts(cx, cy, r, rot=0):
    return " ".join(
        f"{cx + r*math.cos(math.radians(60*i-30+rot)):.1f},"
        f"{cy + r*math.sin(math.radians(60*i-30+rot)):.1f}" for i in range(6))
```

### 7.3 Screenshots

Do not restyle a screenshot. Frame it: a cream mat with padding and a real
`box-shadow`, rounded corners on the image, a caption strip in the mat. That is
what makes a dark application window read as *content* instead of a hole in a
light page.

---

## 8. What WeasyPrint 70 actually supports

Measured, not guessed.

| Feature | Works | Notes |
| :-- | :-- | :-- |
| `position: absolute` + `pt` coords | ✅ | the backbone of the whole deck |
| `linear-gradient`, `radial-gradient` | ✅ | gradients with stops and angles are fine |
| `border-radius`, `box-shadow` | ✅ | multiple shadows fine |
| `transform: rotate/scale` | ✅ | use it on SVGs for corner shards |
| `@font-face`, font subsetting | ✅ | but static weights only |
| `object-fit: contain` | ✅ | `cover` works too, and will crop (§9.4) |
| flexbox row layout | ⚠️ | single line only; `gap` unreliable |
| `flex-wrap` | ❌ | silently ignored — items stack (§9.1) |
| `margin-top: auto` in a flex column | ❌ | does not pin a footer (§9.3) |
| `filter: blur()`, `backdrop-filter` | ❌ | use a radial gradient for glow |
| `clip-path` | ❌ | use an SVG file |
| CSS grid | ❌ | build the grid from `inline-block` |

Rule of thumb: **absolute positioning for structure, inline-block for grids,
SVG for shapes, gradients for glow.** Do not ask WeasyPrint to be a browser.

---

## 9. The five layout bugs, and their fixes

Every one of these failed *silently* — the PDF built, the page count was right,
and the damage was only visible in a render.

### 9.1 `flex-wrap` never wraps in an absolutely-positioned flex container

A `display: flex; flex-wrap: wrap` container with `position: absolute` lays out
as a single line and overflows vertically. The remainder is clipped by
`overflow: hidden`, so a 2×2 grid becomes one column and half the cards vanish.

```css
/* broken */
.cards { display: flex; flex-wrap: wrap; gap: 14pt; }

/* fixed */
.cards { display: block; font-size: 0; }     /* kills inter-element whitespace */
.cards .card { display: inline-block; vertical-align: top; width: 414pt;
               margin: 0 12pt 14pt 0; }
```

`font-size: 0` on the parent works because every child declares its own size.

### 9.2 The trailing inline-block margin counts toward the line width

This is the one that cost the most time. `4 × (204pt + 12pt) = 864pt` does not
fit an 856pt box — because the **last** element's `margin-right` is still part of
the line box. The symptom is not an error; it is a 3-per-row grid with an extra
row pushed past the page edge.

```css
/* 4 tiles across an 856pt content box: 4*(200+12) = 848 ≤ 856 ✅ */
.tile { width: 200pt; margin-right: 12pt; }
```

### 9.3 `margin-top: auto` does not pin a footer inside a flex column

Intended: card text at the top, evidence strip glued to the bottom. WeasyPrint
computes the auto margin as zero, so the strip overlaps the paragraph.

```css
.card { position: relative; padding: 15pt 16pt 52pt; }      /* 52pt = strip zone */
.strip { position: absolute; left: 16pt; right: 16pt; bottom: 14pt; }
```

### 9.4 `object-fit: cover` crops content, not just whitespace

Normalising four screenshots to one height with `cover` cut a dialog's title and
its Cancel button off. `contain` kills the crop but creates letterbox bands that
look worse than ragged heights.

What worked: scale by **height**, let width follow, and centre the result.

```css
.shot { text-align: center; }
.shot img { height: 140pt; width: auto; max-width: 100%; }
```

### 9.5 Positioned overlays need explicit stacking and spacing budgets

Two overlapping mats (a dashboard and a dialog) each need their caption strip
*inside* their own box, and the boxes need non-overlapping caption bands. Put the
caption inside the mat's padding rather than as a sibling, so it can never
collide with the other panel's footer.

---

## 10. The verification loop

Build, then **look at every page**. There is no lint for "this looks broken".

```bash
sh deck/build.sh

pdfinfo deck.pdf | grep -E "Pages|Page size"     # 5 pages, 960 x 540 pts
pdffonts deck.pdf                                # only your fonts, EMB=yes
pdftotext deck.pdf - | head -40                  # text is real, not outlines

rm -rf /tmp/qa && mkdir /tmp/qa
pdftoppm -png -r 96 deck.pdf /tmp/qa/p           # 960pt -> 1280 px wide
pdftoppm -png -r 150 -f 2 -l 2 deck.pdf /tmp/qa/hi
```

`-r 96` on a `960 × 540 pt` page gives 1280 × 720 px — screen-filling, cheap to
review. Spot-check the densest page at `-r 150` to catch kerning and hairlines.

Review checklist for each page:

1. Does anything touch or cross the 52pt margin?
2. Any text over another element?
3. Are cards in a row equal height, and are their footers aligned?
4. Is the footer page number correct after an insert/delete?
5. Any glyph that looks like a different typeface → check `pdffonts`.
6. Is there a band of dead space taller than ~90pt? (Rebalance, or move the
   closing line up to group it with the content.)

`pdftotext` doubles as a facts check: `grep` for every number the deck claims. A
missing figure means a line got clipped or a font lost a glyph.

Finally, keep the **200-line ceiling** on every source file. Splitting CSS by
responsibility (`base` → `components` → `panels` → `slides`) is what keeps a deck
editable after a month.

---

## 11. Where the design came from

The visual language in this deck is **not invented** — it was measured off a
reference deck the maintainer supplied, `assets/BEEZY MOBILE DEV (1).pdf`
(seven pages, 1440 × 810 pt, by an unnamed designer).

What was taken, and how:

| Element | Method |
| :-- | :-- |
| Page proportions | `pdfinfo` on the reference; kept 960 × 540 pt (same 16:9, a third of the file size) |
| Palette | sampled from 150 dpi renders with PIL; ink `#35281c`, brown `#7c561d`, amber `#eba124`, cream `#ece1d1`, strip `#cfc9c1` |
| Typefaces | `pdffonts` on the reference → **Montserrat Bold** and **Open Sauce One**; both are OFL and were bundled rather than referenced |
| Display scale | measured cap height against page width → 88pt title, 44pt section heads, ~−3.6pt tracking |
| Composition | gradient field, huge caps, cream cards, a thin hairline, corner shards, a small institutional lockup top-left |
| CiIT wordmark | cropped from page 1 at 300 dpi and keyed to transparent white (§7.1) |

What is **original** and deliberately not copied:

- The **KitNapper mark** — an amber hexagon holding a duffel bag, drawn as SVG.
  The reference's bee was not reused, and the client's logo is the only brand
  asset lifted, because it is the institution's own mark.
- All layout arithmetic, copy and the card/step/table/stat components.

**Do not commit the reference.** `assets/BEEZY MOBILE DEV (1).pdf` and `.pptx`
are 18 MB combined and belong to someone else; they stay untracked. Reproduce the
*design language*, never redistribute the source deck.

---

## 12. Copy this for a new deck

1. `deck/base.css` — `@page` size in `pt`, `.slide` fixed size + `overflow: hidden`.
2. `deck/build.sh` — the 20-line glue script from §4.
3. Fonts: static TTFs, subset with `pyftsubset`, licence text committed, fallback
   chain pointed at the display font.
4. One fragment per page, absolutely positioned, footer number written by hand.
5. `sh build.sh` → `pdfinfo` → `pdffonts` → `pdftoppm` → **look at every page**.
6. Keep every file under 200 lines; split by responsibility, not by length.

When the design genuinely needs flexbox or CSS grid, switch to headless Chromium
(`playwright` → `page.pdf({ width, height, printBackground: true })`) and keep
everything else in this document — the font, asset and verification steps are
renderer-independent.
