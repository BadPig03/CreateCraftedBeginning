package net.ty.createcraftedbeginning.api.gas.gases.collisions;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCollisionEvent extends Event implements ICancellableEvent {
    private final GasStack firstGasStack;
    private final GasStack secondGasStack;
    private final Level level;
    private final BlockPos pos;

    @Nullable
    private BlockState state;

    public GasCollisionEvent(Level level, BlockPos pos, GasStack firstGasStack, GasStack secondGasStack, @Nullable BlockState state) {
        this.level = level;
        this.pos = pos.immutable();
        this.firstGasStack = firstGasStack.copy();
        this.secondGasStack = secondGasStack.copy();
        this.state = state;
    }

    public static void handleCollision(Level level, BlockPos pos, GasStack firstGasStack, GasStack secondGasStack) {
        GasCollisionEvent event = new GasCollisionEvent(level, pos, firstGasStack, secondGasStack, null);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return;
        }

        level.destroyBlock(pos, true);
        if (event.state == null) {
            return;
        }

        level.setBlockAndUpdate(pos, event.state);
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

    @SuppressWarnings("unused")
    public GasStack getFirstGasStack() {
        return firstGasStack.copy();
    }

    @SuppressWarnings("unused")
    public GasStack getSecondGasStack() {
        return secondGasStack.copy();
    }
}
