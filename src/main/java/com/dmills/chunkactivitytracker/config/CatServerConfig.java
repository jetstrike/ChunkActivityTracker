package com.dmills.chunkactivitytracker.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CatServerConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue INACTIVITY_THRESHOLD_TICKS;
    public static final ModConfigSpec.IntValue PRUNE_RANGE;
    public static final ModConfigSpec.IntValue PRUNE_RANGE_DIVISOR;
    public static final ModConfigSpec.BooleanValue DRY_RUN_MODE;
    public static final ModConfigSpec.IntValue PRUNING_SCHEDULE_DAYS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");

        INACTIVITY_THRESHOLD_TICKS = builder
                .comment("Minimum inhabited ticks required for a chunk to be designated an active core chunk (default: 1200 ticks / 1 minute of dwell time).")
                .defineInRange("inactivityThresholdTicks", 1200, 0, Integer.MAX_VALUE);

        PRUNE_RANGE = builder
                .comment("Radial chunk distance protected around an active core chunk (default radius of 21 chunks).")
                .defineInRange("pruneRange", 21, 0, 1000);

        PRUNE_RANGE_DIVISOR = builder
                .comment("Divider parameter to scale erosion speed over consecutive scheduled pruning cycles (default: 1).")
                .defineInRange("pruneRangeDivisor", 1, 1, 100);

        DRY_RUN_MODE = builder
                .comment("Safety toggle. When active, automatic maintenance schedules only log and export CSV reports without marking disk files for deletion.")
                .define("dryRunMode", true);

        PRUNING_SCHEDULE_DAYS = builder
                .comment("Interval in days between periodic erosion evaluation routines (default: 7).")
                .defineInRange("pruningScheduleDays", 7, 1, 365);

        builder.pop();
        SPEC = builder.build();
    }
}
