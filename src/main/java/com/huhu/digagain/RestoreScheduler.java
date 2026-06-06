package com.huhu.digagain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class RestoreScheduler {

    private static final int UNLOADED_CHUNK_RETRY_TICKS = 20;

    private final DigAgainConfig config;
    private final Map<Integer, TreeMap<Long, List<RestoreEntry>>> entriesByDimension = new HashMap<Integer, TreeMap<Long, List<RestoreEntry>>>();
    private int queueSize;

    public RestoreScheduler(DigAgainConfig config) {
        this.config = config;
    }

    public void enqueue(World world, int x, int y, int z, Block block, int metadata, NBTTagCompound tileNbt) {
        if (queueSize >= config.maxQueuedRestores) {
            return;
        }
        addEntry(
            new RestoreEntry(
                world.provider.dimensionId,
                x,
                y,
                z,
                block,
                metadata,
                tileNbt,
                world.getTotalWorldTime() + config.delayTicks));
    }

    public int getQueueSize() {
        return queueSize;
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world == null || event.world.isRemote) {
            return;
        }
        process(event.world);
    }

    private void process(World world) {
        TreeMap<Long, List<RestoreEntry>> entriesByTick = entriesByDimension
            .get(Integer.valueOf(world.provider.dimensionId));
        if (entriesByTick == null || entriesByTick.isEmpty()) {
            return;
        }

        long now = world.getTotalWorldTime();
        int processed = 0;
        List<RestoreEntry> delayed = new ArrayList<RestoreEntry>();
        Iterator<Map.Entry<Long, List<RestoreEntry>>> iterator = entriesByTick.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, List<RestoreEntry>> bucket = iterator.next();
            if (bucket.getKey()
                .longValue() > now || processed >= config.maxRestoresPerTick) {
                break;
            }
            List<RestoreEntry> bucketEntries = bucket.getValue();
            queueSize -= bucketEntries.size();
            for (int i = 0; i < bucketEntries.size(); i++) {
                RestoreEntry entry = bucketEntries.get(i);
                if (processed >= config.maxRestoresPerTick) {
                    delayRemaining(bucketEntries, i, now + 1, delayed);
                    break;
                }
                processed++;
                RestoreResult result = restore(world, entry);
                if (result == RestoreResult.DEFER) {
                    entry.dueTick = now + UNLOADED_CHUNK_RETRY_TICKS;
                    delayed.add(entry);
                }
            }
            iterator.remove();
        }
        for (RestoreEntry entry : delayed) {
            addEntry(entry);
        }
        if (entriesByTick.isEmpty()) {
            entriesByDimension.remove(Integer.valueOf(world.provider.dimensionId));
        }
    }

    private void delayRemaining(List<RestoreEntry> entries, int startIndex, long dueTick, List<RestoreEntry> delayed) {
        for (int i = startIndex; i < entries.size(); i++) {
            RestoreEntry entry = entries.get(i);
            entry.dueTick = dueTick;
            delayed.add(entry);
        }
    }

    private RestoreResult restore(World world, RestoreEntry entry) {
        if (!world.blockExists(entry.x, entry.y, entry.z)) {
            return config.deferUnloadedChunks ? RestoreResult.DEFER : RestoreResult.SKIP;
        }
        if (config.requireAirOrReplaceable && !canReplace(world, entry.x, entry.y, entry.z)) {
            return RestoreResult.SKIP;
        }
        boolean changed = world.setBlock(entry.x, entry.y, entry.z, entry.block, entry.metadata, 3);
        if (!changed) {
            return RestoreResult.SKIP;
        }
        restoreTileEntity(world, entry);
        return RestoreResult.RESTORED;
    }

    private boolean canReplace(World world, int x, int y, int z) {
        return world.isAirBlock(x, y, z) || world.getBlock(x, y, z)
            .isReplaceable(world, x, y, z);
    }

    private void restoreTileEntity(World world, RestoreEntry entry) {
        if (entry.tileNbt == null) {
            return;
        }
        TileEntity tileEntity = world.getTileEntity(entry.x, entry.y, entry.z);
        if (tileEntity == null) {
            return;
        }
        NBTTagCompound nbt = (NBTTagCompound) entry.tileNbt.copy();
        nbt.setInteger("x", entry.x);
        nbt.setInteger("y", entry.y);
        nbt.setInteger("z", entry.z);
        tileEntity.readFromNBT(nbt);
        tileEntity.markDirty();
        world.markBlockForUpdate(entry.x, entry.y, entry.z);
    }

    private void addEntry(RestoreEntry entry) {
        TreeMap<Long, List<RestoreEntry>> entriesByTick = getDimensionQueue(entry.dimensionId);
        List<RestoreEntry> entries = entriesByTick.get(Long.valueOf(entry.dueTick));
        if (entries == null) {
            entries = new ArrayList<RestoreEntry>();
            entriesByTick.put(Long.valueOf(entry.dueTick), entries);
        }
        entries.add(entry);
        queueSize++;
    }

    private TreeMap<Long, List<RestoreEntry>> getDimensionQueue(int dimensionId) {
        TreeMap<Long, List<RestoreEntry>> entriesByTick = entriesByDimension.get(Integer.valueOf(dimensionId));
        if (entriesByTick == null) {
            entriesByTick = new TreeMap<Long, List<RestoreEntry>>();
            entriesByDimension.put(Integer.valueOf(dimensionId), entriesByTick);
        }
        return entriesByTick;
    }

    private enum RestoreResult {
        RESTORED,
        SKIP,
        DEFER
    }

    private static class RestoreEntry {

        private final int dimensionId;
        private final int x;
        private final int y;
        private final int z;
        private final Block block;
        private final int metadata;
        private final NBTTagCompound tileNbt;
        private long dueTick;

        private RestoreEntry(int dimensionId, int x, int y, int z, Block block, int metadata, NBTTagCompound tileNbt,
            long dueTick) {
            this.dimensionId = dimensionId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.metadata = metadata;
            this.tileNbt = tileNbt;
            this.dueTick = dueTick;
        }
    }
}
