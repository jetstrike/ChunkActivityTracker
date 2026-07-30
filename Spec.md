# Chunk Activity Tracker — Dedicated NeoForge 1.21.1 Migration Specification

This document provides complete instructions, architecture designs, and feature specifications for rebuilding the **Chunk Activity Tracker** mod as a standalone, dedicated **NeoForge 1.21.1** project in a clean workspace. Provide this document to the AI Agent in your new workspace as the foundational architectural specification.

---

## 1. Project Goal & Architectural Shift
The goal of this project is to create a lightweight, robust Minecraft server utility mod for **NeoForge 1.21.1** that tracks historical player activity per chunk and automates intelligent, radial edge-pruning of abandoned chunks.

### 🚫 What to Avoid (Why We Migrated)
The original upstream repository used an complex multi-loader framework (`toni.blahaj`, `dev.kikugie.stonecutter`, outdated Architectury Loom 1.9, and a custom author dependency `txnilib` hosted on an offline server `maven.txni.dev`). 
* **Do NOT use Stonecutter, Architectury Loom, or multi-loader frameworks.**
* **Do NOT include any external library dependencies like `txnilib` or custom Maven repos.**
* Build entirely using official **NeoGradle / ModDevGradle** for **NeoForge 1.21.1** running under **Java 21**.
* Use standard built-in NeoForge configurations (`ModConfigSpec`) and standard Mojang/NeoForge serialization (`StreamCodec`, `Codec`, and `AttachmentType` / NBT).

---

## 2. Core Features & Technical Specification

### A. Activity Tracking & Data Storage (`ChunkActivityInfo`)
The mod must maintain persistent activity statistics for generated server chunks to calculate heat maps and occupancy history:
* **Tracked Metrics**:
  * `inhabitedTime` (long): Total server ticks players have spent inside or loading the chunk.
  * `lastVisitedEpoch` (long): Unix timestamp (in seconds) of when a player was last present in or near the chunk. Added to protect fast-travel corridors.
* **Storage Mechanism**:
  * Utilize NeoForge 1.21.1 **Data Attachments** (`DeferredRegister<AttachmentType<?>>`) attached directly to `LevelChunk`, or a central world-saved `SavedData` index (`ChunkActivityMap`).
  * Integrate standard 1.21.1 `StreamCodec` and `Codec` implementations for robust network syncing and disk persistence.
* **Event Listeners**:
  * Listen to `PlayerEvent.PlayerTickEvent` or `ChunkEvent.Load` to increment `inhabitedTime` and update `lastVisitedEpoch` to current Unix time whenever a player enters or travels across a chunk (ensuring fast-travel routes via airships remain marked as recently visited).

### B. Radial Edge Erosion Pruning Engine (`PruningEngine`)
On servers where players travel rapidly via airships or high-speed transit, traditional inactivity timers cause flight corridors to regenerate constantly. To resolve this, implement a **Radial Buffer Erosion** system:
1. **Core Protection Rules**:
   * Identify all **Active Core Chunks**: Chunks with `inhabitedTime > inactivityThresholdTicks` (e.g., >1200 ticks / 1 minute of dwell time) are marked as active bases or destinations.
   * Protect a radial surrounding buffer range (`pruneRange`, e.g., radius of 21 chunks) around every Active Core Chunk to safeguard building extensions and immediate approaches.
2. **Time-Based Radial Erosion**:
   * Over scheduled inactivity intervals (evaluated via `lastVisitedEpoch`), abandoned outer buffer edges erode inward gradually rather than all at once.
   * *Example Schedule*: After 1 week of zero visits, prune peripheral chunks `>15 chunks` away from the core; after 2 weeks, prune chunks `>10 chunks` away, shrinking inward toward the persistent core while preserving active flight paths.
3. **MCASelector Integration (Safe Pruning)**:
   * Rather than forcefully deleting active disk region files during gameplay (which risks server data corruption), the `PruningEngine` exports an **MCASelector-compatible CSV selection report**.
   * Format: `r.<regionX>.<regionZ>.mca;chunkX;chunkZ`
   * Saved automatically to `config/chunkactivitytracker/prune_reports/mca_prune_<timestamp>.csv`.
   * Admins can then feed this CSV directly into MCASelector or automated offline maintenance scripts to safely strip abandoned chunks.

### C. Server Configuration System (NeoForge `ModConfigSpec`)
Recreate the config system using built-in NeoForge server configs (saved to `config/chunkactivitytracker-server.toml`):
* `inactivityThresholdTicks` (Int, default: `1200`): Minimum ticks required for a chunk to be designated an active core chunk.
* `pruneRange` (Int, default: `21`): Radial chunk distance protected around an active core chunk.
* `pruneRangeDivisor` (Int, default: `1`): Divider parameter to scale erosion speed over consecutive scheduled pruning cycles.
* `dryRunMode` (Boolean, default: `true`): Safety toggle. When active, automatic maintenance schedules only log and export CSV reports without marking disk files for direct deletion.
* `pruningScheduleDays` (Int, default: `7`): Interval in days between periodic erosion evaluation routines.

### D. Admin Commands (`/cat`)
Register administrative commands under the command root `/cat` using Brigadier:
* `/cat status [<x> <z>]`: 
  * Checks the specified chunk (or the chunk the player is currently occupying).
  * Returns formatted chat output displaying: Inhabited Ticks, Calculated Heat Tier (Low, Medium, High, Core), and Formatted Last Visited Timestamp.
* `/cat dryrun`: 
  * Instantly executes an exhaustive analysis run of the `PruningEngine` across all loaded world regions.
  * Generates a validation log and writes an MCASelector CSV report to disk without modifying any actual chunk data.
  * Confirms completion in chat with the generated report file path and summary count of targeted candidate chunks.

---

## 3. License Attribution & Legal Compliance
The original mod was created by Toni under **Toni's MMC License**. When publishing this rewritten mod to your GitHub repository or release distribution, strictly preserve attribution in your `README.md` and `LICENSE` file:
```

Original concept and initial architectures based on ChunkActivityTracker by Toni under Toni's MMC License.
All derivative modifications, pruning engines, and NeoForge port implementations maintain attribution as required.
```

---

## 4. Implementation Step-by-Step for New Workspace Agent
1. **Bootstrap Project**: Initialize a clean NeoForge 1.21.1 project using the official NeoForge starter template (`net.neoforged:moddev-gradle`). Confirm Java 21 compilation via Gradle.
2. **Config & Registration**: Create the mod main entry class and register the `ModConfigSpec` for server parameters.
3. **Attachment Data Structures**: Define `ChunkActivityInfo` class with fields (`inhabitedTime`, `lastVisitedEpoch`). Implement standard Codec serialization and register via NeoForge Attachment Types.
4. **Activity Events**: Hook `PlayerTickEvent` or block interaction/movement events to touch and refresh chunk timestamps during player presence and transit.
5. **Pruning & CSV Export**: Write `PruningEngine.java` containing the radial distance computation, erosion calculation, and CSV file generation formatted for MCASelector.
6. **Command Tree**: Build `CatCommands.java` using standard Minecraft Brigadier nodes and connect it to the server command registration event.
7. **Verify & Build**: Execute `./gradlew runServer` and `./gradlew build` to confirm instant compilation and release artifact assembly.
