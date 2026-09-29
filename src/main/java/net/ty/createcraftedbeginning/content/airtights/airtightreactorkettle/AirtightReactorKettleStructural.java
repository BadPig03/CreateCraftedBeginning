package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightReactorKettleStructural {
    static BlockPos getMaster(BlockPos pos, BlockState state) {
        return switch (state.getBlock()) {
            case AirtightReactorKettleStructuralBlock ignored -> pos.offset(state.getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION).getPosition());
            case AirtightReactorKettleStructuralCogBlock ignored -> pos.offset(state.getValue(AirtightReactorKettleStructuralCogBlock.STRUCTURAL_POSITION).getPosition());
            default -> pos;
        };
    }

    boolean stillValid(BlockGetter level, BlockPos pos, BlockState state);

    EnumProperty<AirtightReactorKettleStructuralPosition> getStructuralPosition();
}
