package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class GasCollisionEvent extends Event {
    protected final Gas firstGas;
    protected final Gas secondGas;
    private final Level level;
    private final BlockPos pos;
    @Nullable
    private BlockState state;

    protected GasCollisionEvent(Level level, BlockPos pos, Gas firstGas, Gas secondGas, @Nullable BlockState state) {
        this.level = level;
        this.pos = pos;
        this.firstGas = firstGas;
        this.secondGas = secondGas;
        this.state = state;
    }

    public Level getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    @Nullable
    public BlockState getState() {
        return state;
    }

    public void setState(@Nullable BlockState state) {
        this.state = state;
    }

    public static class Flow extends GasCollisionEvent {
        public Flow(Level level, BlockPos pos, Gas firstGas, Gas secondGas, @Nullable BlockState defaultState) {
            super(level, pos, firstGas, secondGas, defaultState);
        }

        public Gas getFirstGas() {
            return firstGas;
        }

        public Gas getSecondGas() {
            return secondGas;
        }
    }
}
