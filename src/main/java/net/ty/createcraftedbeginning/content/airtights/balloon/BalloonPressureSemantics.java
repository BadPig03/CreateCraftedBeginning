package net.ty.createcraftedbeginning.content.airtights.balloon;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.gas.atmosphere.AtmosphereStateResolver;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BalloonPressureSemantics {
    private BalloonPressureSemantics() {
    }

    public static long ambientPressurePa(Level level, BlockPos pos) {
        return AtmosphereStateResolver.resolvePressurePa(level, pos);
    }

    public static GameplayPressureProfile gameplayProfile(Level level, BlockPos pos) {
        return GameplayPressureProfiles.resolve(ambientPressurePa(level, pos));
    }
}
