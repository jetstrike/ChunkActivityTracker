# Chunk Activity Tracker & Automated Pruner

A high-performance server activity tracking and automated world maintenance framework for Minecraft, built for multi-loader support (Fabric, Forge, NeoForge). 

This mod silently records player residence time and building interactions per chunk, utilizing spatial algorithms and time-based edge erosion to allow safe world pruning without endangering player builds or active airship transit highways.

---

## ⚖️ License & Attribution Notice

This project is a modified fork of the original **Chunk Activity Tracker** created by **Toni (@txnimc)**, and is distributed strictly under **Toni's MMC License**. See [LICENSE.md](LICENSE.md) for full terms. 

When distributing or maintaining this mod or any derivatives, the following key conditions must be observed:
1. **Monetized Platforms (Modrinth / CurseForge)**: 50% of any revenue generated from forking or re-uploading this mod to monetized platforms must go to the original author (Toni).
2. **Prohibited Sites**: Re-uploading this mod to hosting platforms without author compensation (such as PlanetMinecraft) is strictly prohibited.
3. **Modpack Distribution**: Jar files cannot be directly distributed inside modpacks; they must be downloaded via Modrinth or CurseForge dependencies.
4. **License Preservation**: This software cannot be relicensed, and this copyright notice must accompany any modified code.

---

## 🌟 Features & Architecture

### 1. Activity & Timestamp Tracking
* **Time Spent**: Accurately records residence time (in seconds) per player UUID inside occupied chunks.
* **Block Usage & Placements**: Tracks total block interactions per player UUID, guaranteeing structural build protection.
* **Recency Epochs**: Maintains exact millisecond timestamp records of when a chunk was last occupied or modified (`lastVisitedEpoch`).

### 2. Dilating Buffer Zones & Time-Based Edge Erosion
Designed specifically for servers running airship mods (e.g., Valkyrien Skies, Eureka, Clockwork) or fast exploration tools, where deleting traversed transit corridors would otherwise cause severe terrain regeneration lag:
* **Active Anchor Buffers**: Any chunk with recorded building activity or high residence time maintains a wide concentric protection radius (e.g., 21 chunks / 336 blocks), ensuring smooth lag-free flight departures and arrivals around bases.
* **Inactivity Edge Erosion**: As outposts go dormant, peripheral buffer zones progressively shrink over customizable dormancy intervals (e.g., 21 -> 15 -> 10 chunks after consecutive inactive weeks), slowly releasing historical exploration bloat while permanently protecting an irreducible structural core.

---

## 📖 Usage Guide & Configuration

### Server Configuration
Configuration files are automatically generated under your server config directory (e.g., `chunkactivitytracker-server.toml`):

| Config Parameter | Default | Description |
| :--- | :---: | :--- |
| `enableAutomatedPruning` | `false` | Master authorization switch for scheduled automated pruning routines. |
| `dryRun` | `true` | **Simulation Mode**: When set to `true`, zero filesystem deletions occur. Generates audit reports and MCASelector selection CSVs instead. |
| `anchorTimeThresholdSeconds`| `60` | Minimum residence seconds required for an unmodified chunk to qualify as a protected anchor. |
| `initialPruneRadius` | `21` | Starting protective buffer distance (in chunks) encompassing active anchors. |
| `inactivityDurationDays` | `7` | Days of unbroken dormancy required before initiating a perimeter contraction step. |
| `pruneRangeDivisor` | `1.4` | Scaling factor applied to shrink buffer ring radii after each dormancy interval. |
| `minProtectedRadius` | `5` | Immutable minimum buffer distance permanently kept around structural anchor builds. |
| `pruningIntervalHours` | `168` | Frequency in hours for running background analysis loops. |

---

## 🛡️ Zero-Risk Validation Workflows

To prevent accidental data loss, never execute direct deletions on a production world without visual verification.

### In-Game Slash Commands
* `/cat status` — Queries the current chunk's recorded statistics (total time played, block placement counts, days since last visit, and anchor classification).
* `/cat dryrun` *(Requires OP level 2)* — Triggers immediate spatial buffer analysis across all loaded dimensions. Generates audit logs and exports selection files without modifying world data.

### Visual Verification via MCASelector (Recommended)
1. Run `/cat dryrun` in game or wait for an automated dry-run cycle to finish.
2. Locate the exported CSV file in your world directory under: `<world>/chunk_activity_info/reports/mcaselector_<dimension>_<timestamp>.csv`.
3. Open your world folder on your desktop using the **[MCASelector](https://github.com/Querz/mcaselector)** graphical tool.
4. Go to **Selection -> Import Selection** and pick the exported CSV file.
5. Every targeted chunk will highlight directly over your interactive satellite world map, allowing visual confirmation that active bases and flight lanes remain unselected before running permanent deletions.

---

## 🔌 Developer & Scripting API (KubeJS)

Addons or server scripts can pull real-time activity metrics using static API exposures:

```javascript
const ChunkActivityTracker = Java.loadClass('toni.chunkactivitytracker.ChunkActivityTracker');
const ChunkActivityMap = Java.loadClass('toni.chunkactivitytracker.data.ChunkActivityMap');

PlayerEvents.chat(event => {
    if (event.message === '!mytime') {
        let seconds = ChunkActivityTracker.getSecondsInCurrentChunk(event.player.minecraftPlayer);
        event.player.tell(`You have logged ${seconds} seconds in this chunk!`);
    }
});
```

---

## 🛠️ Building & Development
This mod leverages the Blahaj Gradle Plugin & Stonecutter for multiversion cross-loader development.
To build across targeted platforms:
```bash
./gradlew build
```
