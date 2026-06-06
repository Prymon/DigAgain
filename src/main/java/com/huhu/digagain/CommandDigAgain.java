package com.huhu.digagain;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class CommandDigAgain extends CommandBase {

    private final DigAgainConfig config;
    private final GtOreDetector oreDetector;
    private final RestoreScheduler restoreScheduler;

    public CommandDigAgain(DigAgainConfig config, GtOreDetector oreDetector, RestoreScheduler restoreScheduler) {
        this.config = config;
        this.oreDetector = oreDetector;
        this.restoreScheduler = restoreScheduler;
    }

    @Override
    public String getCommandName() {
        return "digagain";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/digagain <reload|chance|delay|maxPerTick|natural|status|probe>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        String subcommand = args[0];
        if ("reload".equalsIgnoreCase(subcommand)) {
            config.load();
            send(sender, "Dig Again config reloaded.");
            return;
        }
        if ("chance".equalsIgnoreCase(subcommand)) {
            requireArgs(sender, args, 2);
            config.setRestoreChance(parseDoubleBounded(sender, args[1], 0.0D, 1.0D));
            send(sender, "restoreChance = " + config.restoreChance);
            return;
        }
        if ("delay".equalsIgnoreCase(subcommand)) {
            requireArgs(sender, args, 2);
            config.setDelayTicks(parseIntBounded(sender, args[1], 1, Integer.MAX_VALUE));
            send(sender, "delayTicks = " + config.delayTicks);
            return;
        }
        if ("maxPerTick".equalsIgnoreCase(subcommand)) {
            requireArgs(sender, args, 2);
            config.setMaxRestoresPerTick(parseIntBounded(sender, args[1], 1, Integer.MAX_VALUE));
            send(sender, "maxRestoresPerTick = " + config.maxRestoresPerTick);
            return;
        }
        if ("natural".equalsIgnoreCase(subcommand)) {
            requireArgs(sender, args, 2);
            config.setRequireNaturalGtOre(parseBoolean(sender, args[1]));
            send(sender, "requireNaturalGtOre = " + config.requireNaturalGtOre);
            return;
        }
        if ("status".equalsIgnoreCase(subcommand)) {
            send(sender, "queuedRestores = " + restoreScheduler.getQueueSize() + " / " + config.maxQueuedRestores);
            return;
        }
        if ("probe".equalsIgnoreCase(subcommand)) {
            probe(sender);
            return;
        }
        throw new WrongUsageException(getCommandUsage(sender));
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                "reload",
                "chance",
                "delay",
                "maxPerTick",
                "natural",
                "status",
                "probe");
        }
        if (args.length == 2 && "natural".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "true", "false");
        }
        return new ArrayList();
    }

    private void probe(ICommandSender sender) {
        if (!(sender instanceof EntityPlayerMP)) {
            send(sender, "probe can only be used by a player.");
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        MovingObjectPosition target = getBlockLookingAt(player);
        if (target == null || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            send(sender, "No block in sight.");
            return;
        }
        World world = player.worldObj;
        int x = target.blockX;
        int y = target.blockY;
        int z = target.blockZ;
        Block block = world.getBlock(x, y, z);
        int metadata = world.getBlockMetadata(x, y, z);
        NBTTagCompound tileNbt = TileEntityUtil.capture(world, x, y, z);
        GtOreDetector.DetectionResult result = oreDetector.detect(block, metadata, tileNbt);
        String registryName = BlockWhitelist.getRegistryName(block);
        send(sender, "block = " + registryName + ":" + metadata);
        send(sender, "dimension = " + world.provider.dimensionId);
        send(sender, "gtOre = " + result.gtOre + ", natural = " + result.natural + ", source = " + result.source);
        if (result.detail != null && result.detail.length() > 0) {
            send(sender, "detail = " + result.detail);
        }
        send(sender, "fallback whitelist entry = " + registryName + ":" + metadata + " or " + registryName + ":*");
    }

    private MovingObjectPosition getBlockLookingAt(EntityPlayerMP player) {
        float pitch = player.rotationPitch;
        float yaw = player.rotationYaw;
        double eyeX = player.posX;
        double eyeY = player.posY + player.getEyeHeight();
        double eyeZ = player.posZ;
        Vec3 start = Vec3.createVectorHelper(eyeX, eyeY, eyeZ);
        float yawCos = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float yawSin = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float pitchCos = -MathHelper.cos(-pitch * 0.017453292F);
        float pitchSin = MathHelper.sin(-pitch * 0.017453292F);
        Vec3 look = Vec3.createVectorHelper(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
        Vec3 end = start.addVector(look.xCoord * 6.0D, look.yCoord * 6.0D, look.zCoord * 6.0D);
        return player.worldObj.func_147447_a(start, end, false, false, true);
    }

    private static void requireArgs(ICommandSender sender, String[] args, int count) {
        if (args.length < count) {
            throw new WrongUsageException("Missing argument.");
        }
    }

    private static void send(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(message));
    }
}
