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
    /**
     * Checks whether the requested operation can transport.
     *
     * @param level      the level in which the operation is performed
     * @param blockState the block state to inspect or process
     * @param blockPos   the target block position
     * @param direction  the direction associated with the operation
     * @return {@code true} if the requested operation can transport; otherwise {@code false}
     */
    boolean canTransport(Level level, BlockState blockState, BlockPos blockPos, Direction direction);

    /**
     * Returns the advancement behaviour.
     *
     * @return the advancement behaviour
     */
    CCBAdvancementBehaviour getAdvancementBehaviour();
}
