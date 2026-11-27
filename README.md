# GekitotsuKit

<p align="center">
  <img src="src/main/resources/icon.png" width="120" alt="GekitotsuKit icon">
</p>

**An all-in-one toolbox for the Flash game "Gekitotsu Fortress" (激突要塞).**

English | [简体中文](README.zh-CN.md) | [日本語](README.ja.md)

GekitotsuKit is a fan-made toolkit built on a faithful reimplementation of the Gekitotsu Fortress physics engine. It provides batch battle simulation, a native 2D GPU battle viewer, behavior analysis (contribution analysis, matchup profiling, battlefield space telemetry), a fortress workbench with a pluggable effect system, trajectory prediction, a unit dex, and curated community links. It is designed for advanced and hardcore players who want to study, verify and optimize their fortresses.
For performance reasons, the engine uses geometric collision instead of pixel-perfect collision. Individual match results may therefore differ from the original game, but win rates remain accurate to roughly ±5% given a sufficiently large sample size.

> This is an unofficial fan project. It is not affiliated with, endorsed by, or connected to the original game or its author.

## Features

| Module | Description |
|--------|-------------|
| **Battle Simulation** | Run all-vs-all matches between 1P and 2P fortress lists with multi-threaded simulation (Rust-accelerated `gkt-core` by default with built-in Java fallback), battle caching, progress tracking, win-rate statistics, and a one-click native 2D GPU battle viewer (`gkt-render`). |
| **Behavior Analysis** | Analyze fortresses against an evaluation set from three perspectives: **Contribution Analysis** (unit removal ranking), **Matchup Profile** (core heatmap, unit sensitivity, duration/HP distribution, acceleration affinity, Keener strength correction), and **Battlefield Space** (slot damage/death heatmap, progression curves, opening pressure via Rust telemetry). |
| **Fortress Workbench** | Decode, edit, preview and re-encode fortress codes; inspect units and the core; run a built-in analyzer; apply workflow-based effect plugins (including built-in Angle Normalize). |
| **Trajectory Prediction** | Visualize fortress movement and assault trajectories on an interactive canvas with expressions and user-defined variables. |
| **Unit Dex** | Browse all 63 units with sprites, stats, descriptions, tactics and a quickest-action calculator. |
| **Links** | One-click access to the official site and Chinese / Japanese community resources. |

- GUI: Java Swing + FlatLaf 3.5.4, dark theme by default (dark/light toggle & customizable accent color), startup splash window, and GitHub Releases auto-update check / one-click installer.
- Engine: Rust `gkt-core` via JNI (`gkt_jni.dll`, bit-exact simulation) with zero-dependency pure Java fallback, plus an independent wgpu/egui 2D battle viewer (`gkt-render.exe`).
- Language of the UI: Simplified Chinese / Japanese / English (live switching under `Options → Language`).

## Download & Run

### Windows package (recommended)

1. Download the latest `激突Kit vX.Y.Z.zip` from the Releases page.
2. Unzip it anywhere.
3. Run `激突Kit/激突Kit.exe`. A JRE 21 runtime is bundled, so no Java installation is required.

The application reads `1P.txt`, `2P.txt` and `config.ini` from the working directory, and writes `result.txt` and `simple_result.txt`. `config.ini` is created automatically on first launch.

### Run from source

Requirements: JDK 21+ (Maven is optional).

```bash
# Build a runnable fat JAR with Maven
mvn package

# GUI (default)
java -jar target/gekitotsu_java-1.9.2.jar

# CLI
java -jar target/gekitotsu_java-1.9.2.jar --cli
```

To build the bundled Windows executable, run `build_exe.bat` from the project root. Edit the `JDK_HOME` and `M2` paths at the top of the script to match your environment first (and run `build_native.ps1` beforehand if you also want to rebuild `gkt_jni.dll` and `native/gkt-render.exe`). The output is placed in `dist/`:

- `dist/激突Kit/激突Kit.exe` — self-contained application image (with embedded icon, bundled `native/gkt-render.exe` if built)
- `dist/激突Kit v1.9.2.zip` — release archive

## Command-line mode

```bash
java -jar target/gekitotsu_java-1.9.2.jar --cli
```

The CLI reads `1P.txt` and `2P.txt` (one fortress per line, `name&code`), then shows an interactive menu:

| Option | Action |
|--------|--------|
| `0` | Start all-vs-all battles and write `result.txt` / `simple_result.txt` |
| `1` | Set the frame limit |
| `2` | Toggle HP score recording |
| `3` | Reload fortress files |
| `4` | Set the thread count |
| `9` | Exit |

## Feature guide

### 1. Battle Simulation

Runs every pairing of the 1P and 2P fortress lists and reports win-rate statistics.

1. **Edit fortresses** — paste fortress codes into the `1P阵容` and `2P阵容` tabs. Edits are auto-saved to `1P.txt` and `2P.txt` with a 500 ms debounce.
2. **Set parameters** (auto-wrapping settings bar):
   - **Frame limit** — maximum simulation frames per match (default 65536).
   - **Threads** — number of parallel simulation threads (defaults to the number of CPU cores).
   - **HP score** — include remaining HP in the results.
   - **Word wrap** — wrap long result lines.
   - **Watch** (rightmost button) — launch the native 2D GPU battle viewer (`gkt-render.exe`) loaded with the current 1P and 2P lists (defaults to the first 1P vs first 2P matchup; other pairings can be switched inside the viewer).
3. **Start** — click `开始模拟` / `Start Simulation`. The progress bar and status line show progress and elapsed time.
4. **Review** — the `详细结果` and `简要结果` tabs show the contents of `result.txt` and `simple_result.txt`.

### 2. Behavior Analysis

Shares a split input area at the top (**Original fortress** on the left, **Evaluation set** on the right) across three analysis tabs:

#### Contribution Analysis

Measures how much each ball contributes to your fortress's win rate.

1. **Inputs** — import from `1P.txt` / `2P.txt` or paste directly.
2. **Parameters**:
   - **Exploration rate (%)** — proportion of removal combinations to sample (100% = exhaustive enumeration).
   - **Remove count (n)** — how many balls to remove per combination.
   - **Threads** — worker thread count.
   - The label next to the controls shows the number of combinations to be simulated; combinations above 1,000,000 are rejected.
3. **Start / Stop** — the baseline win rate is computed first, then results stream in through a sliding window with an ETA. You can stop at any time.
4. **Results**:
   - **Unit summary** tab — average win-rate change and average change per point of funds for each ball.
   - **Combination ranking** tab — the best/worst removal combinations sorted by win-rate change or change per point of funds (click a column header to re-sort). When `n = 1`, only the unit summary is shown.
   - Select a row and click `复制` / `Copy` (or double-click it) to copy the fortress code with the selected units removed.

#### Matchup Profile

Summarizes match-outcome patterns across the evaluation set:

- **Core Position** — smoothed win-rate heatmap across opponent core coordinates (`0..276`) with individual sample markers.
- **Unit Sensitivity** — ranks opponent unit types by win-rate difference with low-sample shrinkage.
- **Tempo & Margin** — battle duration distribution with win-rate trend and winning-side remaining HP distribution.
- **Acceleration Affinity** — win rate grouped by opponent acceleration level.
- **Keener Correction** — estimates opponent strength via an internal evaluation-set round-robin (cached persistently in `strength_cache.bin`) and reports Keener-corrected win rate and ranking.

#### Battlefield Space (requires Rust engine)

Runs Rust telemetry aggregation over filtered matches (`All` / `Wins` / `Losses` / `Draws` / `Timeouts`):

- **Fortress Damage** — bubble or smoothed slot map showing damage taken or death count across your units and walls.
- **Progression Curve** — time-series curves of remaining HP, alive count, and projectile count grouped by outcome.
- **Opening Pressure** — distribution of first-contact frames and first core-hit frames for both sides.

### 3. Trajectory Prediction

Visualizes fortress movement and assault trajectories to study trajectories and assault mechanics.

- **Lists (left)** — three tabs for trajectories, walls and variables:
  - Add trajectories manually or import them from a fortress code.
  - Variables `a`–`z` can be referenced from trajectory expressions.
  - Click to select, double-click to edit, drag to reorder, Shift/Ctrl+click for multi-select, and click a color swatch to change it.
- **Canvas (right)** — zoom with the mouse wheel (0.1x–5.0x, anchored at the cursor) and drag to pan. The current zoom level is shown at the bottom-right.
- **Shortcuts** — `Delete` delete, `Ctrl+C` copy, `Ctrl+V` paste, `Ctrl+X` cut, `Ctrl+A` select all, `Ctrl+D` deselect. Selections can be copied between items as human-readable text.
- **Trajectory parameters** — name, assault wall x, ball x/y, 1P/2P acceleration level, assault stage count, near/far assault wall, ball code order and color. Coordinates accept integer expressions including variables.

### 4. Fortress Workbench

The creation workbench uses a three-column layout: input/output on the left, preview in the center, and info plus workflow on the right.

#### Editing & parsing

- **Input** — paste a fortress code; the decoded preview updates in real time.
- **Output** — the re-encoded fortress code.
- **➡ button** — copy the output back into the input for iterative edits.
- **Center preview** — layered rendering (assault walls behind, balls in front). Click the background to pick a custom background color.

#### Unit & fortress info

- Below the preview, the selected unit's details (name, HP, CD, AT, cost, coordinates) are shown. Click a unit entry in the `单位列表` list to select it.
- The `要塞信息` tab shows the fortress name, acceleration level and total funds.

#### Built-in analyzer

The workbench automatically detects the following issues and suggests fixes:

- **Overlap** — same-type balls placed at (nearly) identical coordinates.
- **Quickest assault action** — whether balls inside an assault wall use the optimal landing x coordinate for the earliest attack.
- **Wall Ball x-coordinates** — whether Wall Balls (壁玉) sit at recommended positions (54 / 114 / 174 / 234 / 294).
- **Gun Ball angle** — whether a Gun Ball (枪玉) at y = 349 should use angle 2.
- **Healer / Repair Ball position** — whether Heal Balls (愈玉) or Repair Balls (缮玉) appear too late in the unit list.

#### Workflow & effect plugins

The right column hosts a workflow of effects that transform the current fortress.

- **Effect library** — filter with the search box, then double-click or drag an effect into the workflow panel (includes built-in effects such as `Angle Normalize`, which sets wall/core angles to 0).
- **Nodes** — click to toggle an effect on/off (●/×), double-click to edit parameters, right-click for a context menu, long-press and drag to reorder, or select and press `Delete` to remove.
- **Errors** — a failing node shows a red border with a ⚠ icon; hover it to see the message.

#### Writing custom effects

Effect plugin JARs live in `effects/`. Drop a `.java` file onto `effects/build_plugin.bat` to compile and package it as a plugin JAR automatically (JDK 21+ required). `effects/samples/CopyAndRenameEffect.java` is a complete example demonstrating all four parameter types (`INT`, `STRING`, `BOOLEAN`, `UNIT_ID`) plus the `validate` / `execute` interface. Plugin JARs whose filenames start with `_` (such as `_CopyAndRenameEffect.jar`) are disabled by default; remove the `_` prefix to enable them. See `effects/samples/README.md` for details.

### 5. Unit Dex

Browse all 63 units (id 0–62):

- **Left list** — click an entry to view it.
- **Right panel** — sprite (100×100), name, code, a wrapping row of stat tags (HP / CD / AT / cost, etc.), an extended description area loaded from `unit_details_*.json`, and a **Quickest Action calculator**: enter the assault wall x coordinate and whether the ball's code precedes the wall in code order to compute the x coordinates for the earliest possible attack.
- The Quickest Action panel is shown only for units with `cd >= 0 && shoot >= 0` that are not wall-type units.

### 6. Links

Curated Gekitotsu Fortress resources, opened in your browser with one click:

- **Official**: 激突要塞！公式サイト
- **Chinese community**: Baidu Tieba, Gekitotsu Station (激突驿站), Moegirlpedia, the competition QQ group
- **Japanese community**: Discord, 青茶の要塞研究所, wikiwiki, Seesaa wiki, message board

## Fortress code format

### File format

`1P.txt` / `2P.txt` contain one fortress per line:

```
FortressName&Code
```

### Encoding

The code is base-61 with the following alphabet:

```
0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ
```

That is `0-9`, `a-z`, `A-Z` — 61 characters in total.

Each unit is encoded as its id followed by x / y / radius-angle, and the first five characters after the leading core type describe the core position. HP information can be appended after a `#` separator. `Main.compileFort()` parses the code, `to_xyr()` decodes the coordinates, and `Formation.encodeHp()` / `decodeHp()` handle HP encoding.

### File encoding

Input files may be UTF-8 or GBK; the reader auto-detects a BOM and decodes accordingly. All output is UTF-8.

## FAQ

**Q1. Why do simulation results differ from the original game?**
The original game runs on the Flash engine, while this project uses a Java engine. In some places the engine uses collision rules that differ from the original to speed up simulation.

**Q2. Do special techniques that rely on quirks of the original game still work?**
The simulator reproduces techniques such as Box Assault (箱突), Double Repair-Heal (二倍缮愈), Two-Stage Assault (二段突击) and Impact Drop (撞击下落), which cover most practical cases. Extremely precise techniques (e.g. four-stage assault or other delicate structures) may differ slightly from the original game.

**Q3. "formula.json not found" at startup.**
The resource file is missing. When running from source, make sure that all files from `src/main/resources` are present under `target/classes` (running `build_exe.bat` copies them automatically; IntelliJ IDEA usually does too).

**Q4. Simulation results look wrong.**
Check the fortress code format (`name&code`) and the unit coordinates. Validate the code with the workbench parser first.

**Q5. An effect plugin does not appear.**
Make sure the plugin JAR is in the `effects/` directory, implements the `Effect` interface and is registered via `ServiceLoader`. See `effects/samples/CopyAndRenameEffect.java`. A plugin JAR whose filename starts with `_` is disabled.

**Q6. Parts of the UI did not refresh after switching themes.**
Theme switching refreshes all tabs. If anything still looks wrong, restart the application.

**Q7. How do I build the EXE?**
Run `build_exe.bat` in the project root (if `native/gkt-render.exe` has been built via `build_native.ps1`, it will be bundled automatically). The output goes to `dist/`:

- `dist/激突Kit/激突Kit.exe` — application image
- `dist/激突Kit v1.9.2.zip` — release archive

The first build is slow because a JRE runtime is generated; later builds reuse `dist/runtime_cache`.

## Credits

Created by **XX**, **DeepSeek**, **15222HGH** and **MKTL**.

Gekitotsu Fortress (激突要塞) and all game assets belong to their original authors. This project is an unofficial fan work.

## License

Released under the [MIT License](LICENSE).
