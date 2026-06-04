# Agent Instructions

## Code Logic Flowcharts

When the task involves documenting, reviewing, updating, or checking SimPaths code-logic flowcharts, use these files:

- Workflow guide: `documentation/wiki/developer-guide/how-to/code-logic-flowcharts.md`
- Flowchart folder notes: `documentation/flowcharts/README.md`
- Flowchart manifest: `documentation/flowcharts/modules.yml`
- Module flowcharts: `documentation/flowcharts/modules/`
- Shared flowchart-review workflow: `.codex/skills/flowchart-update/SKILL.md`

Use the manifest to find existing flowcharts affected by code changes. Follow module-specific update guidance when it exists.

The `.codex` path is Codex-native, but the `flowchart-update` skill body is plain Markdown workflow guidance. Other AI agents may read and follow it as shared repository guidance when they are asked to review or update flowchart documentation.

For user-facing Markdown documentation, prefer numbered section and subsection headings such as `## 1. ...` and `### 1.1 ...` so concepts, commands, and installation steps can be referenced unambiguously.

## Repository Background

`CLAUDE.md` contains additional repository background written for Claude Code. Codex may consult it for architecture, build, and domain context, but `AGENTS.md` remains the controlling instruction file for Codex behavior.
