# CLAUDE.md — Project Guidelines for Neumann

## Project Overview

Neumann is an Android game of space battles between ships that travel slower
than light. The player commands one ship against AI opponents: they pick a
star to travel to, choose an approach vector into its system, and fight the
fixed defences and ships waiting there. Time moves forward in skips (1 minute,
1 hour, 4 hours, 1 day) that stop early when something worth pausing for
happens. Ship state carries over between battles. The presentation is low on
graphics and close to text, styled like a ship's own display.

- **Shared specification** (`docs/`) — the single source of truth for rules,
  design, and architecture.
- **Data** (`data/` — YAML) — canonical, code-free definitions. Logic loads
  this data; it never hard-codes it.
- **Source** — Kotlin. `core/` holds the game rules; `app/` is the Android
  app.

### Documentation precedence

`docs/` is the source of truth. When code and `docs/` disagree, the docs win:
change the docs first (deliberately), then make the code follow. Keep the two
in step.

## Critical Rules

### Do not loop on errors — stop and explain

- If an approach fails twice, stop immediately. Do not retry the same strategy.
- Explain what went wrong, what was attempted, and suggest alternatives — let me
  decide how to proceed.
- Do not silently retry file operations, compilations, or code execution hoping
  for a different result.
- If a tool call or code execution returns an error, analyze the error before
  taking any further action.
- When stuck on a problem, present the situation clearly rather than burning
  through tokens on repeated attempts.

### Ask questions often

- Ask questions often, especially when there is ambiguity or you believe I have
  made a mistake.

### Never use question prompts

- Never use the `AskUserQuestion` tool or any other interactive
  question/multiple-choice prompt. Write the question(s) as plain prose in the
  chat.

### Stop after asking questions

- Whenever you have a question, or multiple questions, stop immediately after
  asking them. End the turn — no further tool calls or implementation until I
  reply.

### Number your questions

- When asking more than one question in a single message, number them (`1.`,
  `2.`, …) so I can answer "1: …".
- Do **not** number anything else. Lists, sections, summaries, and recaps use
  letters (`A.`, `B.`, …) when order matters, or bullets when it doesn't.
- When a single question offers multiple options, label the options with
  letters (`A`, `B`, …), not numbers.

### When a referenced thing is missing, stop and ask

- If I reference a branch, file, function, document, or anything else that you
  can't find, do not silently proceed against the closest match — tell me what
  you looked for and where, and ask.
- The same applies if you find something with a similar name but aren't sure
  it's the right thing.

### Don't silently change the design

- If a stated rule conflicts with another rule, or can't produce what's being
  asked for, say so and ask — never quietly modify, extend, or "fix" my rule to
  make a deliverable work.
- Do not invent structural concepts and quietly build on them.
- Where a rule is ambiguous, document the chosen reading inline flagged with
  **(interpretation)** so it can be revised.

### Keep answers short

- Short and concise. No fluff. No flattery

### Distinct classes with minimal responsibility

- Model every concept as its own small class with a single clear job.
- **Data holders** carry data and have no flow logic. **Flow logic** (phases,
  stages, resolvers) carries no data definitions.
- Steps of a process (phases / stages) are classes too; each orchestrates one
  step and nothing else.
- Platform-specific concerns (UI, persistence, OS APIs) sit behind plain
  interfaces so core logic can be unit-tested without them.
- See [docs/architecture.md](docs/architecture.md) for the canonical class
  list.

### Configuration and data

- Config holds numbers and enums only — no formula strings that nothing
  evaluates.
- Keep config interfaces/types in sync with their data files.

### Android build versioning (Android projects only)

The build automation (`build_branches.sh`) builds pushed `claude/*` branches
and commits each new APK as `versions/vMAJOR.MINOR.apk`,
using the highest version already on the branch with the
minor bumped by one. Several commits can be pushed before the next build, so
the app's `versionName` has three parts, e.g. `v0.037 a (e687)`:

- **Number** (`v0.037`) — set by hand. The predicted APK filename: the highest
  `vMAJOR.MINOR.apk` in the versions folder on the current branch, with the
  minor + 1. Keep the same digit width as the existing files.
- **Letter** (`a`) — set by hand. `a` for the first commit after a new APK
  lands. Each further commit pushed before the next APK lands moves to `b`,
  `c`, …. When a new APK lands, the number goes up and the letter resets to
  `a`.
- **Commit** (`(e687)`) — added by Gradle at build time: the first 4
  characters of the commit the APK was built from, matching the build
  commit's `(from e687bc0)`. Never write it by hand — a commit can't contain
  its own hash.

Rules:

- To decide the number and letter: fetch the branch first, then check whether
  the build automation committed a new APK since the version was last
  changed. New APK → next number, `a`. No new APK → same number, next letter.
  A failed build produces no APK, so it does not reset the letter.
- `versionCode` is generated at build time from a timestamp
  (`(System.currentTimeMillis() / 60000).toInt()` — minutes since epoch). This
  ensures every build is always higher than the last, regardless of branch, so
  APKs from different `claude/*` branches can be installed over each other
  without uninstalling first. Never set `versionCode` by hand.
- Bump the number/letter on every commit that changes the app, and put the
  hand-set part at the end of the commit message, e.g.
  `Bigger aspect glyph tiles (v0.024 b)`.
- Enable `buildConfig = true` so the app can show `BuildConfig.VERSION_NAME`.

`app/build.gradle.kts`:

```kotlin
defaultConfig {
    // ...
    // versionCode: timestamp-based, always increasing across branches.
    // versionName: predicted APK filename (vMAJOR.MINOR.apk = newest committed
    // APK minor + 1), a per-commit letter (a, b, c… for each commit pushed
    // before a new APK lands; resets to 'a' once a new APK is built), and the
    // 4-char hash of the commit Gradle built from. The first two are set by
    // hand; the hash is added at build time because a commit can't contain
    // its own hash.
    versionCode = (System.currentTimeMillis() / 60000).toInt()
    val versionBase = "v0.001 a"
    val commitHash = try {
        val process = Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--short=4", "HEAD"))
        process.inputStream.bufferedReader().readText().trim().ifEmpty { "unknown" }
    } catch (_: Exception) { "unknown" }
    versionName = "$versionBase ($commitHash)"
}
```

### After pushing — read the build result (Android projects only)

There is no Android SDK in the agent's environment, so the build automation
is the only compile check. It picks up the tip of a pushed `claude/*` branch
within 5–15 minutes (see below) and commits the result back onto the
**same branch**:

- `Build vX.XXX.apk (from <sha>)` — it compiled; that APK is `<sha>`.
- `Build failed, no APK change (from <sha>)` — it did **not** compile.

Older versions of the script prefix the message with `<app>/<branch>:`
(e.g. `Build audiobook-reader/claude/x: no APK change (from <sha>)`). Either
way, a build commit that names an APK version means it built; one that
doesn't means it failed.

After every push:

- How long to wait: the script checks each app on its own schedule. If any
  `claude/*` branch of the app has a commit made in the last hour (including
  the script's own build commits), the app is **active** and is checked every
  5 minutes. Otherwise it is **quiet** and checked every 15 minutes. It goes
  by commit time, not push time, and a push does not move the next check
  forward — so the first push after a quiet spell can wait up to 15 minutes
  before it is even picked up, plus the Gradle build time.
- Before pushing, look at the newest commit on any `claude/*` branch
  (usually yours, or the script's last build commit): within the last hour
  → first check-in ~5 minutes after the push; older → ~15 minutes. Schedule
  a one-shot check-in rather than polling with `sleep`.
- At each check-in, `git pull` and find the build commit whose
  `(from <sha>)` matches the commit you pushed. Read the result; never guess
  it.
  - **Built** → done.
  - **Failed** → read `build-logs/build.log` for the Gradle error, fix it,
    push, and check again.
  - **No build commit for your sha yet** → check again in ~5 minutes. Only
    the branch tip is built, so if you pushed again in the meantime, look for
    your newest sha instead.
  - **Still nothing ~25 minutes after the push** → the script may not be
    running. Stop and tell me rather than continuing to wait.
- Pulling the build commits is not enough — read them before pushing again.
  Don't stack new work on top of a build that failed.
- Never force-push a `claude/*` branch: the build commits and APKs live on
  it. Always pull before pushing.

### Tests

- Tests name the **rule** they protect, not the method they call. A failing
  test should say what behaviour broke.

### Design document conventions

- Cross-reference documents with relative markdown links:
  `[game-design.md](docs/game-design.md)`.
- Use canonical vocabulary verbatim; record it in a glossary
  ([docs/glossary.md](docs/glossary.md)). Glossaries are vocabulary only (1–2
  sentence definitions) — field names, schemas, and defaults belong in the
  design docs.
- Weapons and features deferred to a later version are listed under
  **Pending** in their design doc, not deleted.

## Environment

- Linux, vim. Ruby preferred but only when Ruby is a good option — the game
  itself is Kotlin, because it runs on Android.
- The agent's environment has Java and Gradle but no Android SDK, and cannot
  reach Google's Maven repository. All game rules live in `core/` (plain
  Kotlin, no Android), so their tests run here; the build only includes
  `app/` when an SDK is found (see
  [docs/architecture.md](docs/architecture.md)).
- Maven Central sometimes answers 429 (too many requests); `--max-workers=1`
  helps.

```bash
./gradlew :core:test   # unit tests (no SDK needed)
```

## File Structure

```
.
├── CLAUDE.md
├── README.md
├── data/        # Canonical, code-free data (YAML)
├── docs/        # Shared specification (source of truth)
│   ├── README.md
│   ├── game-design.md
│   ├── weapons.md
│   ├── architecture.md
│   └── glossary.md
├── core/        # Game rules (plain Kotlin, unit-tested)
├── app/         # Android app (Kotlin, Compose)
├── versions/    # APKs committed by build_branches.sh
└── build-logs/  # Build output committed by build_branches.sh
```
