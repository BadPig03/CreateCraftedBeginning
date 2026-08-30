package net.ty.createcraftedbeginning.foundation;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBNbtUtils {
    private CCBNbtUtils() {
    }

    public static boolean contains(CompoundTag compoundTag, String key) {
        return compoundTag.contains(key);
    }

    public static boolean contains(CompoundTag compoundTag, String key, int expectedType) {
        return compoundTag.contains(key, expectedType);
    }

    public static void remove(CompoundTag compoundTag, String key) {
        compoundTag.remove(key);
    }

    public static boolean isEmpty(CompoundTag compoundTag) {
        return compoundTag.isEmpty();
    }

    public static int getInt(CompoundTag compoundTag, String key) {
        return compoundTag.getInt(key);
    }

    public static int getIntOrDefault(CompoundTag compoundTag, String key, int fallback) {
        if (!contains(compoundTag, key, Tag.TAG_ANY_NUMERIC)) {
            return fallback;
        }
        return getInt(compoundTag, key);
    }

    public static long getLong(CompoundTag compoundTag, String key) {
        return compoundTag.getLong(key);
    }

    public static long getLongOrDefault(CompoundTag compoundTag, String key, long fallback) {
        if (!contains(compoundTag, key, Tag.TAG_ANY_NUMERIC)) {
            return fallback;
        }
        return getLong(compoundTag, key);
    }

    public static float getFloat(CompoundTag compoundTag, String key) {
        return compoundTag.getFloat(key);
    }

    public static float getFloatOrDefault(CompoundTag compoundTag, String key, float fallback) {
        if (!contains(compoundTag, key, Tag.TAG_ANY_NUMERIC)) {
            return fallback;
        }
        return getFloat(compoundTag, key);
    }

    public static double getDouble(CompoundTag compoundTag, String key) {
        return compoundTag.getDouble(key);
    }

    public static double getDoubleOrDefault(CompoundTag compoundTag, String key, double fallback) {
        if (!contains(compoundTag, key, Tag.TAG_ANY_NUMERIC)) {
            return fallback;
        }
        return getDouble(compoundTag, key);
    }

    public static boolean getBoolean(CompoundTag compoundTag, String key) {
        return compoundTag.getBoolean(key);
    }

    public static boolean getBooleanOrDefault(CompoundTag compoundTag, String key, boolean fallback) {
        if (!contains(compoundTag, key, Tag.TAG_BYTE)) {
            return fallback;
        }
        return getBoolean(compoundTag, key);
    }

    public static String getString(CompoundTag compoundTag, String key) {
        return compoundTag.getString(key);
    }

    public static String getStringOrDefault(CompoundTag compoundTag, String key, String fallback) {
        if (!contains(compoundTag, key, Tag.TAG_STRING)) {
            return fallback;
        }
        return getString(compoundTag, key);
    }

    public static CompoundTag getCompound(CompoundTag compoundTag, String key) {
        return compoundTag.getCompound(key);
    }

    public static CompoundTag getCompoundOrEmpty(CompoundTag compoundTag, String key) {
        if (!contains(compoundTag, key, Tag.TAG_COMPOUND)) {
            return new CompoundTag();
        }
        return getCompound(compoundTag, key);
    }

    public static ListTag getList(CompoundTag compoundTag, String key, int expectedElementType) {
        return compoundTag.getList(key, expectedElementType);
    }

    public static long[] getLongArray(CompoundTag compoundTag, String key) {
        return compoundTag.getLongArray(key);
    }

    public static UUID getUUID(CompoundTag compoundTag, String key) {
        return compoundTag.getUUID(key);
    }

    @Nullable
    public static Tag getTag(CompoundTag compoundTag, String key) {
        return compoundTag.get(key);
    }

    public static void putInt(CompoundTag compoundTag, String key, int value) {
        compoundTag.putInt(key, value);
    }

    public static void putLong(CompoundTag compoundTag, String key, long value) {
        compoundTag.putLong(key, value);
    }

    public static void putFloat(CompoundTag compoundTag, String key, float value) {
        compoundTag.putFloat(key, value);
    }

    public static void putDouble(CompoundTag compoundTag, String key, double value) {
        compoundTag.putDouble(key, value);
    }

    public static void putBoolean(CompoundTag compoundTag, String key, boolean value) {
        compoundTag.putBoolean(key, value);
    }

    public static void putString(CompoundTag compoundTag, String key, String value) {
        compoundTag.putString(key, value);
    }

    public static void putLongArray(CompoundTag compoundTag, String key, long[] value) {
        compoundTag.putLongArray(key, value);
    }

    public static void putUUID(CompoundTag compoundTag, String key, UUID value) {
        compoundTag.putUUID(key, value);
    }

    public static void putTag(CompoundTag compoundTag, String key, Tag value) {
        compoundTag.put(key, value);
    }
}
