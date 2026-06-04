# SimPaths Flowcharts

This README is the folder index for SimPaths code-logic flowcharts. It points to the editable flowchart sources, the manifest, automation guidance, and rendered figure locations.

Detailed drawing rules and Mermaid style guidance live in:

```text
documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md
```

## 1. Purpose

Flowcharts explain SimPaths code logic for debugging, review, and development. They should remain traceable to Java source code, including branches, method calls, inputs, state changes, and scheduled processes.

The editable Markdown files under `documentation/flowcharts/modules/` are the source of truth for flowchart documentation. Rendered SVG or PNG figures are optional outputs for published documentation.

## 2. Folder Contents

| Path | Role |
|---|---|
| `documentation/flowcharts/README.md` | This folder index. |
| `documentation/flowcharts/automation.md` | User guide for AI-assisted flowchart review automation. |
| `documentation/flowcharts/modules.yml` | Manifest linking flowchart modules to source code, review states, and update triggers. |
| `documentation/flowcharts/modules/*.md` | Editable flowchart module documentation. |
| `documentation/flowcharts/flowchart_review_prompt.md` | Generated review prompt; ignored by Git. |
| `documentation/flowcharts/flowchart_review_agent.log` | Generated Codex review log; ignored by Git. |
| `documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md` | Main human-facing guide for creating and maintaining flowcharts. |
| `documentation/wiki/figures/modules/` | Optional rendered SVG/PNG exports for wiki or website use. |

## 3. Main Guides

| I want to... | Read... |
|---|---|
| Learn when and how to draw code-logic flowcharts | `documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md` |
| Install or use AI-assisted review automation | `documentation/flowcharts/automation.md` |
| Find which flowchart maps to which Java files | `documentation/flowcharts/modules.yml` |
| Edit an existing flowchart | `documentation/flowcharts/modules/*.md` |
| Check agent routing for flowchart tasks | `AGENTS.md` and `.codex/skills/flowchart-update/SKILL.md` |

## 4. Flowchart Modules

Current module source files include:

- `modules/household_composition.md` - schedule-level household composition block.
- `modules/cohabitation.md` - UK cohabitation formation/dissolution flag logic.
- `modules/fertility_give_birth.md` - fertility flagging and newborn creation logic.
- `modules/full_time_hourly_earnings.md` - potential full-time hourly wage update logic.
- `modules/health_long_term_sick.md` - self-rated health and long-term sick or disabled update logic.
- `modules/health_mental_hm1_hm2_cases.md` - active split HM1/HM2 psychological distress caseness logic.
- `modules/health_mental_hm1_hm2_level.md` - active split HM1/HM2 psychological distress level-score logic.
- `modules/labour_supply_consumption.md` - labour supply, disposable income, and consumption decision logic.
- `modules/inschool.md` - in-school decision logic and leaving-school handoff.
- `modules/union_matching.md` - pair-based union matching logic.

Use `modules.yml` as the traceability map rather than relying only on this list.

## 5. Manifest Overview

`modules.yml` records how flowchart modules relate to code and review state. The most important fields are:

- `id`: stable module identifier.
- `title`: human-readable module name.
- `flowchart.source_md`: editable Markdown source file.
- `code_refs.files`: source files whose committed changes may trigger review.
- `code_refs.methods`: optional method hints used to prioritise review.
- `review_state`: current review status.
- `last_verified_commit`: latest commit where the flowchart was checked against code.
- `last_trigger_commit`: latest commit that triggered review.
- `update_triggers`: plain-language reasons the module may need review.

The manifest helps identify review candidates. It does not mean every matched module needs a Mermaid redraw.

## 6. Review States

The `review_state` field tracks the flowchart documentation review workflow:

- `up_to_date`: reviewed and believed to match current committed code.
- `candidate_for_review`: a committed code change touched a mapped source file, so the module should be checked.
- `needs_update`: review found stale documentation that must be edited.
- `updated_unverified`: documentation was edited but has not yet been fully checked.

Recommended transitions:

1. `up_to_date` -> `candidate_for_review` when a relevant committed code change is detected.
2. `candidate_for_review` -> `up_to_date` when review confirms no documentation update is needed.
3. `candidate_for_review` -> `needs_update` when documentation changes are required.
4. `needs_update` -> `updated_unverified` after editing flowchart documentation.
5. `updated_unverified` -> `up_to_date` after checking the revised documentation against code.

## 7. Automation Overview

Flowchart automation can detect committed code changes that map to existing flowchart modules. Depending on the installed mode, it can either:

1. generate a review prompt for later use; or
2. launch Codex CLI automatically to review and update flowchart documentation.

For installation, prerequisites, on-demand review, log files, and troubleshooting, see:

```text
documentation/flowcharts/automation.md
```

The implementation scripts live under:

```text
.codex/skills/flowchart-update/scripts/
```

## 8. Rendering Website Figures

Editable Markdown flowchart files remain the source of truth. For website or wiki display, render stable Mermaid diagrams to SVG or PNG and place published figures under:

```text
documentation/wiki/figures/modules/
```

If a separate website-only Mermaid source is useful, keep it clearly named and traceable to the corresponding module Markdown.

## 9. For AI Agents

Agents should treat this README as a navigation map, not as the full flowchart update procedure.

For Codex tasks involving flowchart review or updates:

1. follow `AGENTS.md` for repository-level instructions;
2. use `.codex/skills/flowchart-update/SKILL.md` for the flowchart update workflow;
3. use `documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md` for human-facing diagram standards;
4. use `documentation/flowcharts/automation.md` only for user-facing automation setup and troubleshooting.
