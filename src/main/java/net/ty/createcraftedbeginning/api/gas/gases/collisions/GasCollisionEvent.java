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

    public GasCollisionEvent(Level level, BlockPos pos, Gas firstGasType, Gas secondGasType, @Nullable BlockState state) {
        this.level = level;
        this.pos = pos;
        this.firstGasType = firstGasType;
        this.secondGasType = secondGasType;
        this.state = state;
    }

    public static void handleCollision(Level level, BlockPos pos, GasStack firstGasStack, GasStack secondGasStack) {
        level.destroyBlock(pos, true);
        GasCollisionEvent event = new GasCollisionEvent(level, pos, firstGasStack.getGasType(), secondGasStack.getGasType(), null);
        NeoForge.EVENT_BUS.post(event);
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

    public Gas getFirstGasType() {
        return firstGasType;
    }

    public Gas getSecondGasType() {
        return secondGasType;
    }
}
