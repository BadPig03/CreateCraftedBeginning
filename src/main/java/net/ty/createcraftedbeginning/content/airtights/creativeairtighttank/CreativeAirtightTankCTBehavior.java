package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

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

public class CreativeAirtightTankCTBehavior extends HorizontalCTBehaviour {
    public CreativeAirtightTankCTBehavior() {
        super(CCBSpriteShifts.CREATIVE_AIRTIGHT_TANK, CCBSpriteShifts.CREATIVE_AIRTIGHT_TANK_TOP);
    }

    @Override
    public boolean buildContextForOccludedDirections() {
        return true;
    }

    @Override
    public boolean connectsTo(BlockState state, @NotNull BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
        return other.getBlock() instanceof CreativeAirtightTankBlock && GasConnectivityHandler.isConnected(reader, pos, otherPos);
    }

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, @NotNull Direction direction, @Nullable TextureAtlasSprite sprite) {
        return direction.getAxis() == Axis.Y ? CCBSpriteShifts.CREATIVE_AIRTIGHT_TANK_TOP : CCBSpriteShifts.CREATIVE_AIRTIGHT_TANK;
    }
}
