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
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasConnectivityHandler;
import net.ty.createcraftedbeginning.foundation.texture.CCBSpriteShifts;
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

        Axis tankAxis = state.getValue(HorizontalAirtightTankBlock.HORIZONTAL_AXIS);
        Axis directionAxis = direction.getAxis();
        if (directionAxis == Axis.Y) {
            return CCBSpriteShifts.AIRTIGHT_TANK;
        }

        if (directionAxis == tankAxis) {
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

        Axis tankAxis = state.getValue(HorizontalAirtightTankBlock.HORIZONTAL_AXIS);
        Axis faceAxis = face.getAxis();
        boolean isAlongX = tankAxis == Axis.X;
        if (faceAxis.isVertical() && isAlongX) {
            return super.getUpDirection(level, pos, state, face).getClockWise();
        }

        if (faceAxis == tankAxis || faceAxis.isVertical()) {
            return super.getUpDirection(level, pos, state, face);
        }
        return Direction.fromAxisAndDirection(tankAxis, isAlongX ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE);
    }

    @Override
    protected Direction getRightDirection(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face) {
        if (!state.hasProperty(HorizontalAirtightTankBlock.HORIZONTAL_AXIS)) {
            return Direction.UP;
        }

        Axis tankAxis = state.getValue(HorizontalAirtightTankBlock.HORIZONTAL_AXIS);
        Axis faceAxis = face.getAxis();
        if (faceAxis.isVertical() && tankAxis == Axis.X) {
            return super.getRightDirection(level, pos, state, face).getClockWise();
        }

        if (faceAxis == tankAxis || faceAxis.isVertical()) {
            return super.getRightDirection(level, pos, state, face);
        }
        return Direction.fromAxisAndDirection(Axis.Y, face.getAxisDirection());
    }
}
