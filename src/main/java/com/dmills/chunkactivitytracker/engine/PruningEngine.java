package com.dmills.chunkactivitytracker.engine;

import com.dmills.chunkactivitytracker.config.CatServerConfig;
import com.dmills.chunkactivitytracker.data.ChunkActivityInfo;
import com.dmills.chunkactivitytracker.registry.CatAttachments;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PruningEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(PruningEngine.class);
    private static final Map<ResourceKey<Level>, Map<ChunkPos, LevelChunk>> LOADED_CHUNKS = new ConcurrentHashMap<>();

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        LOADED_CHUNKS.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(chunk.getPos(), chunk);
    }

    public static void onChunkUnload(ServerLevel level, ChunkPos pos) {
        Map<ChunkPos, LevelChunk> map = LOADED_CHUNKS.get(level.dimension());
        if (map != null) {
            map.remove(pos);
        }
    }

    public static PruningResult runPruningAnalysis(MinecraftServer server, boolean isDryRun) {
        long startTime = System.currentTimeMillis();
        long currentTimeEpoch = startTime / 1000L;
        int thresholdTicks = CatServerConfig.INACTIVITY_THRESHOLD_TICKS.get();
        int basePruneRange = CatServerConfig.PRUNE_RANGE.get();
        int divisor = Math.max(1, CatServerConfig.PRUNE_RANGE_DIVISOR.get());
        int scheduleDays = Math.max(1, CatServerConfig.PRUNING_SCHEDULE_DAYS.get());

        List<String> csvLines = new ArrayList<>();
        int candidateCount = 0;

        for (ServerLevel level : server.getAllLevels()) {
            Map<ChunkPos, LevelChunk> chunks = LOADED_CHUNKS.getOrDefault(level.dimension(), Collections.emptyMap());
            if (chunks.isEmpty()) continue;

            List<ChunkPos> activeCoreChunks = new ArrayList<>();
            for (LevelChunk chunk : chunks.values()) {
                ChunkActivityInfo info = chunk.getData(CatAttachments.CHUNK_ACTIVITY_INFO);
                if (info.getInhabitedTime() >= thresholdTicks) {
                    activeCoreChunks.add(chunk.getPos());
                }
            }

            for (LevelChunk chunk : chunks.values()) {
                ChunkPos pos = chunk.getPos();
                if (activeCoreChunks.contains(pos)) {
                    continue;
                }

                ChunkActivityInfo info = chunk.getData(CatAttachments.CHUNK_ACTIVITY_INFO);
                long secondsInactive = Math.max(0L, currentTimeEpoch - info.getLastVisitedEpoch());
                double daysInactive = secondsInactive / (24.0 * 3600.0);
                int periodsInactive = (int) (daysInactive / scheduleDays);

                double currentProtectedRadius = Math.max(0, basePruneRange - (periodsInactive * divisor * 5.0));

                double minDistance = Double.MAX_VALUE;
                for (ChunkPos corePos : activeCoreChunks) {
                    double dist = Math.hypot(pos.x - corePos.x, pos.z - corePos.z);
                    if (dist < minDistance) {
                        minDistance = dist;
                    }
                }

                if (minDistance > currentProtectedRadius) {
                    int regionX = pos.x >> 5;
                    int regionZ = pos.z >> 5;
                    String mcaFilename = "r." + regionX + "." + regionZ + ".mca";
                    csvLines.add(mcaFilename + ";" + pos.x + ";" + pos.z);
                    candidateCount++;
                }
            }
        }

        Path reportPath = null;
        try {
            Path reportDir = FMLPaths.CONFIGDIR.get().resolve("chunkactivitytracker/prune_reports");
            Files.createDirectories(reportDir);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            reportPath = reportDir.resolve("mca_prune_" + timestamp + ".csv");
            Files.write(reportPath, csvLines);
            LOGGER.info("PruningEngine finished analysis in {}ms. Exported {} candidate chunks to {}", 
                    (System.currentTimeMillis() - startTime), candidateCount, reportPath);
        } catch (IOException e) {
            LOGGER.error("Failed to write MCASelector prune report CSV", e);
        }

        return new PruningResult(candidateCount, reportPath != null ? reportPath.toAbsolutePath().toString() : "ERROR_WRITING_FILE");
    }

    public static class PruningResult {
        private final int candidateCount;
        private final String reportPath;

        public PruningResult(int candidateCount, String reportPath) {
            this.candidateCount = candidateCount;
            this.reportPath = reportPath;
        }

        public int getCandidateCount() {
            return candidateCount;
        }

        public String getReportPath() {
            return reportPath;
        }
    }
}
