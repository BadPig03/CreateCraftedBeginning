package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtights extends ConfigBase {
    @SuppressWarnings("unused")
    public final ConfigGroup airCompressor = group(0, "air_compressor", "Air Compressor");
    public final ConfigBool explodesOnMeltdown = b(true, "explodes_on_meltdown", Comments.explodesOnMeltdown);
    public final ConfigFloat coolantConsumptionChance = f(0.5f, 0, 1, "coolant_consumption_chance", Comments.coolantConsumptionChance);
    public final ConfigInt maxAirCompressorCapacity = i(10, 1, "max_capacity", Comments.buckets, Comments.maxAirCompressorCapacity);
    public final ConfigInt nextOverheatThreshold = i(1200, 20, "next_overheat_threshold", Comments.heatUnits, Comments.nextOverheatThreshold);
    public final ConfigFloat pressurizationRateMultiplier = f(4, 0.0625f, "pressurization_rate_multiplier", Comments.pressurizationRateMultiplier);
    public final ConfigBool useFluidResidueRoundRobin = b(false, "use_fluid_residue_round_robin", Comments.useFluidResidueRoundRobin);
    public final ConfigBool useItemResidueRoundRobin = b(false, "use_item_residue_round_robin", Comments.useItemResidueRoundRobin);
    public final ConfigInt fluidQuantityMultiplier = i(8, 1, 512, "fluid_quantity_multiplier", Comments.fluidQuantityMultiplier);
    public final ConfigInt itemQuantityMultiplier = i(8, 1, 64, "item_quantity_multiplier", Comments.itemQuantityMultiplier);
    public final ConfigInt residueOutletCapacity = i(4, 1, "residue_outlet_capacity", Comments.buckets, Comments.residueOutletCapacity);
    @SuppressWarnings("unused")
    public final ConfigGroup airtightForgingPress = group(0, "airtight_forging_press", "Airtight Forging Press");
    public final ConfigBool enableAutomaticPressingRecipes = b(true, "enable_automatic_pressing_recipes", Comments.enableAutomaticPressingRecipes);
    public final ConfigBool enableAutomaticSmithingRecipes = b(true, "enable_automatic_smithing_recipes", Comments.enableAutomaticSmithingRecipes);
    public final ConfigInt forgingPressFluidCapacity = i(3, 1, "fluid_capacity", Comments.buckets, Comments.forgingPressFluidCapacity);
    public final ConfigInt forgingPressGasCapacity = i(30, 1, "gas_capacity", Comments.buckets, Comments.forgingPressGasCapacity);
    @SuppressWarnings("unused")
    public final ConfigGroup airtightHatch = group(0, "airtight_hatch", "Airtight Hatch");
    public final ConfigInt maxTransferRate = i(50, 1, "max_transfer_rate", Comments.milliBuckets, Comments.maxTransferRate);
    @SuppressWarnings("unused")
    public final ConfigGroup airtightPump = group(0, "airtight_pump", "Airtight Pump");
    public final ConfigInt maxPumpRange = i(32, 1, "max_pump_range", Comments.blocks, Comments.maxPumpRange);
    @SuppressWarnings("unused")
    public final ConfigGroup airtightReactorKettle = group(0, "airtight_reactor_kettle", "Airtight Reactor Kettle");
    public final ConfigBool enableAutomaticMixingRecipes = b(true, "enable_automatic_mixing_recipes", Comments.enableAutomaticMixingRecipes);
    public final ConfigFloat reactorKettleMixerDamageMultiplier = f(1, 0, "mixer_damage_multiplier", Comments.reactorKettleMixerDamageMultiplier);
    public final ConfigInt reactorKettleFluidCapacity = i(9, 1, "fluid_capacity_per_tank", Comments.buckets, Comments.reactorKettleFluidCapacity);
    public final ConfigInt reactorKettleGasCapacity = i(90, 1, "gas_capacity_per_tank", Comments.buckets, Comments.reactorKettleGasCapacity);
    @SuppressWarnings("unused")
    public final ConfigGroup airtightTank = group(0, "airtight_tank", "Airtight Tank");
    public final ConfigInt maxAirtightTankCapacityPerBlock = i(80, 1, "capacity_per_block", Comments.buckets, Comments.maxAirtightTankCapacityPerBlock);
    public final ConfigInt maxAirtightTankLength = i(4, 1, 32, "max_length", Comments.blocks, Comments.maxAirtightTankLength);
    public final ConfigInt maxAirtightTankWidth = i(3, 1, 16, "max_width", Comments.blocks, Comments.maxAirtightTankWidth);
    @SuppressWarnings("unused")
    public final ConfigGroup breezeChamber = group(0, "breeze_chamber", "Breeze Chamber");
    public final ConfigInt maxBreezeChamberCapacity = i(10, 1, "max_gas_capacity", Comments.buckets, Comments.maxBreezeChamberCapacity);
    public final ConfigInt maxProcessingRate = i(1000, 1, "max_processing_rate", Comments.milliBuckets, Comments.maxProcessingRate);
    public final ConfigInt maxWindCapacity = i(72000, 20, "max_wind_capacity", Comments.gameTicks, Comments.maxWindCapacity);
    @SuppressWarnings("unused")
    public final ConfigGroup breezeCooler = group(0, "breeze_cooler", "Breeze Cooler");
    public final ConfigInt breezeCoolerFluidCapacity = i(4, 1, "fluid_capacity", Comments.buckets, Comments.breezeCoolerFluidCapacity);
    public final ConfigInt dangerousFluidTemperature = i(1300, 1, "dangerous_fluid_temperature", Comments.kelvin, Comments.dangerousFluidTemperature);
    public final ConfigInt maxCoolantCapacity = i(72000, 20, "max_coolant_capacity", Comments.gameTicks, Comments.maxCoolantCapacity);
    public final ConfigInt snowballCoolingTime = i(20, 0, "snowball_cooling_time", Comments.gameTicks, Comments.snowballCoolingTime);
    @SuppressWarnings("unused")
    public final ConfigGroup gasCanister = group(0, "gas_canister", "Gas Canister");
    public final ConfigInt maxCanisterCapacity = i(20, 1, "max_canister_capacity", Comments.buckets, Comments.maxCanisterCapacity);
    @SuppressWarnings("unused")
    public final ConfigGroup gasInjectionChamber = group(0, "gas_injection_chamber", "Gas Injection Chamber");
    public final ConfigInt baseFanProcessingGasPerItem = i(100, 0, 10000, "base_fan_processing_gas_per_item", Comments.milliBuckets, Comments.baseFanProcessingGasPerItem);
    public final ConfigInt maxGasInjectionChamberCapacity = i(10, 1, "max_capacity", Comments.buckets, Comments.maxGasInjectionChamberCapacity);
    @SuppressWarnings("unused")
    public final ConfigGroup gasPackager = group(0, "gas_packager", "Gas Packager");
    public final ConfigInt maxGasPerBalloon = i(10, 1, "max_gas_per_balloon", Comments.buckets, Comments.maxGasPerBalloon);
    @SuppressWarnings("unused")
    public final ConfigGroup teslaTurbine = group(0, "tesla_turbine", "Tesla Turbine");
    public final ConfigBool teslaTurbineExplodesOnMixedGases = b(true, "explodes_on_mixed_gases", Comments.teslaTurbineExplodesOnMixedGases);
    public final ConfigFloat teslaTurbineExplosionStrengthMultiplier = f(1, 0, "explosion_strength_multiplier", Comments.teslaTurbineExplosionStrengthMultiplier);
    public final ConfigBool canCoolerGetFromSpawners = b(true, "canCoolerGetFromSpawners", Comments.canCoolerGetFromSpawners);
    public final ConfigBool canExtractAirFromWorld = b(true, "canExtractAirFromWorld", Comments.canExtractAirFromWorld);
    @SuppressWarnings("unused")
    private final ConfigGroup airtightAssemblyDriver = group(0, "airtight_assembly_driver", "Airtight Assembly Driver");
    @SuppressWarnings("unused")
    private final ConfigGroup worldSettings = group(0, "world_settings", "World Settings");

    @Override
    public String getName() {
        return "airtights";
    }

    private static class Comments {
        public static final String blocks = "[in blocks]";
        public static final String buckets = "[in buckets]";
        private static final String gameTicks = "[in game ticks]";
        private static final String heatUnits = "[in heat units]";
        private static final String milliBuckets = "[in millibuckets]";
        private static final String kelvin = "[in Kelvin]";

        private static final String breezeCoolerFluidCapacity = "The fluid input capacity of a Breeze Cooler.";
        private static final String canCoolerGetFromSpawners = "Whether an Empty Breeze Cooler can capture a Breeze from a spawner or trial spawner.";
        private static final String canExtractAirFromWorld = "Whether open-ended pipes can extract air from the environment.";
        private static final String coolantConsumptionChance = "The chance, from 0.0 to 1.0, that an operating Air Compressor consumes or melts the coolant block during a coolant check.";
        private static final String dangerousFluidTemperature = "Fluids at or above this temperature destroy a non-creative Breeze Cooler.";
        private static final String enableAutomaticMixingRecipes = "Whether the Airtight Reactor Kettle can automatically process eligible shapeless crafting recipes.";
        private static final String enableAutomaticPressingRecipes = "Whether the Airtight Forging Press can automatically process Create pressing recipes.";
        private static final String enableAutomaticSmithingRecipes = "Whether the Airtight Forging Press can automatically process smithing recipes.";
        private static final String explodesOnMeltdown = "Whether the Air Compressor explodes upon entering the Meltdown state. If explosions are disabled, the Air Compressor is destroyed without exploding.";
        private static final String fluidQuantityMultiplier = "The quantity multiplier for Fluid Residue generated by the Airtight Assembly Driver.";
        private static final String useFluidResidueRoundRobin = "Whether Fluid Residue generation rotates its preferred Residue Outlet after each successful batch. Disable to always prioritize the first outlet.";
        private static final String forgingPressFluidCapacity = "The fluid capacity of the Airtight Forging Press input tank.";
        private static final String forgingPressGasCapacity = "The gas capacity of the Airtight Forging Press input tank.";
        private static final String itemQuantityMultiplier = "The quantity multiplier for Item Residue generated by the Airtight Assembly Driver.";
        private static final String useItemResidueRoundRobin = "Whether Item Residue generation rotates its preferred Residue Outlet after each successful batch. Disable to always prioritize the first outlet.";
        private static final String maxAirCompressorCapacity = "The capacity of each gas tank in an Air Compressor.";
        private static final String maxAirtightTankCapacityPerBlock = "The gas capacity contributed by each block in an Airtight Tank multiblock.";
        private static final String maxAirtightTankLength = "The maximum height of an Airtight Tank or Creative Airtight Tank multiblock. Existing tanks may need to be reassembled after changing this value.";
        private static final String maxAirtightTankWidth = "The maximum width and depth of an Airtight Tank or Creative Airtight Tank multiblock. Existing tanks may need to be reassembled after changing this value.";
        private static final String maxBreezeChamberCapacity = "The gas capacity of a Breeze Chamber.";
        private static final String maxCanisterCapacity = "The maximum gas capacity of the Gas Canister.";
        private static final String maxCoolantCapacity = "The maximum stored cooling time of a Breeze Cooler.";
        private static final String maxGasInjectionChamberCapacity = "The maximum gas capacity of the Gas Injection Chamber.";
        private static final String baseFanProcessingGasPerItem = "The base amount of gas consumed per item when the Gas Injection Chamber performs fan processing. Set to 0 to disable consumption.";
        private static final String maxGasPerBalloon = "The maximum amount of gas that a single balloon can carry.";
        private static final String maxProcessingRate = "The maximum amount of gas a Breeze Chamber can process per second.";
        private static final String maxPumpRange = "The maximum distance an Airtight Pump can push or pull gas in either direction.";
        private static final String maxTransferRate = "The maximum amount of gas an Airtight Hatch can transfer per second.";
        private static final String maxWindCapacity = "The maximum duration of positive or negative wind charge that a Breeze Chamber can store.";
        private static final String nextOverheatThreshold = "The net heat required for the Air Compressor to advance to the next overheat state.";
        private static final String pressurizationRateMultiplier = "The multiplier applied to the Air Compressor's gas pressurization rate.";
        private static final String reactorKettleFluidCapacity = "The capacity of each fluid tank segment in the Airtight Reactor Kettle.";
        private static final String reactorKettleGasCapacity = "The capacity of each gas tank segment in the Airtight Reactor Kettle.";
        private static final String reactorKettleMixerDamageMultiplier = "The multiplier applied to damage dealt by an operating Airtight Reactor Kettle mixer. Set to 0 to disable mixer damage.";
        private static final String residueOutletCapacity = "The fluid capacity of a Residue Outlet.";
        private static final String snowballCoolingTime = "The amount of cooling time added when a snowball hits a Breeze Cooler. Set to 0 to disable cooling with snowballs.";
        private static final String teslaTurbineExplodesOnMixedGases = "Whether a Tesla Turbine explodes and loses its rotors after incompatible gases are mixed.";
        private static final String teslaTurbineExplosionStrengthMultiplier = "The multiplier applied to the strength of explosions caused by mixing incompatible gases in a Tesla Turbine. The base strength equals the number of installed rotors.";
    }
}
