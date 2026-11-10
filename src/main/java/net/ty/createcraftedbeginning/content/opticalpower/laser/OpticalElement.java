package net.ty.createcraftedbeginning.content.opticalpower.laser;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface OpticalElement {
    @Nullable OpticalGeometry getOpticalGeometry(BlockGetter level, BlockPos pos, BlockState state, float partialTick);

    OpticalGeometry getMountedOpticalGeometry(BlockState state, @Nullable CompoundTag tag);
}
