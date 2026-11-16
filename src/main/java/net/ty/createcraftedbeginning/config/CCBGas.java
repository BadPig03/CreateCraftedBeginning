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
    public final EntityPush entityPush = nested(0, EntityPush::new, "Entity Push");
    public final ItemSuction itemSuction = nested(0, ItemSuction::new, "Item Suction");

    @Override
    public String getName() {
        return "gas";
    }

    public static final class PressureRupture extends ConfigBase {
        public final ConfigFloat explosionStrength = f(4, 0, 64, "explosion_strength", "[Unit: explosion strength]", "Strength of the explosion caused by pressure rupture. Set to 0 to disable the blast; gas release and destruction of the failed pressure-bearing block still occur.");
        public final ConfigBool explosionDamagesSurroundingBlocks = b(false, "explosion_damages_surrounding_blocks", "Allow pressure rupture explosions to destroy surrounding blocks. The failed pressure-bearing block is destroyed regardless of this setting.");

        @Override
        public String getName() {
            return "pressure_rupture";
        }
    }

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

    public static final class EntityPush extends ConfigBase {
        public final ConfigBool enableEntityPush = b(true, "enable_entity_push", "Allow released gas to push nearby entities. Disabling this does not change gas consumption, gas-specific effects or pressure rupture explosions.");
        public final ConfigFloat strengthMultiplier = f(1, 0, 4, "strength_multiplier", "[Unit: multiplier]", "Multiplier for gas-release acceleration and impulses, within their speed limits. Set to 0 to disable gas pushing without changing gas-specific effects.");

        @Override
        public String getName() {
            return "entity_push";
        }
    }

    public static final class ItemSuction extends ConfigBase {
        public final ConfigBool enableItemSuction = b(true, "enable_item_suction", "Allow actual atmospheric intake to attract dropped items to open pipe mouths without inserting them into pipes or inventories. Disabling this does not change gas transfer, gas-specific effects or exhaust pushing.");

        @Override
        public String getName() {
            return "item_suction";
        }
    }
}
