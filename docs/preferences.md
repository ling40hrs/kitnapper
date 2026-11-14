# KitNapper — Working Preferences

> Tracked, **machine-independent** copy of how the maintainer wants work done.
> Portable on purpose: no absolute paths, usernames, or machine setup here.
> Machine-specific details (JDK/Ant locations, signing key, `gh` path, local
> probe directory) stay in the gitignored `AGENTS.md` on each machine.
>
> When a preference here and a local file conflict, this file wins for
> process; local files win for machine setup. Update this file with a `docs:`
> commit whenever the process changes.

## 1. Planning

- Plans must be thorough backbones, not sketches: objective, confirmed
  decisions, API contracts, file inventory, work packages in execution order,
  probe plan, documentation updates, git plan, risks, and a definition of done.
- Put the plan in the repository root, named like the other handoffs
  (`HANDOFF-M<N>-PLAN.md`). The repo is the source of truth — not a hidden or
  external directory.
- Keep a numbered **execution log** in the plan. Every plan or phase gets an
  entry before it is committed, with the starting HEAD, what changed, commands
  run, and concrete results.
- Plans are living documents: check items off and append log entries as
  phases land rather than rewriting history.

## 2. Execution

- **One phase at a time.** Do a single work package per turn, then stop. Never
  execute a whole plan in one go — it burns the context window and makes
  commits hard to review.
- Build service-first: prove business rules, persistence, and edge cases with
  probes before writing any UI screen.
- Keep files under the 200-line cap and split before extending a file that is
  already close to it.
- Respect the existing package boundaries: views never import controllers or
  services; services are the only code that mutates entities or saves data.
- Stay consistent with surrounding structure, naming, and style.

## 3. Documentation and commits

- **Document, then commit, then push.** After documenting a plan or finishing
  a phase, commit and push that work without asking. Standing approval covers
  documented plan/phase work only; anything else still needs explicit approval.
- Each commit follows the repo's rules: conventional subject, ~72-column body
  explaining what/why, signed commits, and the two required co-author trailers.
- Never commit build output, runtime data, or local-only files (`AGENTS.md`,
  save files, images, IDE noise).
- Keep `README.md`, `docs/`, and the handoff files in sync with behavior in
  the same change as the code.
- After each phase, report the commit SHA, the remote SHA, and GitHub's
  verification result — never claim success from the local commit alone.

## 4. Verification and reporting

- Be empirically rigorous. Do not assert correctness from reading code alone;
  write small probes (headless Swing paint/render, temp-dir data checks,
  reflection) and report concrete numbers.
- Prove fixes with before/after measurements where possible.
- Run a final clean build and the full probe suite after changes.
- Prefer concise reports built around a results table.
- State explicitly what was **not** done and why.
- Surface credentials, one-time codes, and required manual steps up front.

## 5. Autonomy and safety

- Work autonomously through multi-step tasks once an approach is approved;
  pause only for genuinely destructive or credentialed actions.
- Do not rewrite or delete unfamiliar work without investigating it first.
- Prefer user-writable tool installs over system-wide changes.
- Keep personal/machine setup out of tracked files.

## 6. Working across machines

- Treat every checkout as potentially fresh: re-read `README.md`, the docs,
  and the current handoff before touching code.
- Do not assume paths, tools, or credentials from another machine exist;
  verify the toolchain first and report gaps instead of guessing.
- Record machine-specific setup in the local `AGENTS.md` (never committed);
  record process in this file (always committed).
