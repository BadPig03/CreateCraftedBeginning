package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour.Base;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.GasConnectivityHandler;
import net.ty.createcraftedbeginning.data.CCBSpriteShifts;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HorizontalAirtightTankCTBehavior extends Base {
    @Override
    @Nullable
    public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        if (!state.hasProperty(HorizontalAirtightTankBlock.HORIZONTAL_AXIS)) {
            return null;
        }

        Axis axis = state.getValue(HorizontalAirtightTankBlock.HORIZONTAL_AXIS);
        if (direction.getAxis() == Axis.Y) {
            return CCBSpriteShifts.AIRTIGHT_TANK;
        }
        else if (direction.getAxis() == axis) {
            return CCBSpriteShifts.AIRTIGHT_TANK_TOP;
        }
        return CCBSpriteShifts.HORIZONTAL_AIRTIGHT_TANK;
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter level, BlockPos pos, BlockPos otherPos, Direction face) {
        return state.getBlock() == other.getBlock() && GasConnectivityHandler.isConnected(level, pos, otherPos);
    }

    @Override
    protected Direction getUpDirection(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face) {
        if (!state.hasProperty(HorizontalAirtightTankBlock.HORIZONTAL_AXIS)) {
            return Direction.UP;
        }

        Axis axis = state.getValue(HorizontalAirtightTankBlock.HORIZONTAL_AXIS);
        Axis facingAxis = face.getAxis();
        boolean alongX = axis == Axis.X;
        if (facingAxis.isVertical() && alongX) {
            return super.getUpDirection(level, pos, state, face).getClockWise();
        }
        else if (facingAxis == axis || facingAxis.isVertical()) {
            return super.getUpDirection(level, pos, state, face);
        }
        return Direction.fromAxisAndDirection(axis, alongX ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE);
    }

    @Override
    protected Direction getRightDirection(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face) {
        if (!state.hasProperty(HorizontalAirtightTankBlock.HORIZONTAL_AXIS)) {
            return Direction.UP;
        }

        Axis axis = state.getValue(HorizontalAirtightTankBlock.HORIZONTAL_AXIS);
        Axis facingAxis = face.getAxis();
        if (facingAxis.isVertical() && axis == Axis.X) {
            return super.getRightDirection(level, pos, state, face).getClockWise();
        }
        else if (facingAxis == axis || facingAxis.isVertical()) {
            return super.getRightDirection(level, pos, state, face);
        }
        return Direction.fromAxisAndDirection(Axis.Y, face.getAxisDirection());
    }
}
