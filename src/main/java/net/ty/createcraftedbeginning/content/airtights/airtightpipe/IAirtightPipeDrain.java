package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IAirtightPipeDrain {
    default boolean shouldRenderDrain(BlockAndTintGetter ignoredLevel, BlockPos ignoredPos, BlockState ignoredState, Direction ignoredFace) {
        return true;
    }
}
