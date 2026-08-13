package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour.Base;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.foundation.texture.CCBSpriteShifts;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPipeCTBehaviour extends Base {
    @Override
    public @Nullable CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        if (!state.getValue(AirtightPipeBlock.CASED)) {
            return null;
        }

        if (direction.getAxis() == state.getValue(AirtightPipeBlock.AXIS)) {
            return null;
        }
        return CCBSpriteShifts.AIRTIGHT_PIPE_CASING;
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter level, BlockPos pos, BlockPos otherPos, Direction face) {
        if (state.getBlock() != other.getBlock() || !state.getValue(AirtightPipeBlock.CASED) || !other.getValue(AirtightPipeBlock.CASED)) {
            return false;
        }

        Axis pipeAxis = state.getValue(AirtightPipeBlock.AXIS);
        Axis otherPipeAxis = other.getValue(AirtightPipeBlock.AXIS);
        if (pipeAxis == face.getAxis() || otherPipeAxis == face.getAxis()) {
            return false;
        }

        int dx = otherPos.getX() - pos.getX();
        int dy = otherPos.getY() - pos.getY();
        int dz = otherPos.getZ() - pos.getZ();
        if (Mth.abs(dx) + Mth.abs(dy) + Mth.abs(dz) != 1) {
            return false;
        }

        Axis connectionAxis = dx != 0 ? Axis.X : dy != 0 ? Axis.Y : Axis.Z;
        return pipeAxis == connectionAxis && otherPipeAxis == connectionAxis;
    }
}
