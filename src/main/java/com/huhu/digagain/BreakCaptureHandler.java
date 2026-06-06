package com.huhu.digagain;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class BreakCaptureHandler {

    private final DigAgainConfig config;
    private final GtOreDetector oreDetector;
    private final RestoreScheduler restoreScheduler;
    private final Random random = new Random();

    public BreakCaptureHandler(DigAgainConfig config, GtOreDetector oreDetector, RestoreScheduler restoreScheduler) {
        this.config = config;
        this.oreDetector = oreDetector;
        this.restoreScheduler = restoreScheduler;
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        World world = event.world;
        if (world == null || world.isRemote) {
            return;
        }
        if (!config.isDimensionAllowed(world.provider.dimensionId)) {
            return;
        }
        if (random.nextDouble() >= config.restoreChance) {
            return;
        }

        Block block = event.block;
        int metadata = world.getBlockMetadata(event.x, event.y, event.z);
        if (!oreDetector.isPossibleTarget(block, metadata)) {
            return;
        }

        NBTTagCompound tileNbt = TileEntityUtil.capture(world, event.x, event.y, event.z);
        if (!oreDetector.shouldRestore(block, metadata, tileNbt)) {
            return;
        }

        restoreScheduler.enqueue(world, event.x, event.y, event.z, block, metadata, tileNbt);
    }
}
