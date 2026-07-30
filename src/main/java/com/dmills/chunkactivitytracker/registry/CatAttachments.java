package com.dmills.chunkactivitytracker.registry;

import com.dmills.chunkactivitytracker.ChunkActivityTracker;
import com.dmills.chunkactivitytracker.data.ChunkActivityInfo;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class CatAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = 
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ChunkActivityTracker.MOD_ID);

    public static final Supplier<AttachmentType<ChunkActivityInfo>> CHUNK_ACTIVITY_INFO = ATTACHMENT_TYPES.register(
            "chunk_activity_info",
            () -> AttachmentType.builder(() -> new ChunkActivityInfo(0L, 0L))
                    .serialize(ChunkActivityInfo.CODEC)
                    .build()
    );
}
