# Seed Data — Scope Note

Concrete seed data lives in **PRD.md §9** (users, equipment table, loan table, expected first-launch state) and its verification criteria in **SRS.md §6**.

The seed dataset now does double duty: it is the **first-launch state** and the target state for the manager's **"reset demo data"** action. The app saves state to a local JSON file; startup loads that file when present, otherwise seeds from PRD §9. Deleting the save file manually resets on next launch.

This file stays small on purpose: if the seed dataset ever grows beyond PRD §9 (e.g., more loan scenarios, larger item pools, or a serialized fixture file), move the full dataset definition here and reference it from PRD §9 instead of inlining it.

## Seed photos (D50)

Every seed item carries one photo, `e-00N.jpg`, in
`desktop-app/resources/photos/`. `service/SeedPhotos` copies a missing file into
`kitnapper-images/` on first launch and again after a reset; it never overwrites an
existing file. The committed JPEGs are already encoded the way the app stores photos —
longest edge 1024 px, bilinear downscale, JPEG quality 0.85 — so installing one is a
byte copy and neither startup nor reset pays a decode cost. Add a replacement the same
way: prepare it to those parameters, then drop it in this folder under the item's id.

Save files seeded before D50 have no `photoFile`; one **Reset demo data** attaches the
photos. No migration code runs on load.

### Attribution

All ten are freely licensed Wikimedia Commons images. The JPEGs here are downscaled and
re-encoded copies; no other modification was made.

| File | Seed item | Author | Licence | Source |
| :--- | :--- | :--- | :--- | :--- |
| `e-001.jpg` | Sony A7 IV | Henry Söderlund | CC BY 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:Sony_A7_IV_(ILCE-7M4)_-_by_Henry_S%C3%B6derlund_(51739988735).jpg) |
| `e-002.jpg` | Canon EOS R6 | GodeNehler | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Canon_R6_und_RF_85_1,2-8068.jpg) |
| `e-003.jpg` | Sony FE 24-70mm f/2.8 GM | PantheraLeo1359531 | CC BY 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Sony_FE_24-70mm_F2.8_GM_II_20240211_HOF07415_RAW-Export_000117.png) |
| `e-004.jpg` | Canon RF 24-70mm f/2.8 L | Henry Söderlund | CC BY 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:Canon_EOS_R5_with_Canon_RF_24-70mm_F2.8L_IS_USM_(50170662581).jpg) |
| `e-005.jpg` | MacBook Pro M3 (14") | Xkalponik | CC0 | [Commons](https://commons.wikimedia.org/wiki/File:Apple_MacBook_Pro_(M3).jpg) |
| `e-006.jpg` | Lenovo ThinkPad P1 Gen 6 | TAKA@P.P.R.S | CC BY-SA 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:ThinkPad_P70_(2).jpg) |
| `e-007.jpg` | Rode Wireless PRO kit | -stk | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Rode_Wireless_Go_II_Set.jpg) |
| `e-008.jpg` | Zoom H6 recorder | MIKI Yoshihito | CC BY 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:ZOOM_H6._(49407670621).jpg) |
| `e-009.jpg` | Manfrotto 504X fluid-head tripod | Tranpan23 | CC BY 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:Manfrotto_190XPROL_%2B_804RC2_(8641876091).jpg) |
| `e-010.jpg` | DeWalt 20V cordless drill kit | TaurusEmerald | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:DeWalt_20_Volt_Max_Cordless_Drill.jpg) |

Three photos show a nearby model of the seeded product rather than the exact SKU
(e-003 shows the GM II version of the lens, e-006 a ThinkPad P70, e-007 a Wireless Go II
set); the seed catalogue is fictional and the images exist to make the demo look real.
