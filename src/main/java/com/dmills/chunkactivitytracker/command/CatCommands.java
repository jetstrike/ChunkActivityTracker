package com.dmills.chunkactivitytracker.command;

import com.dmills.chunkactivitytracker.config.CatServerConfig;
import com.dmills.chunkactivitytracker.data.ChunkActivityInfo;
import com.dmills.chunkactivitytracker.engine.PruningEngine;
import com.dmills.chunkactivitytracker.registry.CatAttachments;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class CatCommands {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("cat")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(context -> checkStatus(context, null, null))
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(context -> checkStatus(context, 
                                                IntegerArgumentType.getInteger(context, "x"), 
                                                IntegerArgumentType.getInteger(context, "z")))
                                )
                        )
                )
                .then(Commands.literal("dryrun")
                        .executes(this::runDryRun)
                )
        );
    }

    private int checkStatus(CommandContext<CommandSourceStack> context, Integer x, Integer z) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        int chunkX, chunkZ;
        if (x != null && z != null) {
            chunkX = x;
            chunkZ = z;
        } else if (source.getEntity() != null) {
            chunkX = source.getEntity().blockPosition().getX() >> 4;
            chunkZ = source.getEntity().blockPosition().getZ() >> 4;
        } else {
            source.sendFailure(Component.literal("You must specify <x> and <z> chunk coordinates when running from console or command block."));
            return 0;
        }

        ChunkAccess chunkAccess = level.getChunkSource().getChunk(chunkX, chunkZ, false);
        if (!(chunkAccess instanceof LevelChunk chunk)) {
            source.sendFailure(Component.literal("Chunk (" + chunkX + ", " + chunkZ + ") is currently not loaded in memory."));
            return 0;
        }

        ChunkActivityInfo info = chunk.getData(CatAttachments.CHUNK_ACTIVITY_INFO);
        long inhabitedTicks = info.getInhabitedTime();
        long lastVisitedEpoch = info.getLastVisitedEpoch();

        int threshold = Math.max(1, CatServerConfig.INACTIVITY_THRESHOLD_TICKS.get());
        String heatTier;
        if (inhabitedTicks >= threshold) {
            heatTier = "§dCore (Active Base)§r";
        } else if (inhabitedTicks >= threshold * 0.6) {
            heatTier = "§cHigh§r";
        } else if (inhabitedTicks >= threshold * 0.25) {
            heatTier = "§6Medium§r";
        } else {
            heatTier = "§7Low§r";
        }

        String formattedTime = lastVisitedEpoch > 0 
                ? Instant.ofEpochSecond(lastVisitedEpoch).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "Never / No data";

        source.sendSuccess(() -> Component.literal(String.format("§b[CAT Status]§r Chunk (§e%d, %d§r):\n  §7• Inhabited Ticks:§r %d\n  §7• Calculated Heat Tier:§r %s\n  §7• Last Visited:§r %s", 
                chunkX, chunkZ, inhabitedTicks, heatTier, formattedTime)), false);

        return 1;
    }

    private int runDryRun(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§e[CAT] Initiating exhaustive radial pruning analysis across loaded world regions...§r"), true);

        PruningEngine.PruningResult result = PruningEngine.runPruningAnalysis(source.getServer(), true);

        source.sendSuccess(() -> Component.literal(String.format("§a[CAT] Dry-run completed successfully!§r\n  §7• Candidate Chunks:§r %d\n  §7• Report File:§r %s", 
                result.getCandidateCount(), result.getReportPath())), true);

        return 1;
    }
}
