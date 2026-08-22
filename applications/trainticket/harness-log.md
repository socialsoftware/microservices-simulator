# Harness Log — trainticket

Append-only. Schema and rules: `.claude/skills/_shared/conventions.md` § "Harness log".

| # | Session | Type | Artifact | Problem | Outcome | Ref |
|---|---------|------|----------|---------|---------|-----|
| 1 | 0 | 1 | `.claude/skills/boot-strap/SKILL.md` | Step 5 needed an inheritable `.mvn/maven.config`, but its fallback chain stops at this checkout's `simulator/.mvn/maven.config`, which is untracked and therefore absent in a linked git worktree; its `>> .git/info/exclude` also fails there because `.git` is a file. | fixed | 5e2d00c97 |
| 2 | 0 | 2 | `AGENTS.md` | Step 6's verification build failed with `release version 21 not supported`; the harness never states the required JDK anywhere, and the shell default is 17. Human chose documenting the requirement over pinning it per-directory. | fixed | 395d4ace2 |
| 3 | 1 | 1 | `.claude/skills/classify-and-plan/SKILL.md` | Step 2.b's §3.2 rule-block regex matched 0 of this spec's 16 blocks and 0 of the shape `docs/templates/domain-model-template.md` § 3.2 itself prescribes: it expects `\| Entities \|` one line after the heading, but the template puts the `\| Field \| Value \|` header and separator rows in between, and rejects the parenthetical qualifier (`STATIONS_EXIST (Route)`) 10 of the 16 headings carry. | fixed | 2709cfbd5 |
| 4 | 1 | 1 | `.claude/skills/classify-and-plan/SKILL.md` | Step 7 declared every file-list path relative to `{src}microservices/{aggregate}/` and asserted `ServiceMapping.java` was the only exception, but `commands/{aggregate}/...` and `events/...` are rooted at the app source root and `sagas/...` at the test root, per `docs/workflow.md` § "Package layout" and `docs/concepts/commands.md` § "File Location". | fixed | 7d0babce6 |
| 5 | 1 | 2 | `.claude/skills/implement-aggregate/session-a.md` | `DocumentType` is a §1 attribute type on three aggregates (User, Contacts, and Order's frozen `contactsDocumentType`), but § "Domain enums" gives only the per-aggregate path and no rule for a shared enum; human chose one shared enum at the app source root over three per-package copies. | fixed | 7d4fad662 |
