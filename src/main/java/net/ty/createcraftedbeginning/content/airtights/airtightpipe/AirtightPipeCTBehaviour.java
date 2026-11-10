package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTType;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour.Base;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.client.render.CCBSpriteShifts;
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

        CTSpriteShiftEntry casingShift = CCBSpriteShifts.AIRTIGHT_PIPE_CASING;
        if (sprite != null && sprite != casingShift.getOriginal() || AirtightPipeConnections.isOpen(state, direction)) {
            return null;
        }

        return casingShift;
    }

    @Override
    public @Nullable CTType getDataType(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction direction) {
        if (!AirtightCasingCTHelper.hasCasingNeighbourOnSurface(level, pos, state, direction)) {
            return null;
        }

        return super.getDataType(level, pos, state, direction);
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter level, BlockPos pos, BlockPos otherPos, Direction face) {
        return AirtightCasingCTHelper.connects(state, other, pos, otherPos, face);
    }

}
