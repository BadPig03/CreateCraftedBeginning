package net.ty.createcraftedbeginning.gas.atmosphere;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.atmosphere.AtmosphereProviderRegistry;
import net.ty.createcraftedbeginning.api.atmosphere.AtmosphereState;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AtmosphereStateResolver {
    private AtmosphereStateResolver() {
    }

    public static AtmosphereState resolve(Level level, BlockPos pos) {
        BlockPos worldPos = SubLevelBridge.resolve(level, pos).blockPos();
        BlockPos sourcePos = SubLevelBridge.findAt(level, pos, pos, sample -> {
            if (level.getBlockState(sample).isAir()) {
                return null;
            }

            return sample;
        });
        if (sourcePos == null) {
            sourcePos = worldPos;
        }

        Gas gas = AtmosphereProviderRegistry.resolveComposition(level, sourcePos);
        if (gas == null) {
            gas = Gas.EMPTY_GAS_HOLDER.value();
        }

        return new AtmosphereState(gas, resolveWorldPressurePa(level, worldPos));
    }

    public static long resolvePressurePa(Level level, BlockPos pos) {
        BlockPos worldPos = SubLevelBridge.resolve(level, pos).blockPos();
        return resolveWorldPressurePa(level, worldPos);
    }

    private static long resolveWorldPressurePa(Level level, BlockPos worldPos) {
        return GasPressureLimits.clampToHardLimit(AtmosphereProviderRegistry.resolvePressurePa(level, worldPos).orElse(GasPressure.VACUUM_PA));
    }
}
