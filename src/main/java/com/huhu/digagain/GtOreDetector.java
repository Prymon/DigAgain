package com.huhu.digagain;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

public class GtOreDetector {

    private static final int NEW_NATURAL_ORE_META_OFFSET = 8000;
    private static final int NEW_SMALL_ORE_META_OFFSET = 16000;

    private final DigAgainConfig config;
    private final Class<?> currentBlockOreClass;
    private final Class<?> currentBlockOreAbstractClass;
    private final Class<?> currentTileEntityOresClass;
    private final Class<?> newGtBlockOreClass;
    private final Class<?> legacyGtBlockOreClass;
    private final Object oreAdapter;
    private final Method getOreInfoMethod;

    public GtOreDetector(DigAgainConfig config) {
        this.config = config;
        currentBlockOreClass = loadClass("gregtech.common.blocks.BlockOres");
        currentBlockOreAbstractClass = loadClass("gregtech.common.blocks.BlockOresAbstract");
        currentTileEntityOresClass = loadClass("gregtech.common.blocks.TileEntityOres");
        newGtBlockOreClass = loadClass("gregtech.common.blocks.GTBlockOre");
        legacyGtBlockOreClass = loadClass("gregtech.common.blocks.GT_Block_Ores_Abstract");
        Class<?> oreAdapterClass = loadClass("gregtech.common.ores.GTOreAdapter");
        oreAdapter = findOreAdapterInstance(oreAdapterClass);
        getOreInfoMethod = findGetOreInfoMethod(oreAdapterClass);
    }

    public boolean isPossibleTarget(Block block, int metadata) {
        if (config.enableGtOreDetector && isGregTechOreBlock(block)) {
            return true;
        }
        if (config.enableFallbackBlockWhitelist && config.getFallbackWhitelist()
            .matches(block, metadata)) {
            return true;
        }
        return config.enableFallbackOreDictionary;
    }

    public DetectionResult detect(Block block, int metadata, NBTTagCompound tileNbt) {
        if (config.enableGtOreDetector) {
            DetectionResult gtResult = detectGregTechOre(block, metadata, tileNbt);
            if (gtResult.gtOre) {
                return gtResult;
            }
        }
        if (config.enableFallbackBlockWhitelist && config.getFallbackWhitelist()
            .matches(block, metadata)) {
            return DetectionResult.fallback(true, true, "fallback block whitelist");
        }
        if (config.enableFallbackOreDictionary && matchesOreDictionary(block, metadata)) {
            return DetectionResult.fallback(true, true, "fallback ore dictionary");
        }
        return DetectionResult.gt(false, false, "not matched", BlockWhitelist.getRegistryName(block));
    }

    public boolean shouldRestore(Block block, int metadata, NBTTagCompound tileNbt) {
        DetectionResult result = detect(block, metadata, tileNbt);
        return result.gtOre && (!config.requireNaturalGtOre || result.natural);
    }

    private DetectionResult detectGregTechOre(Block block, int metadata, NBTTagCompound tileNbt) {
        if (isInstance(currentBlockOreAbstractClass, block) || isInstance(currentBlockOreClass, block)) {
            Boolean natural = readNaturalFlag(tileNbt);
            return DetectionResult.gt(
                true,
                natural == null || natural.booleanValue(),
                "BlockOresAbstract",
                BlockWhitelist.getRegistryName(block));
        }

        Object oreInfo = getOreInfo(block, metadata);
        if (oreInfo != null) {
            try {
                Boolean natural = readNaturalFlag(oreInfo);
                if (natural == null) {
                    natural = Boolean.valueOf(decodeNewNaturalMetadata(metadata));
                }
                return DetectionResult.gt(
                    true,
                    natural.booleanValue(),
                    "GTOreAdapter",
                    oreInfo.getClass()
                        .getName());
            } finally {
                closeOreInfo(oreInfo);
            }
        }

        if (isInstance(newGtBlockOreClass, block) || isInstance(legacyGtBlockOreClass, block)) {
            Boolean natural = readNaturalFlag(tileNbt);
            if (natural == null) {
                natural = Boolean.valueOf(decodeNewNaturalMetadata(metadata));
            }
            return DetectionResult
                .gt(true, natural.booleanValue(), "GTBlockOre", BlockWhitelist.getRegistryName(block));
        }

        if (isLegacyOreTile(tileNbt)) {
            Boolean natural = readNaturalFlag(tileNbt);
            return DetectionResult.gt(true, natural == null || natural.booleanValue(), "legacy ore tile", "NBT");
        }

        return DetectionResult.gt(false, false, "not matched", BlockWhitelist.getRegistryName(block));
    }

    private boolean isGregTechOreBlock(Block block) {
        return isInstance(currentBlockOreAbstractClass, block) || isInstance(currentBlockOreClass, block)
            || isInstance(newGtBlockOreClass, block)
            || isInstance(legacyGtBlockOreClass, block);
    }

    private Object getOreInfo(Block block, int metadata) {
        if (getOreInfoMethod == null) {
            return null;
        }
        if (!Modifier.isStatic(getOreInfoMethod.getModifiers()) && oreAdapter == null) {
            return null;
        }
        try {
            Object target = Modifier.isStatic(getOreInfoMethod.getModifiers()) ? null : oreAdapter;
            return getOreInfoMethod.invoke(target, block, Integer.valueOf(metadata));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean matchesOreDictionary(Block block, int metadata) {
        Item item = Item.getItemFromBlock(block);
        if (item == null) {
            return false;
        }
        ItemStack stack = new ItemStack(item, 1, metadata);
        int[] ids = OreDictionary.getOreIDs(stack);
        for (int id : ids) {
            String name = OreDictionary.getOreName(id);
            if (name != null && name.length() > 3 && name.startsWith("ore") && Character.isUpperCase(name.charAt(3))) {
                return true;
            }
        }
        return false;
    }

    private static boolean decodeNewNaturalMetadata(int metadata) {
        return metadata >= NEW_NATURAL_ORE_META_OFFSET
            && metadata % NEW_SMALL_ORE_META_OFFSET >= NEW_NATURAL_ORE_META_OFFSET;
    }

    private boolean isLegacyOreTile(NBTTagCompound nbt) {
        if (nbt == null) {
            return false;
        }
        if (currentTileEntityOresClass != null) {
            String id = nbt.getString("id");
            if ("GT_TileEntity_Ores".equals(id) || currentTileEntityOresClass.getName()
                .equals(id)) {
                return true;
            }
        }
        return (nbt.hasKey("m") || nbt.hasKey("mMetaData")) && (nbt.hasKey("n") || nbt.hasKey("mNatural"));
    }

    private static void closeOreInfo(Object oreInfo) {
        if (oreInfo instanceof AutoCloseable) {
            try {
                ((AutoCloseable) oreInfo).close();
            } catch (Exception ignored) {}
        }
    }

    private static Boolean readNaturalFlag(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof NBTTagCompound) {
            NBTTagCompound nbt = (NBTTagCompound) object;
            if (nbt.hasKey("n")) {
                return Boolean.valueOf(nbt.getBoolean("n"));
            }
            if (nbt.hasKey("mNatural")) {
                return Boolean.valueOf(nbt.getBoolean("mNatural"));
            }
            return null;
        }
        Boolean byMethod = readBooleanMethod(object, "isNatural");
        if (byMethod != null) {
            return byMethod;
        }
        byMethod = readBooleanMethod(object, "getNatural");
        if (byMethod != null) {
            return byMethod;
        }
        Boolean byField = readBooleanField(object, "isNatural");
        if (byField != null) {
            return byField;
        }
        byField = readBooleanField(object, "natural");
        if (byField != null) {
            return byField;
        }
        return readBooleanField(object, "mNatural");
    }

    private static Boolean readBooleanMethod(Object object, String methodName) {
        try {
            Method method = object.getClass()
                .getMethod(methodName);
            Object value = method.invoke(object);
            return value instanceof Boolean ? (Boolean) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Boolean readBooleanField(Object object, String fieldName) {
        try {
            Field field = object.getClass()
                .getField(fieldName);
            Object value = field.get(object);
            return value instanceof Boolean ? (Boolean) value : null;
        } catch (Throwable ignored) {
            try {
                Field field = object.getClass()
                    .getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(object);
                return value instanceof Boolean ? (Boolean) value : null;
            } catch (Throwable ignoredAgain) {
                return null;
            }
        }
    }

    private static Object findOreAdapterInstance(Class<?> oreAdapterClass) {
        if (oreAdapterClass == null) {
            return null;
        }
        try {
            Field field = oreAdapterClass.getField("INSTANCE");
            return field.get(null);
        } catch (Throwable ignored) {
            try {
                Field field = oreAdapterClass.getDeclaredField("INSTANCE");
                field.setAccessible(true);
                return field.get(null);
            } catch (Throwable ignoredAgain) {
                return null;
            }
        }
    }

    private static Method findGetOreInfoMethod(Class<?> oreAdapterClass) {
        if (oreAdapterClass == null) {
            return null;
        }
        Method[] methods = oreAdapterClass.getMethods();
        for (Method method : methods) {
            if (method.getName()
                .equals("getOreInfo") && method.getParameterTypes().length == 2) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (acceptsBlockParameter(parameterTypes[0]) && isIntegerParameter(parameterTypes[1])) {
                    return method;
                }
            }
        }
        methods = oreAdapterClass.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName()
                .equals("getOreInfo") && method.getParameterTypes().length == 2) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (acceptsBlockParameter(parameterTypes[0]) && isIntegerParameter(parameterTypes[1])) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        return null;
    }

    private static boolean acceptsBlockParameter(Class<?> type) {
        return type.isAssignableFrom(Block.class) || Block.class.isAssignableFrom(type);
    }

    private static boolean isIntegerParameter(Class<?> type) {
        return type == Integer.TYPE || type == Integer.class;
    }

    private static boolean isInstance(Class<?> type, Object object) {
        return type != null && type.isInstance(object);
    }

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static class DetectionResult {

        public final boolean gtOre;
        public final boolean natural;
        public final String source;
        public final String detail;

        private DetectionResult(boolean gtOre, boolean natural, String source, String detail) {
            this.gtOre = gtOre;
            this.natural = natural;
            this.source = source;
            this.detail = detail;
        }

        private static DetectionResult gt(boolean gtOre, boolean natural, String source, String detail) {
            return new DetectionResult(gtOre, natural, source, detail);
        }

        private static DetectionResult fallback(boolean gtOre, boolean natural, String source) {
            return new DetectionResult(gtOre, natural, source, "");
        }
    }
}
