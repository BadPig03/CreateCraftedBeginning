package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
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
public class CreativeAirtightTankCTBehaviour extends HorizontalCTBehaviour {
    public CreativeAirtightTankCTBehaviour() {
        super(CCBSpriteShifts.CREATIVE_AIRTIGHT_TANK, CCBSpriteShifts.CREATIVE_AIRTIGHT_TANK_TOP);
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
        return other.getBlock() instanceof CreativeAirtightTankBlock && GasTankMultiblockConnectivity.isConnected(reader, pos, otherPos);
    }

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        if (direction.getAxis() != Axis.Y) {
            return CCBSpriteShifts.CREATIVE_AIRTIGHT_TANK;
        }

        return CCBSpriteShifts.CREATIVE_AIRTIGHT_TANK_TOP;
    }
}
