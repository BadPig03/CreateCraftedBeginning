package net.ty.createcraftedbeginning.api.gas.gases.interfaces;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IGasTransporter {
    boolean canTransport(Level level, BlockState blockState, BlockPos blockPos, Direction direction);

    CCBAdvancementBehaviour getAdvancementBehaviour();
}
