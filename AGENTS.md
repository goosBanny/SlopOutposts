# Outposts Repository Guidelines

## 1. Configuration & BoostedYAML Architecture
- **Root Configurations (`config.yml`, `lang.yml`, `schedules.yml`):**
  - Load via `BoostedYamlFactory.createRootDocument(file, defaults)`.
  - Schema auto-updating is fully enabled: preserves comments, adds missing keys automatically, tracks `config-version`.
  - In `ConfigManager.reloadAll()`, delegate directly to `loadConfig()` — avoid redundant `reload()` calls before re-instantiation.
- **Arena Files (`outposts/*.yml`):**
  - Load/save via `BoostedYamlFactory.createArenaDocument(file, templateStream)`.
  - **Selective migration only:** Arena configs must NEVER inject template default keys. Only key relocations and version tracking are permitted.
  - Register all path renames in `ArenaConfigMigrations.applyRelocations(builder)` (e.g. `builder.addRelocation("2", "old.path", "new.path", '.')`).
  - Version constants: use `String CURRENT_VERSION` for alignment with BoostedYAML versioning strings.
- **Stream & Resource Hygiene:**
  - Always wrap `getResourceAsStream()` and `Files.newInputStream()` in `try-with-resources` when passing streams to document factories.
- **Concurrency & Async Access:**
  - Config references read asynchronously (e.g. `LangManager.langConfig`, PAPI expansions) must be `volatile`.
  - Async state consumers must rely on immutable snapshot DTOs (`ArenaViewSnapshot`), never direct mutable arena state.

## 2. Scheduling & Event Windows
- **Strict Start Times:** Scheduled events only trigger when `cron.matches(now)` fires in `tick()`. Never retroactively activate an outpost upon server restart mid-window.
- **Reload State Preservation:** In `ScheduleManager.loadSchedules()`, preserve in-flight active state and timers (`active`, `inOvertime`, `activeEndMillis`) across `/outpost reload`.

## 3. Build & CI Releases
- Run JVM builds and tests with local `./gradlew.bat` (Windows) or `./gradlew` (Linux).
- GitHub Actions workflow (`build.yml`) releases to the rolling `latest` tag:
  - Clean build (`./gradlew clean build`) to avoid residual JARs in `build/libs`.
  - Always purge preexisting release assets via GitHub CLI (`gh release delete-asset`) before publishing so only the current version's JAR is attached.
