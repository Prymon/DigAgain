package com.huhu.digagain;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class TileEntityUtil {

    private TileEntityUtil() {}

    public static NBTTagCompound capture(World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity == null) {
            return null;
        }
        NBTTagCompound nbt = new NBTTagCompound();
        tileEntity.writeToNBT(nbt);
        return nbt;
    }
}
