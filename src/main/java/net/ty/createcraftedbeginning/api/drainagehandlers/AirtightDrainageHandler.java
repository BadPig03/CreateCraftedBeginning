package net.ty.createcraftedbeginning.api.drainagehandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightDrainageHandler {
    SimpleRegistry<Gas, AirtightDrainageHandler> REGISTRY = SimpleRegistry.create();

    float getInflation();

    default boolean shouldShowOutline() {
        return true;
    }

    void apply(Level level, BlockPos pos, Direction direction, Gas gasType);

    default void showOutline(Level level, BlockPos pos, Direction direction, float inflation, int color) {
        AirtightDrainageHandlerUtils.showOutline(level, pos, direction, inflation, color);
    }
}
