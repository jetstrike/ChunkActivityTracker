package com.dmills.chunkactivitytracker.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class ChunkActivityInfo {
    public static final Codec<ChunkActivityInfo> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.LONG.fieldOf("inhabitedTime").forGetter(ChunkActivityInfo::getInhabitedTime),
                    Codec.LONG.fieldOf("lastVisitedEpoch").forGetter(ChunkActivityInfo::getLastVisitedEpoch)
            ).apply(instance, ChunkActivityInfo::new)
    );

    public static final StreamCodec<ByteBuf, ChunkActivityInfo> STREAM_CODEC = StreamCodec.of(
            (buf, info) -> {
                buf.writeLong(info.getInhabitedTime());
                buf.writeLong(info.getLastVisitedEpoch());
            },
            buf -> new ChunkActivityInfo(buf.readLong(), buf.readLong())
    );

    private long inhabitedTime;
    private long lastVisitedEpoch;

    public ChunkActivityInfo() {
        this(0L, 0L);
    }

    public ChunkActivityInfo(long inhabitedTime, long lastVisitedEpoch) {
        this.inhabitedTime = inhabitedTime;
        this.lastVisitedEpoch = lastVisitedEpoch;
    }

    public long getInhabitedTime() {
        return inhabitedTime;
    }

    public void setInhabitedTime(long inhabitedTime) {
        this.inhabitedTime = inhabitedTime;
    }

    public void incrementInhabitedTime(long ticks) {
        this.inhabitedTime += ticks;
    }

    public long getLastVisitedEpoch() {
        return lastVisitedEpoch;
    }

    public void setLastVisitedEpoch(long lastVisitedEpoch) {
        this.lastVisitedEpoch = lastVisitedEpoch;
    }

    public void touch() {
        this.lastVisitedEpoch = System.currentTimeMillis() / 1000L;
    }
}
