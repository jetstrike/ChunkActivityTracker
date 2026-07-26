package toni.chunkactivitytracker.pruning;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelResource;
import toni.chunkactivitytracker.ChunkActivityTracker;
import toni.chunkactivitytracker.data.ChunkActivityInfo;
import toni.chunkactivitytracker.data.ChunkActivityMap;
import toni.chunkactivitytracker.foundation.config.AllConfigs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PruningEngine {

    public static class PruneSummary {
        public String dimension;
        public int totalLogged;
        public int anchorCount;
        public int protectedCount;
        public int targetCount;
        public String jsonReportPath;
        public String csvExportPath;

        public PruneSummary(String dimension, int totalLogged, int anchorCount, int protectedCount, int targetCount, String jsonReportPath, String csvExportPath) {
            this.dimension = dimension;
            this.totalLogged = totalLogged;
            this.anchorCount = anchorCount;
            this.protectedCount = protectedCount;
            this.targetCount = targetCount;
            this.jsonReportPath = jsonReportPath;
            this.csvExportPath = csvExportPath;
        }
    }

    public static List<PruneSummary> runDryRunOnAllDimensions() {
        List<PruneSummary> summaries = new ArrayList<>();
        if (ChunkActivityMap.instances == null || ChunkActivityMap.instances.isEmpty()) {
            ChunkActivityTracker.LOGGER.warn("No loaded dimension activity maps found for pruning analysis.");
            return summaries;
        }

        for (ChunkActivityMap map : ChunkActivityMap.instances.values()) {
            PruneSummary summary = analyzeAndExport(map);
            if (summary != null) {
                summaries.add(summary);
            }
        }
        return summaries;
    }

    public static PruneSummary analyzeAndExport(ChunkActivityMap map) {
        String dim = map.getDimension();
        if (dim == null) dim = "unknown";

        Map<Long, ChunkActivityInfo> chunks = map.getChunks();
        if (chunks == null || chunks.isEmpty()) {
            return new PruneSummary(dim, 0, 0, 0, 0, "none", "none");
        }

        int anchorTimeThreshold = AllConfigs.server().anchorTimeThresholdSeconds.get();
        int initialRadius = AllConfigs.server().initialPruneRadius.get();
        int stepDays = Math.max(1, AllConfigs.server().inactivityDurationDays.get());
        float divisor = Math.max(1.0f, AllConfigs.server().pruneRangeDivisor.get());
        int minRadius = Math.max(0, AllConfigs.server().minProtectedRadius.get());
        long now = System.currentTimeMillis();

        Set<Long> protectedChunks = new HashSet<>();
        List<Long> anchors = new ArrayList<>();

        // Step 1: Discover Anchors and build protection radius rings
        for (Map.Entry<Long, ChunkActivityInfo> entry : chunks.entrySet()) {
            long posLong = entry.getKey();
            ChunkActivityInfo info = entry.getValue();

            long totalTime = info.getPlayerTimeMap().values().stream().mapToLong(Long::longValue).sum();
            int totalBlocks = info.getBlocksPlacedMap().values().stream().mapToInt(Integer::intValue).sum();

            boolean isAnchor = totalTime >= anchorTimeThreshold || totalBlocks > 0;
            if (isAnchor) {
                anchors.add(posLong);
                long lastVisited = info.getLastVisitedEpoch();
                long daysInactive = Math.max(0, (now - lastVisited) / (1000L * 60 * 60 * 24));
                long erosionSteps = daysInactive / stepDays;

                int radius = initialRadius;
                for (long i = 0; i < erosionSteps && radius > minRadius; i++) {
                    radius = Math.round(radius / divisor);
                }
                radius = Math.max(radius, minRadius);

                ChunkPos anchorPos = new ChunkPos(posLong);
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        protectedChunks.add(new ChunkPos(anchorPos.x + dx, anchorPos.z + dz).toLong());
                    }
                }
            }
        }

        // Step 2: Identify pruning target candidates outside protected boundaries
        List<ChunkPos> targetPositions = new ArrayList<>();
        List<String> jsonEntries = new ArrayList<>();

        for (Map.Entry<Long, ChunkActivityInfo> entry : chunks.entrySet()) {
            long posLong = entry.getKey();
            if (!protectedChunks.contains(posLong)) {
                ChunkActivityInfo info = entry.getValue();
                int totalBlocks = info.getBlocksPlacedMap().values().stream().mapToInt(Integer::intValue).sum();
                // Ensure double safety: zero blocks placed in targeted chunks
                if (totalBlocks == 0) {
                    ChunkPos cPos = new ChunkPos(posLong);
                    targetPositions.add(cPos);

                    long totalTime = info.getPlayerTimeMap().values().stream().mapToLong(Long::longValue).sum();
                    long daysInactive = Math.max(0, (now - info.getLastVisitedEpoch()) / (1000L * 60 * 60 * 24));
                    jsonEntries.add(String.format("    { \"x\": %d, \"z\": %d, \"secondsPlayed\": %d, \"daysInactive\": %d }", cPos.x, cPos.z, totalTime, daysInactive));
                }
            }
        }

        // Step 3: Write report files
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path reportsDir = ChunkActivityTracker.getWorldPath(new LevelResource("chunk_activity_info/reports/"));
        if (reportsDir == null) {
            ChunkActivityTracker.LOGGER.error("Could not resolve reports directory path.");
            return new PruneSummary(dim, chunks.size(), anchors.size(), protectedChunks.size(), targetPositions.size(), "error", "error");
        }
        reportsDir.toFile().mkdirs();

        String safeDimName = dim.replace("/", "_").replace(":", "_");
        Path jsonPath = reportsDir.resolve("dryrun_" + safeDimName + "_" + timestamp + ".json");
        Path csvPath = reportsDir.resolve("mcaselector_" + safeDimName + "_" + timestamp + ".csv");

        // Write JSON Audit Report
        try (BufferedWriter writer = Files.newBufferedWriter(jsonPath)) {
            writer.write("{\n");
            writer.write(String.format("  \"dimension\": \"%s\",\n", dim));
            writer.write(String.format("  \"timestamp\": \"%s\",\n", timestamp));
            writer.write(String.format("  \"totalLoggedChunks\": %d,\n", chunks.size()));
            writer.write(String.format("  \"anchorChunks\": %d,\n", anchors.size()));
            writer.write(String.format("  \"protectedChunkBufferSetSize\": %d,\n", protectedChunks.size()));
            writer.write(String.format("  \"targetedForPruning\": %d,\n", targetPositions.size()));
            writer.write("  \"targets\": [\n");
            writer.write(String.join(",\n", jsonEntries));
            writer.write("\n  ]\n");
            writer.write("}\n");
        } catch (IOException e) {
            ChunkActivityTracker.LOGGER.error("Failed to write dryrun JSON report: " + e.getMessage());
        }

        // Write MCASelector Selection CSV
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath)) {
            for (ChunkPos pos : targetPositions) {
                writer.write(pos.x + "," + pos.z);
                writer.newLine();
            }
        } catch (IOException e) {
            ChunkActivityTracker.LOGGER.error("Failed to write MCASelector CSV file: " + e.getMessage());
        }

        ChunkActivityTracker.LOGGER.info(String.format("Prune analysis for [%s]: %d logged, %d anchors, %d protected, %d targeted.", dim, chunks.size(), anchors.size(), protectedChunks.size(), targetPositions.size()));
        return new PruneSummary(dim, chunks.size(), anchors.size(), protectedChunks.size(), targetPositions.size(), jsonPath.getFileName().toString(), csvPath.getFileName().toString());
    }
}
