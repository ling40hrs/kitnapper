# Seed Data — Scope Note

Concrete seed data lives in **PRD.md §9** (users, equipment table, loan table, expected first-launch state) and its verification criteria in **SRS.md §6**.

The seed dataset now does double duty: it is the **first-launch state** and the target state for the manager's **"reset demo data"** action. The app saves state to a local JSON file; startup loads that file when present, otherwise seeds from PRD §9. Deleting the save file manually resets on next launch.

This file stays small on purpose: if the seed dataset ever grows beyond PRD §9 (e.g., more loan scenarios, larger item pools, or a serialized fixture file), move the full dataset definition here and reference it from PRD §9 instead of inlining it.
