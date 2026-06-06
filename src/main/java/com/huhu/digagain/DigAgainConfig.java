package com.huhu.digagain;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class DigAgainConfig {

    private static final String CATEGORY_GENERAL = "general";
    private static final String CATEGORY_DIMENSIONS = "dimensions";
    private static final String CATEGORY_DETECTION = "detection";
    private static final String CATEGORY_BLOCKS = "blocks";

    private final File file;
    private Configuration configuration;
    private BlockWhitelist fallbackWhitelist = new BlockWhitelist(new String[0]);
    private Set<Integer> dimensionWhitelist = new HashSet<Integer>();

    public double restoreChance;
    public int delayTicks;
    public int maxRestoresPerTick;
    public int maxQueuedRestores;
    public boolean requireAirOrReplaceable;
    public boolean deferUnloadedChunks;
    public boolean enableGtOreDetector;
    public boolean requireNaturalGtOre;
    public boolean enableFallbackBlockWhitelist;
    public boolean enableFallbackOreDictionary;

    public DigAgainConfig(File file) {
        this.file = file;
    }

    public void load() {
        configuration = new Configuration(file);
        restoreChance = clamp(
            configuration.get(CATEGORY_GENERAL, "restoreChance", 0.30D)
                .getDouble(0.30D),
            0.0D,
            1.0D);
        delayTicks = Math.max(
            1,
            configuration.get(CATEGORY_GENERAL, "delayTicks", 20)
                .getInt(20));
        maxRestoresPerTick = Math.max(
            1,
            configuration.get(CATEGORY_GENERAL, "maxRestoresPerTick", 256)
                .getInt(256));
        maxQueuedRestores = Math.max(
            1,
            configuration.get(CATEGORY_GENERAL, "maxQueuedRestores", 8192)
                .getInt(8192));
        requireAirOrReplaceable = configuration.get(CATEGORY_GENERAL, "requireAirOrReplaceable", true)
            .getBoolean(true);
        deferUnloadedChunks = configuration.get(CATEGORY_GENERAL, "deferUnloadedChunks", false)
            .getBoolean(false);

        enableGtOreDetector = configuration.get(CATEGORY_DETECTION, "enableGtOreDetector", true)
            .getBoolean(true);
        requireNaturalGtOre = configuration.get(CATEGORY_DETECTION, "requireNaturalGtOre", true)
            .getBoolean(true);
        enableFallbackBlockWhitelist = configuration.get(CATEGORY_DETECTION, "enableFallbackBlockWhitelist", false)
            .getBoolean(false);
        enableFallbackOreDictionary = configuration.get(CATEGORY_DETECTION, "enableFallbackOreDictionary", false)
            .getBoolean(false);

        int[] dimensions = configuration.get(CATEGORY_DIMENSIONS, "whitelist", new int[] { 0 })
            .getIntList();
        dimensionWhitelist = new HashSet<Integer>();
        for (int dimension : dimensions) {
            dimensionWhitelist.add(Integer.valueOf(dimension));
        }

        String[] entries = configuration
            .get(CATEGORY_BLOCKS, "fallbackWhitelist", new String[] { "gregtech:gt.blockores*:*" })
            .getStringList();
        for (int i = 0; i < entries.length; i++) {
            entries[i] = entries[i].replace(" ", "");
        }
        fallbackWhitelist = new BlockWhitelist(entries);

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public boolean isDimensionAllowed(int dimensionId) {
        return dimensionWhitelist.contains(Integer.valueOf(dimensionId));
    }

    public BlockWhitelist getFallbackWhitelist() {
        return fallbackWhitelist;
    }

    public void setRestoreChance(double value) {
        restoreChance = clamp(value, 0.0D, 1.0D);
        setDouble(CATEGORY_GENERAL, "restoreChance", restoreChance);
    }

    public void setDelayTicks(int value) {
        delayTicks = Math.max(1, value);
        setInt(CATEGORY_GENERAL, "delayTicks", delayTicks);
    }

    public void setMaxRestoresPerTick(int value) {
        maxRestoresPerTick = Math.max(1, value);
        setInt(CATEGORY_GENERAL, "maxRestoresPerTick", maxRestoresPerTick);
    }

    public void setRequireNaturalGtOre(boolean value) {
        requireNaturalGtOre = value;
        setBoolean(CATEGORY_DETECTION, "requireNaturalGtOre", requireNaturalGtOre);
    }

    private void setDouble(String category, String key, double value) {
        Property property = configuration.get(category, key, value);
        property.set(value);
        configuration.save();
    }

    private void setInt(String category, String key, int value) {
        Property property = configuration.get(category, key, value);
        property.set(value);
        configuration.save();
    }

    private void setBoolean(String category, String key, boolean value) {
        Property property = configuration.get(category, key, value);
        property.set(value);
        configuration.save();
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
