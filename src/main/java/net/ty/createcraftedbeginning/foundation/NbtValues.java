package net.ty.createcraftedbeginning.foundation;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class NbtValues {
    private NbtValues() {
    }

    public static int getIntOrDefault(CompoundTag compoundTag, String key, int fallback) {
        if (!compoundTag.contains(key, Tag.TAG_ANY_NUMERIC)) {
            return fallback;
        }

        return compoundTag.getInt(key);
    }

    public static long getLongOrDefault(CompoundTag compoundTag, String key, long fallback) {
        if (!compoundTag.contains(key, Tag.TAG_ANY_NUMERIC)) {
            return fallback;
        }

        return compoundTag.getLong(key);
    }

    public static float getFloatOrDefault(CompoundTag compoundTag, String key, float fallback) {
        if (!compoundTag.contains(key, Tag.TAG_ANY_NUMERIC)) {
            return fallback;
        }

        return compoundTag.getFloat(key);
    }

    public static double getDoubleOrDefault(CompoundTag compoundTag, String key, double fallback) {
        if (!compoundTag.contains(key, Tag.TAG_ANY_NUMERIC)) {
            return fallback;
        }

        return compoundTag.getDouble(key);
    }

    public static boolean getBooleanOrDefault(CompoundTag compoundTag, String key, boolean fallback) {
        if (!compoundTag.contains(key, Tag.TAG_BYTE)) {
            return fallback;
        }

        return compoundTag.getBoolean(key);
    }

    public static String getStringOrDefault(CompoundTag compoundTag, String key, String fallback) {
        if (!compoundTag.contains(key, Tag.TAG_STRING)) {
            return fallback;
        }

        return compoundTag.getString(key);
    }

    public static CompoundTag getCompoundOrEmpty(CompoundTag compoundTag, String key) {
        if (!compoundTag.contains(key, Tag.TAG_COMPOUND)) {
            return new CompoundTag();
        }

        return compoundTag.getCompound(key);
    }

}
