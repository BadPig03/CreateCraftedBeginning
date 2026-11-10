package net.ty.createcraftedbeginning.content.decoration.industrialiron;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.RotatedPillarCTBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class IndustrialIronPillarCTBehaviour extends RotatedPillarCTBehaviour {
    public IndustrialIronPillarCTBehaviour(CTSpriteShiftEntry shift) {
        super(shift, null);
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter level, BlockPos pos, BlockPos otherPos, Direction face, @Nullable Direction primaryOffset, @Nullable Direction secondaryOffset) {
        Axis axis = state.getValue(RotatedPillarBlock.AXIS);
        return other.is(state.getBlock()) && other.getValue(RotatedPillarBlock.AXIS) == axis && primaryOffset == null && secondaryOffset != null && secondaryOffset.getAxis() == axis;
    }
}
