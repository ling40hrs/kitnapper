# KitNapper M5 — Implementation Backbone

> **Status (2026-11-16, WP0):** plan authored and preflight re-proved; M5 is **not yet
> started**. Starting HEAD `4ff491e` (local = `origin/main`, tree clean). This file is
> the working document for M5 and replaces `HANDOFF-M5.md` as the executable contract;
> the scope note stays authoritative for *why* (design §12–§13, D27).
>
> Authoritative inputs: `docs/implementation-plan.md` §4 (M5 row) and §5 (D7, D8, D11,
> D15, D19, D22, D27), `docs/design.md` §7, §12, §13, `docs/SRS.md` §3.4–§3.5 and §6.2
> steps 11–12, `docs/PRD.md` REQ-010/REQ-012, `docs/preferences.md` §1–§4.
> Process: one work package per turn (`preferences.md` §2), then stop and report.

## 1. Objective

Deliver **M5 — inventory, photos, reset, settings** so a manager can add an item with a
photo, edit it without changing its concrete kind, flag it for maintenance, decommission
it, browse the whole inventory with a photo preview, and reset the demo dataset — with
every photo stored, displayed, and cleaned up exactly as design §12 specifies, and with
the app's first loading skeletons (design §13, D27).

### Milestone exit criteria

| # | Criterion | Source |
| :-- | :--- | :--- |
| 1 | Manager adds an item **with a photo**; the photo shows on the inventory row preview and on the catalog card/preview; it survives an app restart | SRS §6.2 step 11, FR-10, NFR-08 |
| 2 | The same item decommissioned stays listed, is not borrowable, and keeps its photo | SRS FR-10, design §12 lifecycle |
| 3 | Editing an item's category never changes its concrete kind; only the existing kind's extra field is shown | D19 |
| 4 | Reset demo data restores the PRD §9 baseline **and** empties `kitnapper-images/` | SRS §6.2 step 12, FR-12, D11 |
| 5 | Missing/corrupt photo files fall back to the category tile with no error surface | SRS FR-10 acceptance, NFR-08 |
| 6 | Photo filenames are `<equipmentId>.jpg`, system-generated, path-safe | D22 |
| 7 | Photo decode and import show a skeleton per design §13.2/§13.6; reduced motion is honoured | design §13, D27 |
| 8 | Settings exposes save/photo locations, theme, reset, and "Animate loading placeholders" | design §5 screen inventory, §13.4 |
| 9 | `./build.sh clean && ./build.sh` and `ant clean jar` green, zero warnings; `./test/run-all.sh` green with the new probes registered | preferences §4 |
| 10 | Docs, README, and both handoffs match the shipped behavior in the same change | `AGENTS.md` Golden Rule 1 |

## 2. Confirmed decisions (this session)

| # | Question | Decision | Why |
| :-- | :--- | :--- | :--- |
| D32 | How far does `SkeletonGate` go in M5? | **Photos only** — card/preview decode and photo import. Catalog/dashboard/loan query gating stays specified in design §13.1 but is **not** implemented in M5; the M6 audit owns it. | D27 scopes M5 to photos; gating M3/M4 screens that are locked adds risk without a demo step. |
| D33 | What happens to the session after "Reset demo data"? | **Stay signed in.** After `resetToSeed()` the controller calls `AppContext.rebindCurrentUser()`, which re-resolves the session user **by email** from the fresh user list. | Demo walkthrough resets right after adding an item; a signed-in manager with a stale `User` instance would break every role check. |
| D34 | Where does the photo alpha matte come from? | `ImageStore.stage(source, matteRgb)`; the **controller** reads `Theme.bgSurface` and passes its RGB. | Keeps every hex/token in `view/Theme` (no-view-import rule is about service→view, not the reverse); design §12's "flatten onto `BG_SURFACE`" is honoured literally. |
| — | Does Inventory filter from the omnibar? | **Yes, text only** — typed text filters name/serial. No category/status chips, no layout toggle. | The omnibar field is global; an inert field on one screen reads as a bug. Chips are deferred (see §16). |

These four land in `docs/implementation-plan.md` §5 as D32–D34 plus the filtering note
in `docs/design.md` §5 (screen inventory), in the WP6 change.

### Locked constraints carried into M5 (do not re-litigate)

- **JDK only.** Hand-written image decode/scale/encode via `ImageIO`/`Graphics2D`; no
  FlatLaf, no image library, no JSON library.
- **`schemaVersion` stays 1.** `photoFile` stays nullable/additive (D8); older saves load
  unchanged. No new persisted fields in M5.
- **Photo rules (D7, D11, design §12):** `kitnapper-images/<equipmentId>.jpg` beside
  `kitnapper-data.json`; source ≤ 20 MB; extensions `png`/`jpg`/`jpeg`/`gif`/`bmp`;
  downscale to 1024 px longest edge, bilinear; re-encode JPEG q0.85; atomic tmp + move;
  seed ships no photos; reset wipes the folder; orphan sweep on load.
- **Kind vs category (D19):** kind chosen at creation from the category default and
  preserved on edit; changing category never changes the class; the dialog shows only the
  field the existing kind carries.
- **Manager-only (INV-3, D6):** every inventory mutation and the reset re-check the role
  in the service and return a message instead of throwing.
- **Status through entity methods only (INV-1, INV-5, D16):** services call
  `markUnderMaintenance()`, `markAvailable()`, `decommission()`; views never mutate.
- **`DECOMMISSIONED` is terminal** and keeps its photo (audit history).
- **No hex outside `view/Theme`; no `@SuppressWarnings`; ≤ 200 lines per file.**

### Doc deviations to record in the close-out change

1. **Query skeletons deferred** (D32) — design §13.1 lists catalog/loan queries as
   "gated"; M5 ships photo gating only, M6 audits the rest.
2. **`PageHeader` gains an optional action slot** so Inventory's "Add item" sits in the
   content header (design's §5 row names the command but not its location).
3. **Photo staging.** Design §12 names the final file only; M5 stages an import as
   `staging-<n>.jpg` and promotes it to `<id>.jpg` on save, so a cancelled dialog cannot
   leave an orphan that looks like an item photo. The sweep deletes `staging-*` too.
4. **Settings is already routed.** `ScreenCatalog` has a `settings` meta entry and
   `Sidebar` renders the manager footer button since M2; M5 replaces the placeholder, it
   does not add navigation.

## 3. Baseline and ground truth (re-proved 2026-11-16, WP0)

| Fact | Value | How |
| :--- | :--- | :--- |
| HEAD | `4ff491e` — local == `origin/main`, tree clean | `git rev-parse HEAD`, `git ls-remote origin main` |
| Build | `BUILD OK -> build/classes (187 classes)`, zero `-Xlint:all` warnings | `./build.sh clean && ./build.sh` |
| Probes | `ALL PROBES GREEN` — 12/12 registered, `LoansUiProbe PASS=105` | `./test/run-all.sh` |
| Source size | 117 files / 10,793 lines (18 model, 16 service, 10 persistence, 10 controller, 62 view, `Main`) | `find src -name '*.java'` |
| Probe size | 25 files / 2,811 lines | `find test -name '*.java'` |
| Line caps | clean — tallest `CatalogScreen` 199, `PreviewPane` 197, `PenaltyPreviewPane` 195, `LoanPreviewPane` 194, `SegmentedControl` 189 | `wc -l` sweep |
| Static audits | clean — no file > 200, no hex outside `Theme`, no `service`/`controller` imports under `view`, no `@SuppressWarnings`, no `System.exit` outside `Main` | grep sweeps |

### Facts that shape the plan

- **The model and persistence halves of M5 already exist.** `Equipment` has a validated
  `photoFile` with `getPhotoFile()`/`setPhotoFile()` (bare names only), `EquipmentMapper`
  reads/writes `photoFile`, `Ids.nextEquipmentId` generates `e-NNN`, and
  `DataStore.resetToSeed()` reseeds relative to today. M5 adds the *store, the service,
  the screens, and the display pipeline* — not the schema.
- **`Theme.animateSkeletons` already exists** (default `true`, session-only) and only
  needs the Settings switch and the `SkeletonPanel` read.
- **`PreviewPane` (197) and `EquipmentCard` hard-code `new GradientTile(...)`.** Wiring
  photos means editing both; `PreviewPane` must be split first (§6.5).
- **`PreviewPane` is catalog-specific** (checkout action), so Inventory gets its own
  `InventoryPreviewPane` rather than a flag-laden shared pane.
- **`ScreenMounts` (116) gains two mounts**; it is the natural place to grow past the
  150-line warning, so it gets a split before it does (§6.5).
- **`DialogSkin` runs its confirm handler synchronously on the EDT** and keeps the dialog
  open when the handler returns a message. Photo import therefore happens on **file
  choice** in a worker (skeleton + disabled Save), not on confirm.
- **`AppContext` is constructed after `DataStore.load()` in `Main` and in every probe**,
  so the orphan sweep can live in the `AppContext` constructor.

## 4. Architecture and data flow

```
EquipmentDialog (view)                    InventoryScreen / InventoryPreviewPane (view)
  pick file ──► actions.stagePhoto(path)      PhotoTile(PhotoSource)  ◄── SkeletonGate
                     │                              │                        │
             InventoryActions ──────────────► PhotoSource (view interface)   │
                     │                              │                        │
             InventoryController                    └── impl over ImageStore.load ──┐
                     │                                                              │
             ImageStore.stage / promote / load / sweep / deleteAll               │
                     │                                                              │
             InventoryService.add / update / setPhoto / flag / decommission / reset│
                     │                                                              │
             DataStore.save() + AppContext.fireChanged() ──────────────────────────┘
```

- **Layering:** view declares `InventoryScreen.Actions`, `SettingsScreen.Actions`, and
  `PhotoSource`; `controller` implements all three over `service`; `service` owns every
  mutation and the only `DataStore.save()` calls. No view imports `service`/`controller`.
- **Change notification:** `InventoryServiceImpl` calls the `fireChanged` runnable passed
  by `AppContext` (same pattern as `LoanServiceImpl`), so mounted screens reload.
- **Photo pipeline, one path for add and replace:** the dialog calls
  `actions.stagePhoto(path)` → `ImageStore.stage` writes `kitnapper-images/staging-<n>.jpg`
  (validated, downscaled, re-encoded) → on save the service calls
  `ImageStore.promote(staged, id)` then `equipment.setPhotoFile(name)` then `save()`.
  Cancel or replace deletes the staged file. The sweep removes stray `staging-*` and any
  `<stem>.jpg` whose stem is not a live equipment id.
- **Display path:** `PhotoTile` asks `PhotoSource.load(file, maxEdge)` **off the EDT**
  through `SkeletonGate`; success paints the image (cover-scaled, 8 px radius), absence,
  failure, or the 10 s watchdog paints `GradientTile`. `PhotoCache` memoises decoded
  images per `file@edge` so scrolling does not re-decode.
- **Reset path:** `SettingsScreen` confirms (destructive) → `SettingsController.reset()` →
  `InventoryService.resetDemoData()` (`ImageStore.deleteAll()` + `DataStore.resetToSeed()`)
  → `AppContext.rebindCurrentUser()` → `PhotoCache.clear()` → `ScreenMounts.reloadAll()` →
  toast.

## 5. API contracts

### 5.1 `kitnapper.service.ImageRules` (new, constants)

Single home for the photo limits (no magic numbers):

```java
public final class ImageRules {
    public static final String FOLDER = "kitnapper-images";
    public static final String STAGING_PREFIX = "staging-";
    public static final List<String> EXTENSIONS = List.of("png", "jpg", "jpeg", "gif", "bmp");
    public static final long MAX_SOURCE_BYTES = 20L * 1024 * 1024;
    public static final int MAX_EDGE_PX = 1024;
    public static final int CARD_EDGE_PX = 160;     // design §12 card thumbnail
    public static final int PREVIEW_EDGE_PX = 640;  // design §12 preview
    public static final float JPEG_QUALITY = 0.85f;
    public static final String OUTPUT_SUFFIX = ".jpg";
}
```

### 5.2 `kitnapper.service.ImageStore` (new interface)

```java
public interface ImageStore {
    Path folder();

    /** Validates, decodes, downscales and re-encodes {@code source}; returns a staged name. */
    String stage(Path source, int matteRgb);   // IllegalArgumentException on extension/size/format

    /** Moves {@code staging-…} to {@code <equipmentId>.jpg}; returns the bare file name. */
    String promote(String stagedName, String equipmentId);

    /** Decoded image at most {@code maxEdge} px on its longest edge; empty when absent/corrupt. */
    Optional<BufferedImage> load(String photoFile, int maxEdge);

    void delete(String photoFile);              // no-op when already gone
    void deleteAll();                           // reset demo data
    void sweep(Collection<String> equipmentIds); // orphan + staging cleanup on load
}
```

`ImageStoreImpl` behaviour, in order:

1. **Validate** — extension from `ImageRules.EXTENSIONS` (case-insensitive), `Files.size`
   ≤ 20 MB, `ImageIO.read` non-null. Each failure throws `IllegalArgumentException` with a
   user-facing message ("Unsupported image type: .tiff", "Photo is larger than 20 MB.",
   "That file is not a readable image.").
2. **Downscale** — only when the longest edge exceeds `MAX_EDGE_PX`; `Graphics2D` with
   `VALUE_INTERPOLATION_BILINEAR`, `RenderingHints.VALUE_RENDER_QUALITY`.
3. **Flatten** — draw onto an opaque surface filled with `matteRgb` (D34) before encoding,
   so PNG/alpha GIF transparency becomes the theme surface rather than black.
4. **Encode** — `ImageIO.getImageWritersByFormatName("jpeg")` +
   `JPEGImageWriteParam.setCompressionQuality(JPEG_QUALITY)`.
5. **Write atomically** — `.tmp` sibling then `Files.move(ATOMIC_MOVE)`, falling back to
   `REPLACE_EXISTING` on `AtomicMoveNotSupportedException` (identical to `DataStoreImpl`).
6. **Tolerate** — `load` catches `IOException`/`RuntimeException` and returns empty;
   `delete`/`sweep` ignore `IOException`. Nothing about a photo may surface an error.
7. **Stay inside the folder** — `photoFile` is re-validated (no separators, no `..`) before
   any resolve; the model already enforces it, this is the defensive second check (D22).

### 5.3 `kitnapper.service.InventoryService` (new interface + impl)

```java
public interface InventoryService {
    String peekNextId();                                  // Ids.nextEquipmentId, no reservation
    Optional<String> add(ItemDraft draft);                // empty = saved
    Optional<String> update(String equipmentId, ItemDraft draft);
    Optional<String> setPhoto(String equipmentId, String stagedName);
    Optional<String> removePhoto(String equipmentId);
    Optional<String> flagMaintenance(String equipmentId);
    Optional<String> clearMaintenance(String equipmentId);
    Optional<String> decommission(String equipmentId);
    void resetDemoData();
}
```

- `Optional<String>` is the house style for a translated failure (D16): the dialog shows
  the message inline, the screen shows it in a toast, nothing throws to the EDT.
- **Role guard first** (D6/INV-3): not signed in as `Manager` → `"Only managers can change
  the inventory."` Reset is guarded the same way.
- `add` — `EquipmentFactory.create(peekNextId(), draft)`, insert into
  `store.equipment()`, `save()`, `fireChanged()`. A staged photo is promoted **after** the
  item exists, then `setPhotoFile`, then a second `save()`; failure after the first save
  leaves an image the sweep removes (documented, not a crash path).
- `update` — `equipment.updateDetails(...)` plus the **existing kind's** extra field
  (`DigitalEquipment.updateWarrantyContact` / `MechanicalTool.updateMaintenanceInterval`);
  category changes never re-instantiate (D19). Decommissioned items are rejected with
  `"Decommissioned items are read-only."`.
- `flagMaintenance` / `clearMaintenance` / `decommission` — call the entity transitions and
  translate `IllegalStateException` into the same wording the catalog uses
  ("Only available or loaned items can be flagged for maintenance.", etc.).
- `resetDemoData` — manager-only; `ImageStore.deleteAll()` then `store.resetToSeed()` then
  `fireChanged()`.
- Validation shared with the dialog: blank name/serial, non-finite/negative value,
  negative interval → the same messages the dialog produces.

### 5.4 `kitnapper.service.ItemDraft` and `EquipmentFactory` (new)

```java
public record ItemDraft(String name, EquipmentCategory category, String serialNumber,
                        double replacementValue, String warrantyContact,
                        int maintenanceIntervalDays) { }
```

`EquipmentFactory.create(String id, ItemDraft draft)` maps category → concrete kind per
D19: `CAMERA`/`LENS`/`LAPTOP`/`AUDIO` → `DigitalEquipment`, `TOOL` → `MechanicalTool`,
`OTHER` → `GeneralEquipment`. Kind is *not* a dialog field.

### 5.5 `AppContext` and `DataStore` additions (modified)

```java
// DataStore (+impl)
Path folder();                       // the app folder holding kitnapper-data.json

// AppContext
public ImageStore images();          // built in the constructor from store.folder()
public InventoryService inventory(); // built in the constructor
public void rebindCurrentUser();     // D33: re-resolve by email, keep the session
```

The `AppContext` constructor builds `ImageStoreImpl(store.folder())`, then
`images.sweep(ids)` where `ids` are the loaded equipment ids — the load-time orphan sweep
(D11). A missing folder is a no-op, so probes with scratch directories are unaffected.

### 5.6 Controllers (new)

| Class | Responsibility |
| :--- | :--- |
| `InventoryController` | Query state (search text), `results()` (name/serial, case-insensitive, sorted by name then id — the `CatalogServiceImpl` ordering), selection, `isManager()`, delegates mutations to `InventoryService`, translates results into messages. |
| `InventoryActions` | Implements `InventoryScreen.Actions` + `PhotoSource`; reads `Theme.bgSurface` for the matte (D34), opens nothing itself (the view opens the dialog), reports counts to the status sink. |
| `SettingsController` | Implements `SettingsScreen.Actions`: theme read/apply through the `Consumer<Boolean>` supplied by `AppController`, path strings from `DataStore.folder()`, `Theme.animateSkeletons`, and `reset()` (confirm is the view's job) which runs the §4 reset path. |

### 5.7 View-facing contracts (new)

```java
// kitnapper.view.PhotoSource — implemented by InventoryActions, called off the EDT
interface PhotoSource {
    Optional<BufferedImage> load(String photoFile, int maxEdge);
}

// kitnapper.view.InventoryScreen.Actions — implemented by InventoryActions
interface Actions {
    List<Equipment> search(String query);
    boolean isManager();
    Optional<String> add(EquipmentForm form);
    Optional<String> update(String equipmentId, EquipmentForm form);
    Optional<String> flagMaintenance(Equipment item);
    Optional<String> clearMaintenance(Equipment item);
    Optional<String> decommission(Equipment item);
    Optional<BufferedImage> stagePhoto(Path source);   // returns the staged image for the well
    void discardStaged(String stagedName);
    Optional<String> applyPhoto(String equipmentId, String stagedName);
    Optional<String> removePhoto(String equipmentId);
    PhotoSource photos();
    void setStatus(String left, String right);
}

// kitnapper.view.SettingsScreen.Actions — implemented by SettingsController
interface Actions {
    boolean isDarkTheme();
    void setDarkTheme(boolean dark);
    String saveFilePath();
    String photoFolderPath();
    boolean animateSkeletons();
    void setAnimateSkeletons(boolean on);
    Optional<String> resetDemoData();   // empty = reset ran
}
```

`EquipmentForm` (view record) carries the dialog's field values; the controller maps it to
`ItemDraft`. Names stay distinct so no view type crosses into `service`.

### 5.8 Skeleton timing constants (view)

`SkeletonGate` owns the numbers design §13.2 lists, exposed as `static final int` so probes
assert them instead of sleeping: `DELAY_MS = 150`, `MIN_VISIBLE_MS = 300`,
`FADE_MS = 167`, `WATCHDOG_MS = 10_000`, `PULSE_MS = 40`.

## 6. File inventory

### 6.1 New — service (7 files, ~620 lines)

| File | Lines | Content |
| :--- | ---: | :--- |
| `service/ImageRules.java` | 40 | §5.1 constants |
| `service/ImageStore.java` | 45 | §5.2 interface |
| `service/ImageStoreImpl.java` | 175 | validate → scale → flatten → encode → atomic write; load/delete/sweep |
| `service/InventoryService.java` | 45 | §5.3 interface |
| `service/InventoryServiceImpl.java` | 190 | role guard, CRUD, photo promote/remove, reset; `fireChanged` |
| `service/ItemDraft.java` | 25 | §5.4 record |
| `service/EquipmentFactory.java` | 40 | §5.4 category→kind factory |

### 6.2 New — controller (3 files, ~330 lines)

| File | Lines | Content |
| :--- | ---: | :--- |
| `controller/InventoryController.java` | 110 | query state, selection, mutation delegation |
| `controller/InventoryActions.java` | 130 | `InventoryScreen.Actions` + `PhotoSource` + matte |
| `controller/SettingsController.java` | 95 | `SettingsScreen.Actions`, theme, paths, reset |

### 6.3 New — view (12 files, ~1,520 lines)

| File | Lines | Content |
| :--- | ---: | :--- |
| `view/PhotoSource.java` | 20 | §5.7 interface |
| `view/PhotoCache.java` | 75 | access-ordered LRU, key `file@edge`, `clear()` |
| `view/PhotoTile.java` | 150 | 16:9 media slot; skeleton → image or `GradientTile` |
| `view/SkeletonPanel.java` | 120 | `Variant` MEDIA/PREVIEW/CARD/TABLE; flat 4 px blocks + pulse |
| `view/SkeletonGate.java` | 155 | 150/300/167/10 000 ms, watchdog, accessible state, reduced motion |
| `view/ToggleSwitch.java` | 90 | 40×20 switch (§6 components), Space/Enter, accessible name |
| `view/InventoryScreen.java` | 185 | header + "Add item", table, preview, omnibar search, status counts |
| `view/InventoryTable.java` | 120 | `TableSkins` table, 5 columns, selection hook, `inScrollPane()` |
| `view/InventoryTableModel.java` | 90 | typed columns like `CatalogTableModel` |
| `view/InventoryPreviewPane.java` | 155 | 280 px pane: `PhotoTile`, badge, metadata, Edit/Flag/Decommission |
| `view/EquipmentDialog.java` | 190 | add/edit form, category→field swap, photo well, inline errors |
| `view/PhotoWell.java` | 130 | 16:9 well: `Choose photo…`/`Remove photo`, skeleton, busy state |
| `view/SettingsScreen.java` | 165 | theme, paths, animate switch, reset |

### 6.4 Modified (11 files, ~+180 net, ~-90 moved out)

| File | Change |
| :--- | :--- |
| `service/DataStore.java` / `DataStoreImpl.java` | add `Path folder()` |
| `service/AppContext.java` | `images()`, `inventory()`, `rebindCurrentUser()`, sweep in constructor |
| `view/PreviewPane.java` | 197 → ~160: media slot extracted to `PreviewMedia`; `PhotoTile` wiring |
| `view/PreviewMedia.java` (new, ~90) | 16:9 media slot swapped between `GradientTile`/`PhotoTile`, `refreshTheme` |
| `view/EquipmentCard.java` | media block becomes `PhotoTile` (160 px) |
| `view/PageHeader.java` | 24 → ~40: optional right-aligned action slot |
| `view/ScreenCatalog.java` | no route change; verify `settings`/`inventory` meta wording |
| `controller/ScreenMounts.java` | mount `inventory` + `settings`; `reloadAll()`; split if > 150 |
| `controller/AppController.java` | `toggleTheme` → `applyTheme(boolean)`; pass it to `ScreenMounts` |
| `view/Theme.java` | javadoc only (the Settings toggle now owns `animateSkeletons`) |
| `desktop-app/test/run-all.sh` | register the three new probes |

### 6.5 Line-cap strategy

- **`PreviewPane` (197) splits before it grows:** the media slot, its `GradientTile`
  construction, and its theme refresh move to `PreviewMedia` (new). `PreviewPane` keeps
  metadata, badge, action, hints.
- **`ScreenMounts` (116) splits when it passes 150:** if `mountManager` +
  `mountDataScreens` (inventory/settings) exceed the warning line, the new mounts move to
  a `DataScreenMounts` helper the same way `ScreenMounts` was carved out of
  `AppController` at M4 WP3. Hard ceiling is 200 either way.
- **`EquipmentDialog` (est. 190) splits on contact:** if the photo well pushes it over 175,
  the well is already a separate class (`PhotoWell`); if it still exceeds 190, the form
  fields move to `EquipmentFormFields`.
- **`InventoryServiceImpl` (est. 190):** messages go to `InventoryMessages` (mirroring
  `LoanMessages`/`PenaltyMessages`) if the class passes 180.
- Nothing else in the touch list is above 120.

### 6.6 Totals after M5

| Group | Files | Lines |
| :--- | ---: | ---: |
| `service` | +7 (23) | +620 |
| `controller` | +3 (13) | +330 |
| `view` | +13 (75) | +1,610 |
| Probes | +3 (+3 support) (28→31) | +~1,000 |
| **Total source** | **117 → 140** | **10,793 → ~13,350** |

## 7. Work packages (execution order)

### WP0 — Plan + preflight (DONE 2026-11-16)

Author this file; re-prove baseline; static audits; commit. No application code.

### WP1 — Image store, inventory service, `InventoryProbe`

- `ImageRules`, `ImageStore(+Impl)`, `ItemDraft`, `EquipmentFactory`,
  `InventoryService(+Impl)`, `DataStore.folder()`, `AppContext` additions (no view code).
- `test/kitnapper/InventoryProbe.java` + `InventoryChecks` + `InventoryPhotoChecks` +
  `ResetChecks` (headless, temp dir, fixed `T`).
- Register `InventoryProbe` in `run-all.sh`.
- **Done when:** the probe proves D19/D22/D32-photo/D7 rules headless; build + suite green.

### WP2 — Skeleton and photo display, wired into catalog

- `SkeletonPanel`, `SkeletonGate`, `PhotoCache`, `PhotoSource`, `PhotoTile`.
- Split `PreviewPane` → `PreviewMedia`; wire `EquipmentCard` (160 px) and the preview
  (640 px).
- `test/kitnapper/view/PhotoUiProbe.java` + `PhotoTileChecks` (display).
- **Done when:** a saved photo shows on the card and preview; a missing one falls back;
  the skeleton only appears past 150 ms; reduced motion yields static blocks.

### WP3 — Inventory screen

- `InventoryTable(Model)`, `InventoryPreviewPane`, `InventoryScreen`, `InventoryController`,
  `InventoryActions`, `PageHeader` action slot, `ScreenMounts` mount of `inventory`.
- `test/kitnapper/view/InventoryUiProbe.java` + `InventoryUiChecks`.
- **Done when:** the manager route renders 10 rows, omnibar filters, selection drives the
  preview, and the status bar counts; `inventory` no longer shows the placeholder.

### WP4 — `EquipmentDialog` + photo import flow

- `EquipmentDialog`, `PhotoWell`, `InventoryService.setPhoto/removePhoto` wiring,
  replace-photo confirmation, inline `DANGER` messages, busy Save while importing.
- Extend `InventoryUiChecks` with dialog-body assertions per category (D19) and the
  import failure paths.
- **Done when:** add-with-photo and edit work end to end through the controller; the kind
  survives a category change; a rejected file shows an inline error and leaves no file.

### WP5 — Settings + demo reset

- `SettingsScreen`, `ToggleSwitch`, `SettingsController`, `ScreenMounts.reloadAll()`,
  `AppController.applyTheme`, `AppContext.rebindCurrentUser()`, route `settings`.
- `test/kitnapper/ResetChecks` extended: reset wipes images, restores 10 items, re-binds
  the user, and rewrites the save file.
- **Done when:** reset from Settings restores the §6.1 baseline, empties
  `kitnapper-images/`, keeps the session, and refreshes every mounted screen.

### WP6 — Verification, docs, handoff

- Manual SRS §6.2 steps 11–12 through the real `AppController` (throwaway harness, not
  committed), visual checks (§10.2), full clean build, `ant clean jar`, full suite,
  static audits, docs, both handoffs.

## 8. UI specification

### 8.1 Inventory (manager)

- **Header:** `PageHeader("Inventory", "The full equipment inventory.")` with the
  **Add item** primary button in the header's action slot.
- **Table** (reuses `TableSkins`/`TableCells`): `Name` (text), `Category` (text),
  `Status` (badge cell), `Serial` (mono), `Replacement value` (money, right-aligned);
  40 px rows, 36 px header, no vertical gridlines, `TableRowSorter` wired with typed
  columns (money sorts by value, not text).
- **Omnibar:** the typed text filters name/serial (case-insensitive); no chips, no layout
  toggle. Clearing the field restores all rows.
- **Status bar:** `"<n> items · <m> selected"` left, aggregate replacement value right
  (`Formats.money`).
- **Empty state** when the filter matches nothing: "No items match this filter." with a
  **Clear search** action that clears the omnibar field.
- **Preview** (280 px): 16:9 `PhotoTile` (246 px wide → 640 px decode), title 16 px
  semibold, status badge, metadata rows Category / Serial / Replacement value / ID
  (mono) plus one kind-specific row — **Warranty contact** for `DigitalEquipment`,
  **Maintenance interval** (`"<n> days"`, or "—" when 0) for `MechanicalTool` — and the
  actions: **Edit** (primary), **Flag for maintenance** or **Clear maintenance**
  (secondary, depending on state), **Decommission** (destructive). Caption line:
  "Decommissioned items are read-only." when retired; "Return or flag the item before
  decommissioning." when `LOANED`.

### 8.2 `EquipmentDialog` (480 px)

- Fields: **Name**, **Category** (combo: Camera, Lens, Laptop, Audio, Tool, Other),
  **Serial number**, **Replacement value** (₱, numeric), then the kind-specific field the
  item already carries (D19): **Warranty contact** or **Maintenance interval (days)**.
  Add mode shows the field implied by the selected category (and swaps it when the
  category changes); edit mode shows **only** the existing kind's field, labelled with the
  kind.
- **Photo well** (`PhotoWell`, 16:9): the current image (or the category tile), a
  **Choose photo…** button and a **Remove photo** button, and one status line below.
  Choosing a file stages it in a worker: the well shows the media skeleton, both buttons
  and **Save** are disabled, and the field shows "Importing photo…". Success shows the
  imported image; rejection shows an inline `DANGER` line (`ImageRules` message) and the
  previous image stays.
- **Save** validates inline (blank name/serial, non-numeric or negative value, negative
  interval) before calling the controller; a service rejection re-opens the dialog with
  the message (existing `DialogSkin` contract).
- Replacing an existing photo asks for confirmation (design §7 lists photo replace as
  destructive); removing one does too.

### 8.3 Settings / Data (manager)

Four stacked sections in a single scroll-free column (max width 720):

| Section | Content |
| :--- | :--- |
| **Appearance** | Theme `ToggleSwitch` ("Dark theme"), wired to the same `applyTheme` path as the title-bar button |
| **Loading** | `ToggleSwitch` "Animate loading placeholders" → `Theme.animateSkeletons` (session-only, D3 precedent) with a secondary line: "Turns off the pulse and crossfade on photo loading placeholders." |
| **Storage** | Read-only mono rows: save file (`<folder>/kitnapper-data.json`) and photos (`<folder>/kitnapper-images/`), each with a caption that they are local-only and never committed |
| **Demo data** | Explanatory line + **Reset demo data** destructive button → confirmation dialog naming the consequences (all loans, penalties, and uploaded photos are discarded) → toast "Demo data reset." |

Theme and the animate switch apply immediately and need no save button; nothing on this
screen is persisted except through the existing save file.

### 8.4 Skeletons and photo tiles

- `SkeletonPanel` variants used in M5: `MEDIA` (single 16:9 block, photo well and import),
  `PREVIEW` (16:9 + four metadata bars), `CARD` (16:9 + two text bars). `TABLE` ships as a
  variant for M6 but has no host in M5.
- Flat `skeletonBlock` rounded rectangles (4 px radius), media at full token alpha, text
  bars at 70 %, pulse 0.55 ↔ 1.0 alpha over 1 000 ms on a 40 ms tick, **only while
  `isShowing()`** and never when `Theme.animateSkeletons` is off.
- **Never** gradient, shimmer, accent, focusable, or clickable. Accessible name
  "Loading photo" on the host; one name-change event at start and one at end.
- Failure/timeout always resolves to `GradientTile`, never to an error surface.

### 8.5 Toasts

`"Item added · e-011"`, `"Item updated"`, `"Photo updated"`, `"Photo removed"`,
`"Decommissioned <name>"`, `"Flagged <name> for maintenance"`, `"Demo data reset."`;
failures arrive as the service's message.

## 9. Probe plan

### 9.1 Harness updates

- `test/run-all.sh`: add `kitnapper.InventoryProbe headless`,
  `kitnapper.view.InventoryUiProbe display`, `kitnapper.view.PhotoUiProbe display`.
- Reuse `ProbeSupport` (headless checks, `signIn`, `deleteTree`) and `UiProbeSupport`
  (`find`, `button`, `hasLabel`, `paint`, `nonBlank`, `settle`). New shared needs go into
  those two files, not a third helper.
- Generated test images are built in-probe with `BufferedImage` + `ImageIO` (no fixtures
  committed).

### 9.2 `InventoryProbe` (headless, temp dir, `T = 2026-11-16`)

| Group | Checks |
| :--- | :--- |
| Baseline | seed loads 10 items / 3 users; `peekNextId()` is `e-011`; `folder()` is the temp dir |
| Add | add with photo → id `e-011`, kind per category (6 categories → 3 kinds), `photoFile` = `e-011.jpg`, file exists, second save round-trips through `RootMapper` |
| Validation | blank name, blank serial, negative value, negative interval, unknown extension, 21 MB source, undecodable bytes — each returns a message and leaves the dataset untouched |
| Update (D19) | category `CAMERA → TOOL` keeps `DigitalEquipment`; warranty contact persists; interval is not introduced; decommissioned item rejected |
| Status (INV-1/INV-5) | flag from AVAILABLE and from LOANED; clear from UNDER_MAINTENANCE; decommission from AVAILABLE/UNDER_MAINTENANCE; `LOANED` decommission rejected; second decommission rejected (terminal) |
| Role (INV-3) | borrower session → every mutator returns the manager message; dataset unchanged |
| Photos (D7/D22) | 3000×2000 source → 1024 px longest edge; output decodes as JPEG; PNG with alpha flattened onto the passed matte; `load` respects 160/640; missing file → empty; truncated file → empty; `.jpg` name containing `/` rejected |
| Sweep (D11) | orphan `e-999.jpg` deleted, `staging-*` deleted, live `e-011.jpg` kept, unrelated `.txt` untouched |
| Reset (FR-12/D33) | `resetDemoData()` → 10 items, 3 loans, 0 penalties, 0 photos, images folder empty; `rebindCurrentUser()` returns the fresh manager instance for the same email |

### 9.3 `PhotoUiProbe` (display)

- `PhotoTile` paints the photo when the file exists (`nonBlank` > threshold) and the
  category tile when it is missing/corrupt — measured by distinct pixel signatures.
- A cached decode shows **no** skeleton; a supplier slower than `DELAY_MS` paints one
  (assert via the gate's state flag, not by sleeping past 10 s).
- Watchdog and failure paths: a throwing supplier and an empty result both end on the
  fallback tile and never leave a visible skeleton.
- `Theme.animateSkeletons = false` → static blocks, no pulse timer created; constants
  asserted (`150/300/167/10 000`).

### 9.4 `InventoryUiProbe` (display)

- Route renders: 10 rows, header counts, `commands()` reachable, no placeholder text.
- Omnibar filtering: typing "Canon" → 1 row; clearing → 10; no-match → `EmptyState`.
- Selection: preview title/badge/metadata, action enablement per status (AVAILABLE /
  LOANED / UNDER_MAINTENANCE / DECOMMISSIONED).
- Dialog bodies: add mode shows name/category/serial/value + the category's kind field;
  edit mode shows only the existing kind's field; the photo well exposes Choose/Remove.
- Settings: both switches reflect state and flip `Theme.animateSkeletons`; paths name the
  injected temp folder; reset button is destructive-labelled.
- `printAll` paint produces a non-blank frame (per the M4 capture gotcha).

### 9.5 Expected baseline after M5

Build `187 + ~23 = ~210` classes; probes `15` registered, all green; `InventoryProbe`
≈ 60 checks, `PhotoUiProbe` ≈ 12, `InventoryUiProbe` ≈ 30. **Paste real numbers** in
§17 — do not carry these estimates forward.

## 10. Manual verification

### 10.1 SRS §6.2 (M5 slice)

1. **Camera-equipped walkthrough (step 11):** log in as manager → Inventory → **Add item**
   with a real photo → the row appears and the preview shows the photo → Edit the item and
   change its category → the concrete kind and its field are unchanged → **Decommission**
   → the row stays listed, preview reads "Decommissioned items are read-only.", and the
   catalog shows it as `DECOMMISSIONED` with checkout unavailable.
2. **Restart:** close and relaunch → the item and its photo are still there (FR-10).
3. **Reset (step 12):** Settings → **Reset demo data** → confirm → the baseline from §6.1
   returns, the toast appears, `kitnapper-images/` is empty, and the session stays signed
   in as the manager.
4. **Fallback:** rename the image file on disk, relaunch → the category tile shows, no
   error dialog (NFR-08).
5. **Reduced motion:** switch "Animate loading placeholders" off → photo loading shows
   flat blocks with no pulse; the timing still applies.

### 10.2 Visual and behavior checks

- Photos are cover-scaled with an 8 px radius on card, preview, and the dialog well; no
  stretching, no black bars.
- Skeleton geometry matches the content it replaces (no layout shift on swap).
- Decommission confirmation, photo replace, photo remove, and reset are all destructive
  and all confirmed (design §7).
- Keyboard: dialog fields tab in order, `Enter` saves, `Esc` cancels, both switches toggle
  with `Space`.
- Both themes: photo tile background, skeleton blocks, and Settings rows use tokens only.

### 10.3 Notes

- Modal dialogs cannot be driven from probes under xvfb; the click-through runs in the
  throwaway manual harness described in `HANDOFF-M4-PLAN.md` §17 (reflection into
  `AppController.attemptLogin`, actions from a background thread, default button on the
  EDT). It is never committed.

## 11. Documentation updates (same change as behavior)

| File | Change |
| :--- | :--- |
| `README.md` | Status paragraph, file tree (`HANDOFF-M5-PLAN.md`), docs table, "next: M6", feature list |
| `docs/implementation-plan.md` | §2.2/§2.4/§2.5 file inventory rows; §4 M5 row → delivered (M6 becomes next); §5 new decisions D32–D34; revision history |
| `docs/SRS.md` | §3.4 FR-10/FR-12 acceptance confirmed against shipped behavior; §6.2 steps 11–12 wording aligned with the Inventory/Settings screens; changelog entry |
| `docs/design.md` | §5 screen inventory (Inventory/Settings details, add-item location, text-only filter); §12 "delivered in M5" note incl. staging; §13.7 milestones flipped; §7 destructive list unchanged |
| `docs/decisions.md` | §8 photo-scope note if the delivered pipeline differs from the written one |
| `HANDOFF-M5-PLAN.md` | §17 execution log per WP; Resume-here block |
| `HANDOFF-M5.md` | Header → "closed at <sha>; record is `HANDOFF-M5-PLAN.md`" |
| `.opencode/HANDOFF.md` | `STATE`/`NEXT`/`GOTCHAS`/`FILES` after every WP |

## 12. Git plan

One commit per WP, conventional subject, ~72-column body, SSH-signed, with both required
co-author trailers. Documented WPs commit **and push** without asking (standing approval,
`docs/preferences.md` §3); each push is followed by `git ls-remote` and a verified-badge
check, reported with local SHA, remote SHA, and result.

| WP | Commit |
| :--- | :--- |
| WP0 | `docs(m5): author the M5 implementation backbone` |
| WP1 | `feat(m5): add the image store and inventory service` |
| WP2 | `feat(m5): add photo tiles and skeleton loading` |
| WP3 | `feat(m5): add the inventory screen` |
| WP4 | `feat(m5): add the equipment dialog and photo import` |
| WP5 | `feat(m5): add settings and demo reset` |
| WP6 | `docs(m5): verify and close M5` |

Never stage `AGENTS.md`, `build/`, `dist/`, `kitnapper-data.json`, or `kitnapper-images/`.

## 13. Risks and mitigations

| Risk | Mitigation |
| :--- | :--- |
| Stale `User` after reseed breaks role checks | `rebindCurrentUser()` is part of the reset path and is probe-checked (D33) |
| Photo written but save fails → orphan | Staging + promote order, sweep on load, documented as a tolerated state |
| `JFileChooser` untestable in probes | Import logic lives entirely in `ImageStore`; the probe drives it with temp paths, the dialog asserts bodies only |
| Skeleton timers outliving their component | Pulse starts only when `isShowing()`; gate cancels on host removal/dispose; watchdog caps every run at 10 s |
| `PreviewPane`/`ScreenMounts`/`EquipmentDialog` hitting 200 | Split rules fixed in §6.5 before any edit |
| `ImageIO` behaviour differences across JDKs (GIF/BMP) | Only JPEG is written; reads are tolerant (empty on failure) and never surface errors |
| Reset during an open dialog | Dialogs are modal to the frame; the reset button is on Settings behind them |
| Manager nav drift | Settings stays a footer button (M2 shell), not a `navKeys` entry — D24 unchanged |
| Probe flakiness from timing | Assert constants and states, never wall-clock sleeps; display probes run under `xvfb-run -a`, serially |

## 14. Definition of Done

- [ ] All ten §1 exit criteria demonstrated, with probe numbers for the automated ones.
- [ ] `./build.sh clean && ./build.sh` → `BUILD OK`, zero warnings, plus
      `sh ../../netbeans/extide/ant/bin/ant clean jar` green.
- [ ] `./test/run-all.sh` → `ALL PROBES GREEN` with 15 registered probes.
- [ ] Static audits: no file > 200 lines, no hex outside `Theme`, no `service`/`controller`
      imports under `view`, no `@SuppressWarnings`, no `System.exit` outside `Main`.
- [ ] `schemaVersion` still 1; an M4 save file loads unchanged (probe-checked).
- [ ] SRS §6.2 steps 11–12 pass by hand at 1280×800 and once at 1366×768.
- [ ] Docs updated in the same commits; `HANDOFF-M5-PLAN.md` §17 fully logged.
- [ ] Every WP pushed, with remote SHA and verified badge reported.

## 15. Out of scope (do not do in M5)

- Query skeletons for catalog/dashboard/loan screens (D32; M6 audit).
- Thin scrollbars, Fluent motion polish, DPI/contrast passes, 1024×640 fixes, the
  `PenaltiesTable` 1280 px clip, and the toast/preview overlap (M6).
- Any schema change; any new persisted field; exporting/importing the save file.
- Cloud/hosted images, networking, real auth, account management.
- Reworking M3/M4 catalog or loan behavior beyond the `PhotoTile` wiring.
- Inventory category/status filter chips and a cards layout (§16).

## 16. Follow-ups after M5

1. Inventory filter chips + layout toggle, if the manager demo wants parity with Catalog.
2. `SkeletonPanel.TABLE` host and the measured ≥ 150 ms query gate (design §13.1), for M6.
3. Photo drag-and-drop onto the well (out of scope; `JFileChooser` only).
4. `AGENTS.md` milestone table refresh once M5 is merged.

## 17. Execution log

### WP0 — Plan authored and preflight re-proved (2026-11-16)

**Starting HEAD:** `4ff491e` (`docs(m4): record M4 completion and hand off M5`), local ==
`origin/main`, tree clean.

**What changed:** this file only (`HANDOFF-M5-PLAN.md`, new). No application code, no
probe changes.

**Commands run and results:**

| Command | Result |
| :--- | :--- |
| `git log --oneline -5`, `git status --short` | tip `4ff491e`, tree clean |
| `git rev-parse HEAD`, `git ls-remote origin main` | both `4ff491efea9580c4d68a70530399181e3e204ffc` |
| `./build.sh clean && ./build.sh` | `BUILD OK -> build/classes (187 classes)` |
| `./test/run-all.sh` | `ALL PROBES GREEN`, 12/12 registered |
| `wc -l` sweep | no file > 200; tallest `CatalogScreen` 199 |
| hex / view-import / suppression / exit grep | all clean |
| `find src -name '*.java'` | 117 files / 10,793 lines |
| `find test -name '*.java'` | 25 files / 2,811 lines |

**Decisions taken this session:** D32 (photos-only skeletons), D33 (reset keeps the
session and re-binds the user), D34 (matte RGB passed from the controller), Inventory
filtering text-only. Recorded in §2 and scheduled for `docs/implementation-plan.md` §5 in
WP6.

**Not done in WP0:** no service, view, or probe code; the four decisions are not yet in
`docs/implementation-plan.md` (WP6 owns that, per §11).

**Next:** WP1 — `ImageRules`, `ImageStore(+Impl)`, `ItemDraft`, `EquipmentFactory`,
`InventoryService(+Impl)`, `DataStore.folder()`, `AppContext` additions, and
`InventoryProbe` (headless).
