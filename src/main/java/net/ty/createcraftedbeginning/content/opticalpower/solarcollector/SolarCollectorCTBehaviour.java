package net.ty.createcraftedbeginning.content.opticalpower.solarcollector;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTType;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour.Base;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.foundation.texture.CCBSpriteShifts;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SolarCollectorCTBehaviour extends Base {
    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter level, BlockPos pos, BlockPos otherPos, Direction face) {
        if (face != Direction.UP || !(state.getBlock() instanceof SolarCollectorBlock) || !(other.getBlock() instanceof SolarCollectorBlock)) {
            return false;
        }

        SolarCollectorGeometry geometry = SolarCollectorGeometry.findGeometry(level, pos);
        return geometry.isActive(pos) && geometry.isActive(otherPos);
    }

    @Override
    public @Nullable CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        if (direction != Direction.UP) {
            return null;
        }
        return CCBSpriteShifts.SOLAR_COLLECTOR_PANEL;
    }

    @Override
    public @Nullable CTType getDataType(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction direction) {
        if (direction != Direction.UP || !SolarCollectorGeometry.findGeometry(level, pos).isActive(pos)) {
            return null;
        }
        return CCBSpriteShifts.SOLAR_COLLECTOR_PANEL.getType();
    }
}
