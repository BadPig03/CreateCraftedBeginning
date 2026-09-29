package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightForgingPressStructural {
    static BlockPos getMaster(BlockPos pos, BlockState state) {
        return switch (state.getBlock()) {
            case AirtightForgingPressStructuralBlock ignored -> pos.offset(state.getValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION).getMasterOffset());
            case AirtightForgingPressStructuralShaftBlock ignored -> pos.offset(state.getValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION).getMasterOffset());
            default -> pos;
        };
    }

    boolean stillValid(BlockGetter level, BlockPos pos, BlockState state);

    EnumProperty<AirtightForgingPressStructuralPosition> getStructuralPosition();
}
