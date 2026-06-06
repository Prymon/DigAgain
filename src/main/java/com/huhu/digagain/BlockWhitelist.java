package com.huhu.digagain;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;

public class BlockWhitelist {

    private final List<Entry> entries = new ArrayList<Entry>();

    public BlockWhitelist(String[] rawEntries) {
        for (String rawEntry : rawEntries) {
            Entry entry = Entry.parse(rawEntry);
            if (entry != null) {
                entries.add(entry);
            }
        }
    }

    public boolean matches(Block block, int metadata) {
        String registryName = getRegistryName(block);
        if (registryName == null) {
            return false;
        }
        for (Entry entry : entries) {
            if (entry.matches(registryName, metadata)) {
                return true;
            }
        }
        return false;
    }

    public static String getRegistryName(Block block) {
        Object name = Block.blockRegistry.getNameForObject(block);
        return name == null ? null : name.toString();
    }

    private static class Entry {

        private final String blockPattern;
        private final Integer metadata;
        private final boolean prefixPattern;

        private Entry(String blockPattern, Integer metadata, boolean prefixPattern) {
            this.blockPattern = blockPattern;
            this.metadata = metadata;
            this.prefixPattern = prefixPattern;
        }

        private boolean matches(String registryName, int candidateMetadata) {
            boolean blockMatches = prefixPattern ? registryName.startsWith(blockPattern)
                : registryName.equals(blockPattern);
            return blockMatches && (metadata == null || metadata.intValue() == candidateMetadata);
        }

        private static Entry parse(String rawEntry) {
            if (rawEntry == null) {
                return null;
            }
            String entry = rawEntry.trim();
            if (entry.length() == 0 || entry.charAt(0) == '#') {
                return null;
            }
            int metaSeparator = entry.lastIndexOf(':');
            if (metaSeparator <= 0 || metaSeparator == entry.length() - 1) {
                return null;
            }
            String block = entry.substring(0, metaSeparator);
            String meta = entry.substring(metaSeparator + 1);
            boolean prefix = block.endsWith("*");
            if (prefix) {
                block = block.substring(0, block.length() - 1);
            }
            Integer metadata = null;
            if (!"*".equals(meta)) {
                try {
                    metadata = Integer.valueOf(Integer.parseInt(meta));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return new Entry(block, metadata, prefix);
        }
    }
}
