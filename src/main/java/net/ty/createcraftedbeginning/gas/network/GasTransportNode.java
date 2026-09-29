package net.ty.createcraftedbeginning.gas.network;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasTransportNode {
    boolean allowsGasTransport(Level level, BlockState blockState, BlockPos blockPos, Direction direction);

    CCBAdvancementBehaviour getAdvancementBehaviour();
}
