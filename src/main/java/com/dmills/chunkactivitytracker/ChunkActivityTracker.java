package com.dmills.chunkactivitytracker;

import com.dmills.chunkactivitytracker.command.CatCommands;
import com.dmills.chunkactivitytracker.config.CatServerConfig;
import com.dmills.chunkactivitytracker.event.CatEventHandler;
import com.dmills.chunkactivitytracker.registry.CatAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(ChunkActivityTracker.MOD_ID)
public class ChunkActivityTracker {
    public static final String MOD_ID = "chunkactivitytracker";

    public ChunkActivityTracker(IEventBus modEventBus, ModContainer modContainer) {
        // Register mod attachments to the mod event bus
        CatAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // Register NeoForge server configurations
        modContainer.registerConfig(ModConfig.Type.SERVER, CatServerConfig.SPEC, "chunkactivitytracker-server.toml");

        // Register game event listeners to the NeoForge event bus
        NeoForge.EVENT_BUS.register(new CatEventHandler());
        NeoForge.EVENT_BUS.register(new CatCommands());
    }
}
