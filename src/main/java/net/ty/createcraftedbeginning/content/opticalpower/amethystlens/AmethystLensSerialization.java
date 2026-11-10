package net.ty.createcraftedbeginning.content.opticalpower.amethystlens;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystLensSerialization {
    private static final String COMPOUND_KEY_FOCAL_LENGTH = "FocalLength";

    private AmethystLensSerialization() {
    }

    static int readFocalLength(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains(COMPOUND_KEY_FOCAL_LENGTH)) {
            return AmethystLensFocusBehaviour.DEFAULT_FOCAL_LENGTH;
        }

        return Mth.clamp(tag.getInt(COMPOUND_KEY_FOCAL_LENGTH), AmethystLensFocusBehaviour.MIN_FOCAL_LENGTH, AmethystLensFocusBehaviour.MAX_FOCAL_LENGTH);
    }

    static void writeFocalLength(CompoundTag tag, int focalLength) {
        tag.putInt(COMPOUND_KEY_FOCAL_LENGTH, focalLength);
    }
}
