package net.ty.createcraftedbeginning.api.gas.gases.collisions;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class GasCollisionEvent extends Event {
    private final Gas firstGasType;
    private final Gas secondGasType;
    private final Level level;
    private final BlockPos pos;

    @Nullable
    private BlockState state;

    /**
     * Creates a new {@code GasCollisionEvent} instance.
     *
     * @param level         the level in which the operation is performed
     * @param pos           the target block position
     * @param firstGasType  the first gas type to use
     * @param secondGasType the second gas type to use
     * @param state         the block state to inspect or process
     */
    public GasCollisionEvent(Level level, BlockPos pos, Gas firstGasType, Gas secondGasType, @Nullable BlockState state) {
        this.level = level;
        this.pos = pos;
        this.firstGasType = firstGasType;
        this.secondGasType = secondGasType;
        this.state = state;
    }

    /**
     * Handles a collision involving the supplied gas stack.
     *
     * @param level          the level in which the operation is performed
     * @param pos            the target block position
     * @param firstGasStack  the first gas stack to inspect or process
     * @param secondGasStack the second gas stack to inspect or process
     */
    public static void handleCollision(Level level, BlockPos pos, GasStack firstGasStack, GasStack secondGasStack) {
        level.destroyBlock(pos, true);
        GasCollisionEvent event = new GasCollisionEvent(level, pos, firstGasStack.getGasType(), secondGasStack.getGasType(), null);
        NeoForge.EVENT_BUS.post(event);
        if (event.state == null) {
            return;
        }

        level.setBlockAndUpdate(pos, event.state);
    }

    /**
     * Returns the level.
     *
     * @return the level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Returns the pos.
     *
     * @return the pos
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Returns the state.
     *
     * @return the state
     */
    @Nullable
    public BlockState getState() {
        return state;
    }

    /**
     * Sets the state.
     *
     * @param state the block state to inspect or process
     */
    public void setState(@Nullable BlockState state) {
        this.state = state;
    }

    /**
     * Returns the first available gas type.
     *
     * @return the first available gas type
     */
    public Gas getFirstGasType() {
        return firstGasType;
    }

    /**
     * Returns the second gas type.
     *
     * @return the second gas type
     */
    public Gas getSecondGasType() {
        return secondGasType;
    }
}
