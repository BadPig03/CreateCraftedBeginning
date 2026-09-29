package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBGas extends ConfigBase {
    public final PressureRupture pressureRupture = nested(0, PressureRupture::new, "Pressure Rupture");
    public final Atmosphere atmosphere = nested(0, Atmosphere::new, "Atmosphere");

    @Override
    public String getName() {
        return "gas";
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class PressureRupture extends ConfigBase {
        public final ConfigFloat explosionStrength = f(4, 0, 64, "explosion_strength", "[Unit: explosion strength]", "Strength of the explosion caused by pressure rupture. Set to 0 to disable the blast; gas release and destruction of the failed pressure-bearing block still occur.");
        public final ConfigBool explosionDamagesSurroundingBlocks = b(false, "explosion_damages_surrounding_blocks", "Allow pressure rupture explosions to destroy surrounding blocks. The failed pressure-bearing block is destroyed regardless of this setting.");

        @Override
        public String getName() {
            return "pressure_rupture";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class Atmosphere extends ConfigBase {
        public final ConfigBool allowEnvironmentAirExtraction = b(true, "allow_environment_air_extraction", "Allow open-ended gas pipes to extract air from the environment. This does not disable gas exhaust into the environment.");
        public final ConfigFloat overworldPressure = f(1, 0, GasPressureLimits.HARD_PRESSURE_ATM, "overworld_pressure", "[Unit: atm; 1 atm = 100000 Pa]", "Default Overworld pressure supplied by the built-in atmosphere provider. Also used as the fallback for dimensions without a dedicated provider override.");
        public final ConfigFloat netherPressure = f(1.6F, 0, GasPressureLimits.HARD_PRESSURE_ATM, "nether_pressure", "[Unit: atm; 1 atm = 100000 Pa]", "Default Nether pressure supplied by the built-in atmosphere provider. A dedicated provider can override this value.");
        public final ConfigFloat endPressure = f(0.4F, 0, GasPressureLimits.HARD_PRESSURE_ATM, "end_pressure", "[Unit: atm; 1 atm = 100000 Pa]", "Default End pressure supplied by the built-in atmosphere provider. A dedicated provider can override this value.");

        @Override
        public String getName() {
            return "atmosphere";
        }
    }
}
