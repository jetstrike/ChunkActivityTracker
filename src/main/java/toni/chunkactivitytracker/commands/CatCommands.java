package toni.chunkactivitytracker.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import toni.chunkactivitytracker.data.ChunkActivityInfo;
import toni.chunkactivitytracker.data.ChunkActivityMap;
import toni.chunkactivitytracker.foundation.config.AllConfigs;
import toni.chunkactivitytracker.pruning.PruningEngine;

import java.util.List;

public class CatCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("cat")
                .then(Commands.literal("status")
                    .executes(CatCommands::executeStatus)
                )
                .then(Commands.literal("dryrun")
                    .requires(source -> source.hasPermission(2))
                    .executes(CatCommands::executeDryRun)
                )
            );
        });
    }

    private static int executeStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("Only ingame players can check current chunk status."));
            return 0;
        }

        Level level = player.level();
        ChunkPos chunkPos = player.chunkPosition();
        ChunkActivityInfo info = ChunkActivityMap.getChunkInfo(level.dimension(), chunkPos);

        if (info == null) {
            source.sendSuccess(() -> Component.literal(String.format("[%d, %d] No activity recorded in this chunk yet.", chunkPos.x, chunkPos.z)), false);
            return Command.SINGLE_SUCCESS;
        }

        long timeSeconds = info.getPlayerTimeMap().values().stream().mapToLong(Long::longValue).sum();
        int blocksPlaced = info.getBlocksPlacedMap().values().stream().mapToInt(Integer::intValue).sum();
        long lastVisited = info.getLastVisitedEpoch();
        long daysInactive = Math.max(0, (System.currentTimeMillis() - lastVisited) / (1000L * 60 * 60 * 24));

        int threshold = AllConfigs.server().anchorTimeThresholdSeconds.get();
        boolean isAnchor = timeSeconds >= threshold || blocksPlaced > 0;

        source.sendSuccess(() -> Component.literal(String.format("=== Chunk Activity [%d, %d] ===", chunkPos.x, chunkPos.z)), false);
        source.sendSuccess(() -> Component.literal(String.format("Total Time: %d seconds | Blocks Placed/Used: %d", timeSeconds, blocksPlaced)), false);
        source.sendSuccess(() -> Component.literal(String.format("Days Inactive: %d | Anchor Protected: %s", daysInactive, isAnchor ? "YES" : "NO")), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeDryRun(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("Starting automated pruning dry-run simulation across all loaded dimensions..."), true);

        List<PruningEngine.PruneSummary> results = PruningEngine.runDryRunOnAllDimensions();
        if (results.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No loaded dimension activity maps found."), true);
            return Command.SINGLE_SUCCESS;
        }

        for (PruningEngine.PruneSummary summary : results) {
            source.sendSuccess(() -> Component.literal(String.format("[%s] Logged: %d | Anchors: %d | Protected Buffer: %d | Targeted for Deletion: %d",
                    summary.dimension, summary.totalLogged, summary.anchorCount, summary.protectedCount, summary.targetCount)), true);
            source.sendSuccess(() -> Component.literal(String.format("  -> Exported report: %s | MCASelector CSV: %s", summary.jsonReportPath, summary.csvExportPath)), false);
        }

        return Command.SINGLE_SUCCESS;
    }
}
