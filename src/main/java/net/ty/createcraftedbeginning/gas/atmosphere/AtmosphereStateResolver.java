package net.ty.createcraftedbeginning.gas.atmosphere;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.atmosphere.AtmosphereProviderRegistry;
import net.ty.createcraftedbeginning.api.atmosphere.AtmosphereState;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AtmosphereStateResolver {
    private AtmosphereStateResolver() {
    }

    public static AtmosphereState resolve(Level level, BlockPos pos) {
        Gas gas = AtmosphereProviderRegistry.resolveComposition(level, pos);
        if (gas == null) {
            gas = Gas.EMPTY_GAS_HOLDER.value();
        }
        return new AtmosphereState(gas, resolvePressurePa(level, pos));
    }

    public static long resolvePressurePa(Level level, BlockPos pos) {
        return GasPressureLimits.clampToHardLimit(AtmosphereProviderRegistry.resolvePressurePa(level, pos).orElse(GasPressure.VACUUM_PA));
    }
}
