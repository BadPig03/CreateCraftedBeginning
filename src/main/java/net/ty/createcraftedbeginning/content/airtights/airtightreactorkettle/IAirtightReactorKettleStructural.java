package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.NotNull;

public interface IAirtightReactorKettleStructural {
    boolean stillValid(BlockGetter level, BlockPos pos, @NotNull BlockState state);

    EnumProperty<AirtightReactorKettleStructuralPosition> getStructuralPosition();
}
