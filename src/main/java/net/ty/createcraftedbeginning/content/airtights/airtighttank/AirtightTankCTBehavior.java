package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasConnectivityHandler;
import net.ty.createcraftedbeginning.data.CCBSpriteShifts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AirtightTankCTBehavior extends HorizontalCTBehaviour {
    public AirtightTankCTBehavior() {
        super(CCBSpriteShifts.AIRTIGHT_TANK, CCBSpriteShifts.AIRTIGHT_TANK_TOP);
    }

    @Override
    public boolean buildContextForOccludedDirections() {
        return true;
    }

    @Override
    public boolean connectsTo(@NotNull BlockState state, @NotNull BlockState other, BlockAndTintGetter level, BlockPos pos, BlockPos otherPos, Direction face) {
        return state.getBlock() == other.getBlock() && GasConnectivityHandler.isConnected(level, pos, otherPos);
    }

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, @NotNull Direction direction, @Nullable TextureAtlasSprite sprite) {
        return direction.getAxis() == Axis.Y ? CCBSpriteShifts.AIRTIGHT_TANK_TOP : CCBSpriteShifts.AIRTIGHT_TANK;
    }
}
