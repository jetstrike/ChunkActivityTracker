# Chunk Activity Tracker (NeoForge 1.21.1)

A lightweight, robust Minecraft server utility mod for **NeoForge 1.21.1** that tracks historical player activity per chunk and automates intelligent, radial edge-pruning of abandoned chunks.

## Features
- **Activity Tracking**: Maintains persistent statistics (`inhabitedTime`, `lastVisitedEpoch`) per generated chunk via standard NeoForge data attachments.
- **Radial Edge Erosion Pruning**: Protects active core bases and their surrounding buffer radiuses while smoothly eroding forgotten outskirts over time.
- **MCASelector Integration**: Generates safe, offline-compatible CSV pruning selection reports without risking live world data corruption.
- **Admin Commands**: In-game inspection and manual pruning dry-run execution via `/cat status` and `/cat dryrun`.

## License & Attribution
```
Original concept and initial architectures based on ChunkActivityTracker by Toni under Toni's MMC License.
All derivative modifications, pruning engines, and NeoForge port implementations maintain attribution as required.
```
