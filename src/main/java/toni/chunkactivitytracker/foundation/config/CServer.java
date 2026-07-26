package toni.chunkactivitytracker.foundation.config;

import toni.lib.config.ConfigBase;

public class CServer extends ConfigBase {

    public final ConfigBool storeHeightmaps = b(false, "Store Heightmap Info", "Store initial chunk heightmap data. Useful for some mods, disabled by default as it takes up much more space.");

    // Automated Pruning Configurations
    public final ConfigBool enableAutomatedPruning = b(false, "Enable Automated Pruning", "Enable automated calculation and pruning of inactive perimeter chunks.");
    public final ConfigBool dryRun = b(true, "Dry-Run Simulation Mode", "When enabled, zero deletions occur; instead, audit reports and MCASelector selection manifests are generated.");
    public final ConfigInt anchorTimeThresholdSeconds = i(60, 0, 86400, "Anchor Time Threshold", "Minimum total residence time (in seconds) required to classify a chunk as a protected anchor.");
    public final ConfigInt initialPruneRadius = i(21, 1, 100, "Initial Protection Radius", "Protective buffer distance (in chunks) maintained around actively inhabited anchor chunks.");
    public final ConfigInt inactivityDurationDays = i(7, 1, 365, "Inactivity Step Duration (Days)", "Number of days without visits before the surrounding protected radius initiates an erosion step.");
    public final ConfigFloat pruneRangeDivisor = f(1.4f, 1.0f, 5.0f, "Prune Range Divisor", "Divisor applied to shrink the buffer radius after each consecutive inactivity period.");
    public final ConfigInt minProtectedRadius = i(5, 0, 50, "Minimum Safe Core Radius", "The lowest limit to which an active base's buffer can erode over long periods of dormancy.");
    public final ConfigInt pruningIntervalHours = i(168, 1, 8760, "Pruning Interval (Hours)", "Frequency in hours for running the automated world maintenance loop.");

    @Override
    public String getName() {
        return "server";
    }
}
