package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTType;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour.Base;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.ty.createcraftedbeginning.client.render.CCBSpriteShifts;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightCasingCTHelper;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightMeterCTBehaviour extends Base {
    @Override
    public @Nullable CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        CTSpriteShiftEntry casingShift = CCBSpriteShifts.AIRTIGHT_METER_CASING;
        if (sprite != null && sprite != casingShift.getOriginal() || direction.getAxis() == state.getValue(BlockStateProperties.AXIS) || AbstractAirtightMeterBlock.isDisplayFace(state, direction)) {
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

    @Override
    protected Direction getUpDirection(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face) {
        Axis axis = state.getValue(BlockStateProperties.AXIS);
        switch (axis) {
            case Y -> {
                return super.getUpDirection(level, pos, state, face);
            }
            case X -> {
                if (face == Direction.DOWN) {
                    return Direction.WEST;
                }

                return Direction.EAST;
            }
            default -> {
                if (face == Direction.DOWN) {
                    return Direction.SOUTH;
                }

                return Direction.NORTH;
            }
        }
    }

    @Override
    protected Direction getRightDirection(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face) {
        Axis axis = state.getValue(BlockStateProperties.AXIS);
        return switch (axis) {
            case Y -> super.getRightDirection(level, pos, state, face);
            case X -> switch (face) {
                case UP -> Direction.NORTH;
                case DOWN -> Direction.SOUTH;
                default -> Direction.UP;
            };
            default -> switch (face) {
                case UP -> Direction.WEST;
                case DOWN -> Direction.EAST;
                default -> Direction.UP;
            };
        };
    }
}
