package net.ty.createcraftedbeginning.content.airtights.handlers.thermoregulator;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandlers;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ThermoregulatorSampling {
    private ThermoregulatorSampling() {
    }

    public static float calculateTemperature(Level level, BlockPos origin, BlockPos minimum, int width) {
        Set<BlockPos> sources = new HashSet<>();
        float total = AirtightThermoregulatorHandler.NONE;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
                BlockPos sample = minimum.offset(x, 0, z);
                if (!level.isLoaded(sample)) {
                    return AirtightThermoregulatorHandler.NONE;
                }

                TemperatureSource source = SubLevelBridge.findAt(level, origin, sample, pos -> {
                    BlockState state = level.getBlockState(pos);
                    AirtightThermoregulatorHandler handler = AirtightThermoregulatorHandlers.resolve(state.getBlock());
                    float temperature = handler.getHeat(level, pos, state);
                    if (!Float.isFinite(temperature) || temperature == AirtightThermoregulatorHandler.NONE) {
                        return null;
                    }

                    return new TemperatureSource(pos.immutable(), temperature);
                });
                if (source == null || !sources.add(source.pos())) {
                    continue;
                }

                total += source.temperature();
            }
        }
        if (!Float.isFinite(total)) {
            return AirtightThermoregulatorHandler.NONE;
        }

        return total;
    }

    private record TemperatureSource(BlockPos pos, float temperature) {}
}
