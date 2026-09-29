package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBMachines extends ConfigBase {
    public final ResidueOutlet residueOutlet = nested(0, ResidueOutlet::new, "Residue Outlet");
    public final AirtightForgingPress airtightForgingPress = nested(0, AirtightForgingPress::new, "Airtight Forging Press");
    public final AirtightFractionationTower airtightFractionationTower = nested(0, AirtightFractionationTower::new, "Airtight Fractionation Tower");
    public final AirtightHatch airtightHatch = nested(0, AirtightHatch::new, "Airtight Hatch");
    public final AirtightPump airtightPump = nested(0, AirtightPump::new, "Airtight Pump");
    public final AirtightRegulatorPump airtightRegulatorPump = nested(0, AirtightRegulatorPump::new, "Airtight Regulator Pump");
    public final AirtightReactorKettle airtightReactorKettle = nested(0, AirtightReactorKettle::new, "Airtight Reactor Kettle");
    public final AirtightTank airtightTank = nested(0, AirtightTank::new, "Airtight Tank");
    public final BreezeChamber breezeChamber = nested(0, BreezeChamber::new, "Breeze Chamber");
    public final BreezeCooler breezeCooler = nested(0, BreezeCooler::new, "Breeze Cooler");
    public final GasInjectionChamber gasInjectionChamber = nested(0, GasInjectionChamber::new, "Gas Injection Chamber");
    public final GasPackager gasPackager = nested(0, GasPackager::new, "Gas Packager");
    public final TeslaTurbine teslaTurbine = nested(0, TeslaTurbine::new, "Tesla Turbine");
    public final AirtightAssemblyDriver airtightAssemblyDriver = nested(0, AirtightAssemblyDriver::new, "Airtight Assembly Driver");
    public final EndIncinerationBlower endIncinerationBlower = nested(0, EndIncinerationBlower::new, "End Incineration Blower");
    public final EndSculkSilencer endSculkSilencer = nested(0, EndSculkSilencer::new, "End Sculk Silencer");

    @Override
    public String getName() {
        return "machines";
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class ResidueOutlet extends ConfigBase {
        public final ConfigInt fluidCapacity = i(4, 1, "fluid_capacity", "[Unit: buckets]", "Fluid storage capacity of one Residue Outlet.");

        @Override
        public String getName() {
            return "residue_outlet";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AirtightForgingPress extends ConfigBase {
        public final ConfigBool enableAutomaticPressingRecipes = b(true, "enable_automatic_pressing_recipes", "Allow the Airtight Forging Press to process eligible Create pressing recipes in addition to its native recipes.");
        public final ConfigBool enableAutomaticCrushingRecipes = b(true, "enable_automatic_crushing_recipes", "Allow the Airtight Forging Press to process eligible Create crushing recipes with a Heavy Core installed as the press head and an empty addition slot. Items without a matching recipe are kept. Disabling this option leaves native forging recipes available.");
        public final ConfigBool enableAutomaticSmithingRecipes = b(true, "enable_automatic_smithing_recipes", "Allow the Airtight Forging Press to process eligible smithing recipes in addition to its native recipes.");
        public final ConfigBool requireSmithingTemplateForComponentCopy = b(true, "require_smithing_template_for_component_copy", "Require a smithing upgrade or armor trim template as the press head to copy input item components to the primary output. Disable to allow component copying with other press heads.");
        public final ConfigInt fluidCapacity = i(3, 1, "fluid_capacity", "[Unit: buckets]", "Fluid capacity of the Airtight Forging Press input tank.");
        public final ConfigInt gasVolume = i(30, 1, "gas_volume", "[Unit: kL]", "Physical gas volume of the Airtight Forging Press input tank. Gas amount at a given pressure scales with this volume.");

        @Override
        public String getName() {
            return "airtight_forging_press";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AirtightFractionationTower extends ConfigBase {
        private static final int MAX_ITEM_SLOTS_PER_LAYER = 64;
        private static final int MAX_TANKS_PER_LAYER = 9;
        private static final int MAX_FLUID_CAPACITY_PER_TANK = 2147483;

        public final ConfigInt itemSlotsPerLayer = i(9, 1, MAX_ITEM_SLOTS_PER_LAYER, "item_slots_per_layer", "[Unit: slots]", "Number of shared item slots in each Airtight Fractionation Tower layer. Applies to newly assembled towers.");
        public final ConfigInt fluidTanksPerLayer = i(1, 1, MAX_TANKS_PER_LAYER, "fluid_tanks_per_layer", "[Unit: tanks]", "Number of shared fluid tanks in each Airtight Fractionation Tower layer. Applies to newly assembled towers.");
        public final ConfigInt gasTanksPerLayer = i(1, 1, MAX_TANKS_PER_LAYER, "gas_tanks_per_layer", "[Unit: tanks]", "Number of shared gas tanks in each Airtight Fractionation Tower layer. Applies to newly assembled towers.");
        public final ConfigInt fluidCapacityPerTank = i(9, 1, MAX_FLUID_CAPACITY_PER_TANK, "fluid_capacity_per_tank", "[Unit: buckets]", "Fluid capacity of each Airtight Fractionation Tower fluid tank. Applies to newly assembled towers.");
        public final ConfigInt gasVolumePerTank = i(90, 1, "gas_volume_per_tank", "[Unit: kL]", "Physical gas volume of each Airtight Fractionation Tower gas tank. Gas amount at a given pressure scales with this volume. Applies to newly assembled towers.");

        @Override
        public String getName() {
            return "airtight_fractionation_tower";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AirtightHatch extends ConfigBase {
        public final ConfigInt maxTransferPerSecond = i(20000, 1, "max_transfer_per_second", "[Unit: GU/s]", "Maximum passive gas transfer through an Airtight Hatch per second. Transfer requires a favorable pressure difference and slows as the connected pressures approach equilibrium.");

        @Override
        public String getName() {
            return "airtight_hatch";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AirtightPump extends ConfigBase {
        public final ConfigFloat maxPressureBoost = f(4, 0, GasPressureLimits.HARD_PRESSURE_ATM, "max_pressure_boost", "[Unit: atm; 1 atm = 100000 Pa]", "Maximum pressure boost in the pumping direction at Create's configured maximum rotation speed. Boost scales with absolute speed; forward flow stops when the opposing pressure difference reaches the available boost. Set to 0 to disable pressure boost.");
        public final ConfigInt maxFlowPerTick = i(4000, 0, 1000000, "max_flow_per_tick", "[Unit: GU/t]", "Maximum free flow at Create's configured maximum rotation speed with an inlet pressure of at least 1 atm. Flow scales with absolute speed and decreases with opposing pressure difference. Inlet pressure below 1 atm further reduces throughput. Set to 0 to disable pump flow.");

        @Override
        public String getName() {
            return "airtight_pump";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AirtightRegulatorPump extends ConfigBase {
        public final ConfigFloat maxPressureRise = f(8, 0, GasPressureLimits.HARD_PRESSURE_ATM, "max_pressure_rise", "[Unit: atm; 1 atm = 100000 Pa]", "Maximum pressure rise above the inlet pressure in one pump stage, independent of rotation speed. Flow stops at the lower of the outlet set pressure and the inlet pressure plus this rise.");
        public final ConfigInt maxFlowPerTick = i(4000, 0, 1000000, "max_flow_per_tick", "[Unit: GU/t]", "Maximum free flow at Create's configured maximum rotation speed with an inlet pressure of at least 1 atm. Flow scales with absolute speed and decreases as outlet pressure approaches the regulation limit. Low inlet pressure further reduces throughput. Set to 0 to disable pump flow.");

        @Override
        public String getName() {
            return "airtight_regulator_pump";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AirtightReactorKettle extends ConfigBase {
        public final ConfigBool enableAutomaticMixingRecipes = b(true, "enable_automatic_mixing_recipes", "Allow the Airtight Reactor Kettle to process eligible Create mixing recipes and chilled mixing recipes with no additional processing duration. Automatic brewing is controlled separately. Disabling this option cancels uncommitted automatic mixing operations and leaves other reactor kettle recipe categories available under their existing rules.");
        public final ConfigBool enableAutomaticBrewingRecipes = b(true, "enable_automatic_brewing_recipes", "Allow the Airtight Reactor Kettle to process brewing recipes generated by Create with no additional processing duration. This option is independent of automatic mixing and Create's mixer brewing option. Disabling it cancels uncommitted automatic brewing operations and leaves other reactor kettle recipe categories available under their existing rules.");
        public final ConfigBool enableAutomaticShapelessRecipes = b(true, "enable_automatic_shapeless_recipes", "Allow the Airtight Reactor Kettle to process eligible shapeless crafting recipes in addition to its native recipes.");
        public final ConfigFloat mixerDamageMultiplier = f(1, 0, "mixer_damage_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Multiplier applied to operating mixer damage, whose base value is absolute rotation speed divided by 32. Set to 0 to disable mixer damage.");
        public final ConfigInt fluidCapacityPerTank = i(9, 1, "fluid_capacity_per_tank", "[Unit: buckets]", "Fluid capacity of each Airtight Reactor Kettle fluid tank segment.");
        public final ConfigInt gasVolumePerTank = i(90, 1, "gas_volume_per_tank", "[Unit: kL]", "Physical gas volume of each Airtight Reactor Kettle gas tank segment. Gas amount at a given pressure scales with this volume.");

        @Override
        public String getName() {
            return "airtight_reactor_kettle";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AirtightTank extends ConfigBase {
        public final ConfigInt gasVolumePerBlock = i(80, 1, "gas_volume_per_block", "[Unit: kL]", "Physical gas volume contributed by each block of an Airtight Tank multiblock. At 1 atm, each liter holds 1 GU; increasing volume increases the gas amount held at the same pressure.");
        public final ConfigInt maxHeight = i(4, 1, 32, "max_height", "[Unit: blocks]", "Maximum height of an Airtight Tank or Creative Airtight Tank multiblock. Existing tanks may need to be reassembled after changing this value.");
        public final ConfigInt maxWidth = i(3, 1, 16, "max_width", "[Unit: blocks]", "Maximum width and depth of an Airtight Tank or Creative Airtight Tank multiblock. Existing tanks may need to be reassembled after changing this value.");

        @Override
        public String getName() {
            return "airtight_tank";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class BreezeChamber extends ConfigBase {
        public final ConfigInt gasVolume = i(10, 1, "gas_volume", "[Unit: kL]", "Physical gas volume of the Breeze Chamber tank. Gas amount at a given pressure scales with this volume.");
        public final ConfigInt maxProcessingPerSecond = i(2500, 1, "max_processing_per_second", "[Unit: GU/s]", "Maximum gas processing rate of a Breeze Chamber. Actual processing also depends on the available wind charge and operating pressure.");
        public final ConfigInt maxWindChargeTicks = i(72000, 20, "max_wind_charge_ticks", "[Unit: ticks; 20 ticks = 1 second at 20 TPS]", "Maximum stored duration of positive or negative wind charge in a Breeze Chamber.");

        @Override
        public String getName() {
            return "breeze_chamber";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class BreezeCooler extends ConfigBase {
        public final ConfigBool allowSpawnerCapture = b(true, "allow_spawner_capture", "Allow an Empty Breeze Cooler to capture a Breeze from a spawner or trial spawner.");
        public final ConfigInt fluidCapacity = i(4, 1, "fluid_capacity", "[Unit: buckets]", "Fluid capacity of the Breeze Cooler input tank.");
        public final ConfigInt destructionTemperatureKelvin = i(1300, 1, "destruction_temperature_kelvin", "[Unit: K]", "Fluid temperature at or above which a non-creative Breeze Cooler is destroyed.");
        public final ConfigInt maxCoolingTicks = i(72000, 20, "max_cooling_ticks", "[Unit: ticks; 20 ticks = 1 second at 20 TPS]", "Maximum stored cooling duration in a Breeze Cooler. This controls cooling time, not fluid capacity.");
        public final ConfigInt coolingTicksPerSnowball = i(20, 0, "cooling_ticks_per_snowball", "[Unit: ticks; 20 ticks = 1 second at 20 TPS]", "Cooling duration added when a snowball hits a Breeze Cooler. Set to 0 to disable cooling with snowballs.");

        @Override
        public String getName() {
            return "breeze_cooler";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class GasInjectionChamber extends ConfigBase {
        public final ConfigInt fanProcessingGasPerItem = i(500, 0, 10000, "fan_processing_gas_per_item", "[Unit: GU per item]", "Base gas consumption per item processed by the Gas Injection Chamber fan. Set to 0 to disable this consumption.");
        public final ConfigInt gasVolume = i(10, 1, "gas_volume", "[Unit: kL]", "Physical gas volume of the Gas Injection Chamber tank. Gas amount at a given pressure scales with this volume.");

        @Override
        public String getName() {
            return "gas_injection_chamber";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class GasPackager extends ConfigBase {
        public final ConfigInt referenceGasPerBalloon = i(10, 1, "reference_gas_per_balloon", "[Unit: kGU]", "Gas amount corresponding to one standard balloon volume at 1 atm. The local packing limit scales with ambient pressure.");

        @Override
        public String getName() {
            return "gas_packager";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class TeslaTurbine extends ConfigBase {
        public final ConfigBool explodesOnIncompatibleGases = b(true, "explodes_on_incompatible_gases", "Cause a Tesla Turbine to explode and lose its rotors when incompatible gases are mixed.");
        public final ConfigFloat explosionStrengthMultiplier = f(1, 0, "explosion_strength_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Multiplier applied to the explosion strength when incompatible gases are mixed in a Tesla Turbine. Base strength equals the installed rotor count. A value of 0 does not protect the rotors when mixed-gas failure is enabled.");
        public final ConfigBool explosionDamagesSurroundingBlocks = b(false, "explosion_damages_surrounding_blocks", "Allow explosions from incompatible gases in a Tesla Turbine to destroy surrounding blocks. This does not prevent rotor loss when mixed-gas failure is enabled.");

        @Override
        public String getName() {
            return "tesla_turbine";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class AirtightAssemblyDriver extends ConfigBase {
        public final ConfigBool useFluidResidueRoundRobin = b(false, "use_fluid_residue_round_robin", "Rotate the preferred Residue Outlet after each successfully generated Fluid Residue batch. Disable to prioritize the first outlet for every batch.");
        public final ConfigBool useItemResidueRoundRobin = b(false, "use_item_residue_round_robin", "Rotate the preferred Residue Outlet after each successfully generated Item Residue batch. Disable to prioritize the first outlet for every batch.");
        public final ConfigFloat fluidResidueQuantityMultiplier = f(1, 0.125F, 64, "fluid_residue_quantity_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Scales Fluid Residue generation with the Airtight Assembly Driver level. Each generation batch produces 8 times the level times this multiplier in mB, rounded down to whole mB and subject to available output space.");
        public final ConfigFloat itemResidueQuantityMultiplier = f(1, 0.125F, 8, "item_residue_quantity_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Scales standard Item Residue generation progress, whose base coefficient per driver level is 8. Fractional progress accumulates before whole items are emitted; output space can limit successful generation.");

        @Override
        public String getName() {
            return "airtight_assembly_driver";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class EndIncinerationBlower extends ConfigBase {
        public final ConfigBool ignitionAffectsPlayers = b(true, "ignition_affects_players", "Allow the End Incineration Blower ignition mode to affect players.");
        public final ConfigFloat ignitionDamagePerPulse = f(2, 0, "ignition_damage_per_pulse", "[Unit: damage points]", "Direct damage per ignition pulse to living entities other than Snow Golems. A value of 0 disables this direct damage but does not disable ignition.");
        public final ConfigFloat maxRadius = f(3.5F, 0.5F, 7, "max_radius", "[Unit: blocks]", "Maximum radius of the End Incineration Blower cubic working area. Increasing the radius increases scanning overhead.");

        @Override
        public String getName() {
            return "end_incineration_blower";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class EndSculkSilencer extends ConfigBase {
        public final ConfigFloat requiredSpeedMultiplier = f(1, 0, "required_speed_multiplier", "[Unit: multiplier relative to standard behavior; 1 = standard]", "Multiplier applied to the rotation speed required for each End Sculk Silencer range setting.");

        @Override
        public String getName() {
            return "end_sculk_silencer";
        }
    }
}
