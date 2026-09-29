package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.block.state.BlockStatePredicate;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.compat.kubejs.KubeJSHandlerAdapters;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class AirtightThermoregulatorHandlerEvent implements KubeEvent {
    public void add(Block block, ThermoregulatorHandler handler) {
        KubeJSHandlerAdapters.registerThermoregulator(block, handler);
    }

    public void addAdvanced(BlockStatePredicate predicate, ThermoregulatorHandler handler) {
        KubeJSHandlerAdapters.registerThermoregulator(predicate, handler);
    }

    @FunctionalInterface
    public interface ThermoregulatorHandler {
        float apply(Level level, BlockPos pos, BlockState state);
    }
}
