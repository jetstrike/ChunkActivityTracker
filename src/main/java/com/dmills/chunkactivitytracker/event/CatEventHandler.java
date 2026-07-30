package com.dmills.chunkactivitytracker.event;

import com.dmills.chunkactivitytracker.data.ChunkActivityInfo;
import com.dmills.chunkactivitytracker.engine.PruningEngine;
import com.dmills.chunkactivitytracker.registry.CatAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class CatEventHandler {

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Only update every 20 ticks (1 second) to minimize processing overhead while remaining precise
        if (serverLevel.getGameTime() % 20 != 0) {
            return;
        }

        ChunkAccess chunkAccess = serverLevel.getChunkSource().getChunk(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4, false);
        if (chunkAccess instanceof LevelChunk chunk) {
            ChunkActivityInfo info = chunk.getData(CatAttachments.CHUNK_ACTIVITY_INFO);
            info.incrementInhabitedTime(20);
            info.touch();
            chunk.setUnsaved(true);
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide() || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        if (event.getLevel() instanceof ServerLevel serverLevel) {
            PruningEngine.onChunkLoad(serverLevel, chunk);

            ChunkPos chunkPos = chunk.getPos();
            boolean playerNear = serverLevel.players().stream().anyMatch(p -> 
                Math.abs((p.blockPosition().getX() >> 4) - chunkPos.x) <= 4 && Math.abs((p.blockPosition().getZ() >> 4) - chunkPos.z) <= 4);
            
            if (playerNear) {
                ChunkActivityInfo info = chunk.getData(CatAttachments.CHUNK_ACTIVITY_INFO);
                info.touch();
                chunk.setUnsaved(true);
            }
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel serverLevel && event.getChunk() instanceof LevelChunk chunk) {
            PruningEngine.onChunkUnload(serverLevel, chunk.getPos());
        }
    }
}
