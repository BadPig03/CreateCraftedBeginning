package net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower;

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
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightFractionationTowerCTBehaviour extends Base {
    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter level, BlockPos pos, BlockPos otherPos, Direction face) {
        if (state.getBlock() != other.getBlock() || !(level.getBlockEntity(pos) instanceof AirtightFractionationTowerBlockEntity tower) || !(level.getBlockEntity(otherPos) instanceof AirtightFractionationTowerBlockEntity otherTower)) {
            return false;
        }

        BlockPos origin = tower.getOrigin();
        return origin != null && origin.equals(otherTower.getOrigin());
    }

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        if (direction.getAxis() == Axis.Y) {
            return CCBSpriteShifts.AIRTIGHT_FRACTIONATION_TOWER_TOP;
        }

        return CCBSpriteShifts.AIRTIGHT_FRACTIONATION_TOWER;
    }
}
