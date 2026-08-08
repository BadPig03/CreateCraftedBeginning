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

    /**
     * Returns the inflation.
     *
     * @return the inflation
     */
    float getInflation();

    /**
     * Checks whether the caller should show outline.
     *
     * @return {@code true} if the caller should show outline; otherwise {@code false}
     */
    default boolean shouldShowOutline() {
        return true;
    }

    /**
     * Applies this operation to the supplied context.
     *
     * @param level     the level in which the operation is performed
     * @param pos       the target block position
     * @param direction the direction associated with the operation
     * @param gasType   the gas type to inspect or process
     */
    void apply(Level level, BlockPos pos, Direction direction, Gas gasType);

    /**
     * Displays the outline.
     *
     * @param level     the level in which the operation is performed
     * @param pos       the target block position
     * @param direction the direction associated with the operation
     * @param inflation the inflation value to use
     * @param color     the color value to use
     */
    default void showOutline(Level level, BlockPos pos, Direction direction, float inflation, int color) {
        AirtightDrainageHandlerUtils.showOutline(level, pos, direction, inflation, color);
    }
}
