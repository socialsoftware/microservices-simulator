# Harness Log — trainticket

Append-only. Schema and rules: `.claude/skills/_shared/conventions.md` § "Harness log".

| # | Session | Type | Artifact | Problem | Outcome | Ref |
|---|---------|------|----------|---------|---------|-----|
| 1 | 0 | 1 | `.claude/skills/boot-strap/SKILL.md` | Step 5 needed an inheritable `.mvn/maven.config`, but its fallback chain stops at this checkout's `simulator/.mvn/maven.config`, which is untracked and therefore absent in a linked git worktree; its `>> .git/info/exclude` also fails there because `.git` is a file. | fixed | 5e2d00c97 |
| 2 | 0 | 2 | `AGENTS.md` | Step 6's verification build failed with `release version 21 not supported`; the harness never states the required JDK anywhere, and the shell default is 17. Human chose documenting the requirement over pinning it per-directory. | fixed | 395d4ace2 |
