package net.ty.createcraftedbeginning.content.airtights.airvents;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTType;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour.Base;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock.VentState;
import net.ty.createcraftedbeginning.data.CCBSpriteShifts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AirVentCTBehaviour extends Base {
    @Override
    public boolean connectsTo(@NotNull BlockState state, BlockState other, BlockAndTintGetter level, BlockPos pos, BlockPos otherPos, Direction face, Direction primaryOffset, Direction secondaryOffset) {
        VentState ventState = state.getValue(AirVentBlock.PROPERTY_BY_DIRECTION.get(face));
        return !ventState.canHandInteract() && connectsTo(state, other, level, pos, otherPos, face);
    }

    @Override
    public @Nullable CTSpriteShiftEntry getShift(@NotNull BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        return CCBSpriteShifts.AIR_VENT;
    }

    @Override
    public CTType getDataType(BlockAndTintGetter level, BlockPos pos, @NotNull BlockState state, Direction direction) {
        return AllCTTypes.OMNIDIRECTIONAL;
    }
}
