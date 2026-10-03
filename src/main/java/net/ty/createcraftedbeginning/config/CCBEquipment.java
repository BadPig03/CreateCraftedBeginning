package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBEquipment extends ConfigBase {
    public final GasCanister gasCanister = nested(0, GasCanister::new, "Gas Canister");
    public final AirtightCannon airtightCannon = nested(0, AirtightCannon::new, "Airtight Cannon");
    public final AirtightExtendArm airtightExtendArm = nested(0, AirtightExtendArm::new, "Airtight Extend Arm");
    public final AirtightHandheldDrill airtightHandheldDrill = nested(0, AirtightHandheldDrill::new, "Airtight Handheld Drill");
    public final AirtightHelmet airtightHelmet = nested(0, AirtightHelmet::new, "Airtight Helmet");
    public final AirtightChestplate airtightChestplate = nested(0, AirtightChestplate::new, "Airtight Chestplate");
    public final AirtightLeggings airtightLeggings = nested(0, AirtightLeggings::new, "Airtight Leggings");
    public final AirtightBoots airtightBoots = nested(0, AirtightBoots::new, "Airtight Boots");

    @Override
    public String getName() {
        return "equipment";
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class GasCanister extends ConfigBase {
        public final ConfigInt gasVolume = i(2, 1, "gas_volume", "[Unit: kL]", "Base physical volume of a Gas Canister before Capacity enchantment scaling. Gas amount at a given pressure scales with this volume.");
        public final ConfigFloat maxPressure = f(10, 0, GasPressureLimits.HARD_PRESSURE_ATM, "max_pressure", "[Unit: atm; 1 atm = 100000 Pa]", "Maximum absolute pressure a Gas Canister can accept. This value cannot exceed the global 24 atm hard limit.");

        @Override
        public String getName() {
            return "gas_canister";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AirtightCannon extends ConfigBase {
        public final ConfigInt gasPerShot = i(100, 0, "gas_per_shot", "[Unit: GU per shot]", "Base gas consumption per shot with the Airtight Cannon. Actual consumption also depends on the selected gas supply and applicable upgrades. A value of 0 removes this base cost without bypassing other operating requirements.");

        @Override
        public String getName() {
            return "airtight_cannon";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AirtightExtendArm extends ConfigBase {
        public final ConfigInt gasPerPoweredAction = i(10, 0, "gas_per_powered_action", "[Unit: GU per powered action]", "Base gas consumption per powered action with the Airtight Extend Arm. Actual consumption also depends on the selected gas supply and applicable upgrades. A value of 0 removes this base cost without bypassing other operating requirements.");

        @Override
        public String getName() {
            return "airtight_extend_arm";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AirtightHandheldDrill extends ConfigBase {
        public final ConfigInt gasPerBlock = i(10, 0, "gas_per_block", "[Unit: GU per block mined]", "Base gas consumption per block mined with the Airtight Handheld Drill. Actual consumption also depends on the selected gas supply and applicable upgrades. A value of 0 removes this base cost without bypassing other operating requirements.");
        public final ConfigInt chainMiningMaxBlocks = i(64, 1, 512, "chain_mining_max_blocks", "[Unit: blocks]", "Maximum number of face-connected blocks of the same type selected by the Chain Mining template.");
        public final ConfigInt gasPerEntityHit = i(5, 0, "gas_per_entity_hit", "[Unit: GU per entity hit]", "Base gas consumption per entity hit with the Airtight Handheld Drill. Actual consumption also depends on the selected gas supply and applicable upgrades. A value of 0 removes this base cost without bypassing other operating requirements.");
        public final ConfigFloat harvestOptimizationGasMultiplier = f(1, 1, "harvest_optimization_gas_multiplier", "[Unit: multiplier; 1 = unchanged gas cost]", "Multiplies the applicable mining gas cost when Harvest Optimization is in Silk Touch mode. Not applied in Fortune mode or when Experience Conversion is active; applicable Magnet costs still multiply the result.");
        public final ConfigFloat liquidReplacementGasMultiplier = f(1, 1, "liquid_replacement_gas_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Multiplies the base per-block gas cost for replacing liquid. This cost replaces the normal mining cost for liquid blocks and is added to it for blocks containing fluid.");
        public final ConfigFloat magnetGasMultiplier = f(1, 1, "magnet_gas_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Scales the standard Magnet gas cost, which multiplies applicable mining costs by 1.5. Multiplies applicable mining costs after liquid replacement and Silk Touch adjustments.");
        public final ConfigFloat experienceConversionGasMultiplier = f(1, 1, "experience_conversion_gas_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Scales the standard Experience Conversion gas cost, which multiplies applicable mining costs by 3. Applied after applicable Magnet costs; Silk Touch does not add its multiplier in this mode.");

        @Override
        public String getName() {
            return "airtight_handheld_drill";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AirtightHelmet extends ConfigBase {
        public final ConfigFloat effectsProtectionGasMultiplier = f(1, 0, "effects_protection_gas_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Scales the standard effect protection cost of 0.5 GU per effect level per duration tick. Actual consumption also depends on the selected gas supply. A value of 0 removes the cost but still requires a suitable gas supply.");
        public final ConfigInt waterBreathingGasPerSecond = i(20, 0, "water_breathing_gas_per_second", "[Unit: GU/s]", "Base gas consumption per second while Water Breathing is active and the wearer is underwater. Actual consumption also depends on the selected gas supply. A value of 0 removes the cost but still requires a suitable gas supply.");
        public final ConfigInt visionGasPerSecond = i(10, 0, "vision_gas_per_second", "[Unit: GU/s]", "Base gas consumption per second while the Vision Upgrade is active. Actual consumption also depends on the selected gas supply. A value of 0 removes the cost but still requires a suitable gas supply.");
        public final ConfigFloat resistanceGasMultiplier = f(1, 0, "resistance_gas_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Scales the standard cost of 5 GU per incoming damage point for the Airtight Helmet Resistance Upgrade. Does not apply to damage that bypasses Resistance. A value of 0 removes the cost but still requires a suitable gas supply.");

        @Override
        public String getName() {
            return "airtight_helmet";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AirtightChestplate extends ConfigBase {
        public final ConfigInt gasPerElytraBoost = i(25, 0, "gas_per_elytra_boost", "[Unit: GU per Elytra boost]", "Base gas consumption per Elytra boost with the Airtight Chestplate. Actual consumption also depends on the selected gas supply and applicable upgrades. A value of 0 removes this base cost without bypassing other operating requirements.");
        public final ConfigInt creativeFlightGasPerSecond = i(50, 0, "creative_flight_gas_per_second", "[Unit: GU/s]", "Base gas consumption per second while Creative Flight is active and the wearer is flying. Actual consumption also depends on the selected gas supply. A value of 0 removes the cost but still requires a suitable gas supply.");
        public final ConfigInt regenerationGasPerSecond = i(30, 0, "regeneration_gas_per_second", "[Unit: GU/s]", "Base gas consumption per second while Regeneration is active and the wearer is recovering health. Actual consumption also depends on the selected gas supply. A value of 0 removes the cost but still requires a suitable gas supply.");
        public final ConfigInt invisibilityGasPerSecond = i(10, 0, "invisibility_gas_per_second", "[Unit: GU/s]", "Base gas consumption per second while the Invisibility Upgrade is active. Actual consumption also depends on the selected gas supply. A value of 0 removes the cost but still requires a suitable gas supply.");
        public final ConfigFloat resistanceGasMultiplier = f(1, 0, "resistance_gas_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Scales the standard cost of 5 GU per incoming damage point for the Airtight Chestplate Resistance Upgrade. Does not apply to damage that bypasses Resistance. A value of 0 removes the cost but still requires a suitable gas supply.");

        @Override
        public String getName() {
            return "airtight_chestplate";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AirtightLeggings extends ConfigBase {
        public final ConfigFloat projectileDeflectionGasMultiplier = f(1, 0, "projectile_deflection_gas_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Scales the standard deflection cost of 25 GU per unit of projectile speed. Actual consumption also depends on the selected gas supply. A value of 0 removes the cost but still requires a suitable gas supply.");
        public final ConfigInt quickSwimmingGasPerSecond = i(10, 0, "quick_swimming_gas_per_second", "[Unit: GU/s]", "Base gas consumption per second while Quick Swimming is active and the wearer is underwater. Actual consumption also depends on the selected gas supply. A value of 0 removes the cost but still requires a suitable gas supply.");
        public final ConfigFloat resistanceGasMultiplier = f(1, 0, "resistance_gas_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Scales the standard cost of 5 GU per incoming damage point for the Airtight Leggings Resistance Upgrade. Does not apply to damage that bypasses Resistance. A value of 0 removes the cost but still requires a suitable gas supply.");

        @Override
        public String getName() {
            return "airtight_leggings";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AirtightBoots extends ConfigBase {
        public final ConfigFloat resistanceGasMultiplier = f(1, 0, "resistance_gas_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Scales the standard cost of 5 GU per incoming damage point for the Airtight Boots Resistance Upgrade. Does not apply to damage that bypasses Resistance. A value of 0 removes the cost but still requires a suitable gas supply.");

        @Override
        public String getName() {
            return "airtight_boots";
        }
    }
}
