package net.ty.createcraftedbeginning.content.opticalpower.laser;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.UUID;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record OpticalTarget(ResourceKey<Level> dimension, BlockPos blockPos, @Nullable UUID contraptionId, boolean inSubLevel) {
    public OpticalTarget {
        Objects.requireNonNull(dimension, "Optical target dimension must not be null.");
        blockPos = blockPos.immutable();
        if (contraptionId != null && inSubLevel) {
            throw new IllegalArgumentException("Optical target cannot use both contraption and sub-level coordinates; got " + contraptionId + " at " + blockPos + '.');
        }
    }
}
