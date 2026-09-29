package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour.Base;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.client.render.CCBSpriteShifts;
import net.ty.createcraftedbeginning.gas.multiblock.GasTankMultiblockConnectivity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightTankCTBehaviour extends Base {
    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter level, BlockPos pos, BlockPos otherPos, Direction face) {
        return state.getBlock() == other.getBlock() && GasTankMultiblockConnectivity.isConnected(level, pos, otherPos);
    }

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        if (direction.getAxis() != Axis.Y) {
            return CCBSpriteShifts.AIRTIGHT_TANK;
        }

        return CCBSpriteShifts.AIRTIGHT_TANK_TOP;
    }
}
