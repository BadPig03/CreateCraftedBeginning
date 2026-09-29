package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightFlowmeterBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightManometerBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralCogBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump.AirtightRegulatorPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.HorizontalAirtightTankBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightvalve.AirtightValveBlock;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock;
import net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet.BoilerSteamOutletBlock;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlock;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterBlock;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterBlock;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBlock;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlock;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlock;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerBlock;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasunpackager.GasUnpackagerBlock;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceBlock;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlock;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.EmptyBreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.crates.andesitecrate.AndesiteCrateBlock;
import net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateBlock;
import net.ty.createcraftedbeginning.content.crates.cardboardcrate.CardboardCrateBlock;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlock;
import net.ty.createcraftedbeginning.content.end.endcasing.EndCasingBlock;
import net.ty.createcraftedbeginning.content.end.endincinerationblower.EndIncinerationBlowerBlock;
import net.ty.createcraftedbeginning.content.end.endincinerationblower.EndIncinerationBlowerStructuralBlock;
import net.ty.createcraftedbeginning.content.end.endsculksilencer.EndSculkSilencerBlock;
import net.ty.createcraftedbeginning.content.end.endsculksilencer.EndSculkSilencerMovementBehaviour;
import net.ty.createcraftedbeginning.content.end.endsculksilencer.EndSculkSilencerStructuralBlock;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlock;
import net.ty.createcraftedbeginning.content.opticalpower.laserreceiver.LaserReceiverBlock;
import net.ty.createcraftedbeginning.content.opticalpower.opticalfiber.OpticalFiberBlock;
import net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel.AmethystCollectorPanelBlock;
import net.ty.createcraftedbeginning.content.pneumaticengine.PneumaticEngineBlock;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout.CCBCreativeTabSection;
import net.ty.createcraftedbeginning.registry.registrate.AirtightBlockProperties;
import net.ty.createcraftedbeginning.registry.registrate.AirtightMachineRegistration;
import net.ty.createcraftedbeginning.registry.registrate.BreezeRegistration;
import net.ty.createcraftedbeginning.registry.registrate.CCBCreativeSectionTracker;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrateProvider;
import net.ty.createcraftedbeginning.registry.registrate.CrateRegistration;
import net.ty.createcraftedbeginning.registry.registrate.EndRegistration;
import net.ty.createcraftedbeginning.registry.registrate.GasLogisticsRegistration;
import net.ty.createcraftedbeginning.registry.registrate.GasPipeRegistration;
import net.ty.createcraftedbeginning.registry.registrate.GasStorageRegistration;
import net.ty.createcraftedbeginning.registry.registrate.ObsidianRegistration;
import net.ty.createcraftedbeginning.registry.registrate.OpticalRegistration;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBBlocks {
    private static final CreateRegistrate CCB_REGISTRATE = CCBRegistrateProvider.get();

    static {
        CCBCreativeSectionTracker.set(CCB_REGISTRATE, CCBCreativeTabSection.AIRTIGHTS);
    }

    public static final BlockEntry<Block> AIRTIGHT_SHEET_BLOCK = CCB_REGISTRATE.block("airtight_sheet_block", Block::new).transform(GasStorageRegistration.airtightSheetBlock()).transform(AirtightBlockProperties.airtightMetal()).register();

    public static final BlockEntry<AirtightPipeBlock> AIRTIGHT_PIPE_BLOCK = CCB_REGISTRATE.block("airtight_pipe", AirtightPipeBlock::new).transform(GasPipeRegistration.airtightPipe()).transform(AirtightBlockProperties.airtightComponent()).register();
    public static final BlockEntry<AirtightEncasedPipeBlock> AIRTIGHT_ENCASED_PIPE_BLOCK = CCB_REGISTRATE.block("airtight_encased_pipe", AirtightEncasedPipeBlock::new).transform(GasPipeRegistration.airtightEncasedPipe()).transform(AirtightBlockProperties.airtightComponent()).register();
    public static final BlockEntry<AirtightCheckValveBlock> AIRTIGHT_CHECK_VALVE_BLOCK = CCB_REGISTRATE.block("airtight_check_valve", AirtightCheckValveBlock::new).transform(GasPipeRegistration.airtightCheckValve()).transform(AirtightBlockProperties.airtightComponent()).register();
    public static final BlockEntry<AirtightValveBlock> AIRTIGHT_VALVE_BLOCK = CCB_REGISTRATE.block("airtight_valve", AirtightValveBlock::new).transform(GasPipeRegistration.airtightValve()).transform(AirtightBlockProperties.airtightComponentWithImpact(4)).register();
    public static final BlockEntry<SmartAirtightPipeBlock> SMART_AIRTIGHT_PIPE_BLOCK = CCB_REGISTRATE.block("smart_airtight_pipe", SmartAirtightPipeBlock::new).transform(GasPipeRegistration.smartAirtightPipe()).transform(AirtightBlockProperties.airtightComponent()).register();
    public static final BlockEntry<AirtightPumpBlock> AIRTIGHT_PUMP_BLOCK = CCB_REGISTRATE.block("airtight_pump", AirtightPumpBlock::new).transform(GasPipeRegistration.airtightPump()).transform(AirtightBlockProperties.airtightComponentWithImpact(8)).register();
    public static final BlockEntry<AirtightRegulatorPumpBlock> AIRTIGHT_REGULATOR_PUMP_BLOCK = CCB_REGISTRATE.block("airtight_regulator_pump", AirtightRegulatorPumpBlock::new).transform(GasPipeRegistration.airtightRegulatorPump()).transform(AirtightBlockProperties.airtightComponentWithImpact(16)).register();

    public static final BlockEntry<AirtightFlowmeterBlock> AIRTIGHT_FLOWMETER_BLOCK = CCB_REGISTRATE.block("airtight_flowmeter", AirtightFlowmeterBlock::new).transform(GasPipeRegistration.airtightMeter("flowmeter")).transform(GasPipeRegistration.airtightFlowmeterProperties()).register();
    public static final BlockEntry<AirtightManometerBlock> AIRTIGHT_MANOMETER_BLOCK = CCB_REGISTRATE.block("airtight_manometer", AirtightManometerBlock::new).transform(GasPipeRegistration.airtightMeter("manometer")).transform(GasPipeRegistration.airtightManometerProperties()).register();

    public static final BlockEntry<AirtightTankBlock> AIRTIGHT_TANK_BLOCK = CCB_REGISTRATE.block("airtight_tank", AirtightTankBlock::new).transform(GasStorageRegistration.airtightTank()).transform(GasStorageRegistration.airtightTankProperties()).register();
    public static final BlockEntry<AirtightFractionationTowerBlock> AIRTIGHT_FRACTIONATION_TOWER_BLOCK = CCB_REGISTRATE.block("airtight_fractionation_tower", AirtightFractionationTowerBlock::new).transform(AirtightMachineRegistration.airtightFractionationTower()).transform(AirtightBlockProperties.airtightComponent()).register();
    public static final BlockEntry<HorizontalAirtightTankBlock> HORIZONTAL_AIRTIGHT_TANK_BLOCK = CCB_REGISTRATE.block("horizontal_airtight_tank", HorizontalAirtightTankBlock::new).transform(GasStorageRegistration.horizontalAirtightTank()).transform(GasStorageRegistration.horizontalAirtightTankProperties()).register();
    public static final BlockEntry<CreativeAirtightTankBlock> CREATIVE_AIRTIGHT_TANK_BLOCK = CCB_REGISTRATE.block("creative_airtight_tank", CreativeAirtightTankBlock::new).transform(GasStorageRegistration.creativeAirtightTank()).transform(GasStorageRegistration.creativeAirtightTankProperties()).register();

    public static final BlockEntry<AirtightHatchBlock> AIRTIGHT_HATCH_BLOCK = CCB_REGISTRATE.block("airtight_hatch", AirtightHatchBlock::new).transform(GasStorageRegistration.airtightHatch()).transform(AirtightBlockProperties.airtightComponent()).register();
    public static final BlockEntry<GasInjectionChamberBlock> GAS_INJECTION_CHAMBER_BLOCK = CCB_REGISTRATE.block("gas_injection_chamber", GasInjectionChamberBlock::new).transform(AirtightMachineRegistration.gasInjectionChamber()).transform(AirtightBlockProperties.airtightComponent()).register();
    public static final BlockEntry<PortableGasInterfaceBlock> PORTABLE_GAS_INTERFACE_BLOCK = CCB_REGISTRATE.block("portable_gas_interface", PortableGasInterfaceBlock::new).transform(GasLogisticsRegistration.portableGasInterface()).transform(GasLogisticsRegistration.portableGasInterfaceProperties()).register();

    public static final BlockEntry<EmptyBreezeCoolerBlock> EMPTY_BREEZE_COOLER_BLOCK = CCB_REGISTRATE.block("empty_breeze_cooler", EmptyBreezeCoolerBlock::new).transform(BreezeRegistration.emptyBreezeCooler()).transform(BreezeRegistration.breezeProperties()).register();
    public static final BlockEntry<BreezeCoolerBlock> BREEZE_COOLER_BLOCK = CCB_REGISTRATE.block("breeze_cooler", BreezeCoolerBlock::new).transform(BreezeRegistration.breezeCooler()).transform(BreezeRegistration.breezeCoolerProperties()).register();
    public static final BlockEntry<BreezeChamberBlock> BREEZE_CHAMBER_BLOCK = CCB_REGISTRATE.block("breeze_chamber", BreezeChamberBlock::new).transform(BreezeRegistration.breezeChamber()).transform(BreezeRegistration.breezeChamberProperties()).register();

    public static final BlockEntry<AirtightEngineBlock> AIRTIGHT_ENGINE_BLOCK = CCB_REGISTRATE.block("airtight_engine", AirtightEngineBlock::new).transform(AirtightMachineRegistration.airtightEngine()).transform(AirtightBlockProperties.airtightComponentWithCapacity(1024)).register();
    public static final BlockEntry<ResidueOutletBlock> RESIDUE_OUTLET_BLOCK = CCB_REGISTRATE.block("residue_outlet", ResidueOutletBlock::new).transform(AirtightMachineRegistration.residueOutlet()).transform(AirtightBlockProperties.airtightComponent()).register();
    public static final BlockEntry<BoilerSteamOutletBlock> BOILER_STEAM_OUTLET_BLOCK = CCB_REGISTRATE.block("boiler_steam_outlet", BoilerSteamOutletBlock::new).transform(AirtightMachineRegistration.boilerSteamOutlet()).transform(AirtightMachineRegistration.boilerSteamOutletProperties()).register();

    public static final BlockEntry<TeslaTurbineBlock> TESLA_TURBINE_BLOCK = CCB_REGISTRATE.block("tesla_turbine", TeslaTurbineBlock::new).transform(AirtightMachineRegistration.teslaTurbine()).transform(AirtightMachineRegistration.teslaTurbineProperties()).register();
    public static final BlockEntry<TeslaTurbineStructuralBlock> TESLA_TURBINE_STRUCTURAL_BLOCK = CCB_REGISTRATE.block("tesla_turbine_structural", TeslaTurbineStructuralBlock::new).transform(AirtightMachineRegistration.teslaTurbineStructural()).transform(AirtightBlockProperties.airtightStructural()).register();
    public static final BlockEntry<TeslaTurbineNozzleBlock> TESLA_TURBINE_NOZZLE_BLOCK = CCB_REGISTRATE.block("tesla_turbine_nozzle", TeslaTurbineNozzleBlock::new).transform(AirtightMachineRegistration.teslaTurbineNozzle()).transform(AirtightBlockProperties.airtightComponent()).register();

    public static final BlockEntry<AirtightReactorKettleBlock> AIRTIGHT_REACTOR_KETTLE_BLOCK = CCB_REGISTRATE.block("airtight_reactor_kettle", AirtightReactorKettleBlock::new).transform(AirtightMachineRegistration.airtightReactorKettle()).transform(AirtightBlockProperties.airtightComponent()).register();
    public static final BlockEntry<AirtightReactorKettleStructuralBlock> AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_BLOCK = CCB_REGISTRATE.block("airtight_reactor_kettle_structural", AirtightReactorKettleStructuralBlock::new).transform(AirtightMachineRegistration.airtightReactorKettleStructural()).transform(AirtightBlockProperties.airtightStructural()).register();
    public static final BlockEntry<AirtightReactorKettleStructuralCogBlock> AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_COG_BLOCK = CCB_REGISTRATE.block("airtight_reactor_kettle_structural_cog", AirtightReactorKettleStructuralCogBlock::new).transform(AirtightMachineRegistration.airtightReactorKettleStructuralCog()).transform(AirtightBlockProperties.airtightStructuralWithImpact(16)).register();

    public static final BlockEntry<AirtightForgingPressBlock> AIRTIGHT_FORGING_PRESS_BLOCK = CCB_REGISTRATE.block("airtight_forging_press", AirtightForgingPressBlock::new).transform(AirtightMachineRegistration.airtightForgingPress()).transform(AirtightBlockProperties.airtightComponent()).register();
    public static final BlockEntry<AirtightForgingPressStructuralBlock> AIRTIGHT_FORGING_PRESS_STRUCTURAL_BLOCK = CCB_REGISTRATE.block("airtight_forging_press_structural", AirtightForgingPressStructuralBlock::new).transform(AirtightMachineRegistration.airtightForgingPressStructural()).transform(AirtightBlockProperties.airtightStructural()).register();
    public static final BlockEntry<AirtightForgingPressStructuralShaftBlock> AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT_BLOCK = CCB_REGISTRATE.block("airtight_forging_press_structural_shaft", AirtightForgingPressStructuralShaftBlock::new).transform(AirtightMachineRegistration.airtightForgingPressStructuralShaft()).transform(AirtightBlockProperties.airtightStructuralWithImpact(16)).register();

    public static final BlockEntry<GasPackagerBlock> GAS_PACKAGER_BLOCK = CCB_REGISTRATE.block("gas_packager", GasPackagerBlock::new).transform(GasLogisticsRegistration.gasPackager()).transform(GasLogisticsRegistration.airtightRedstoneComponentProperties()).register();
    public static final BlockEntry<GasUnpackagerBlock> GAS_UNPACKAGER_BLOCK = CCB_REGISTRATE.block("gas_unpackager", GasUnpackagerBlock::new).transform(GasLogisticsRegistration.gasUnpackager()).transform(GasLogisticsRegistration.airtightRedstoneComponentProperties()).register();
    public static final BlockEntry<GasRepackagerBlock> GAS_REPACKAGER_BLOCK = CCB_REGISTRATE.block("gas_repackager", GasRepackagerBlock::new).transform(GasLogisticsRegistration.gasRepackager()).transform(GasLogisticsRegistration.airtightRedstoneComponentProperties()).register();
    public static final BlockEntry<GasFactoryGaugeBlock> GAS_FACTORY_GAUGE_BLOCK = CCB_REGISTRATE.block("gas_factory_gauge", GasFactoryGaugeBlock::new).transform(GasLogisticsRegistration.gasFactoryGauge()).transform(GasLogisticsRegistration.gasFactoryGaugeProperties()).register();

    public static final BlockEntry<GasCanisterBlock> GAS_CANISTER_BLOCK = CCB_REGISTRATE.block("gas_canister", GasCanisterBlock::new).transform(GasStorageRegistration.gasCanister()).transform(AirtightBlockProperties.airtightMetal()).register();
    public static final BlockEntry<CreativeGasCanisterBlock> CREATIVE_GAS_CANISTER_BLOCK = CCB_REGISTRATE.block("creative_gas_canister", CreativeGasCanisterBlock::new).transform(GasStorageRegistration.creativeGasCanister()).transform(AirtightBlockProperties.airtightMetal()).register();

    static {
        CCBCreativeSectionTracker.set(CCB_REGISTRATE, CCBCreativeTabSection.OPTICAL_POWER);
    }

    public static final BlockEntry<ColoredFallingBlock> POWDERED_AMETHYST_BLOCK = CCB_REGISTRATE.block("powdered_amethyst_block", properties -> new ColoredFallingBlock(new ColorRGBA(0xFF8D6ACC), properties)).transform(OpticalRegistration.powderedAmethystBlock()).transform(OpticalRegistration.powderedAmethystBlockProperties()).register();

    public static final BlockEntry<OpticalFiberBlock> OPTICAL_FIBER_BLOCK = CCB_REGISTRATE.block("optical_fiber", OpticalFiberBlock::new).transform(OpticalRegistration.opticalFiber()).transform(OpticalRegistration.opticalComponentProperties()).register();
    public static final BlockEntry<AmethystCollectorPanelBlock> AMETHYST_COLLECTOR_PANEL_BLOCK = CCB_REGISTRATE.block("amethyst_collector_panel", AmethystCollectorPanelBlock::new).transform(OpticalRegistration.amethystCollectorPanel()).transform(OpticalRegistration.opticalComponentProperties()).register();
    public static final BlockEntry<LaserEmitterBlock> LASER_EMITTER_BLOCK = CCB_REGISTRATE.block("laser_emitter", LaserEmitterBlock::new).transform(OpticalRegistration.laserEmitter()).transform(OpticalRegistration.opticalComponentProperties()).register();
    public static final BlockEntry<LaserReceiverBlock> LASER_RECEIVER_BLOCK = CCB_REGISTRATE.block("laser_receiver", LaserReceiverBlock::new).transform(OpticalRegistration.laserReceiver()).transform(OpticalRegistration.laserReceiverProperties()).register();

    static {
        CCBCreativeSectionTracker.set(CCB_REGISTRATE, CCBCreativeTabSection.ENDS);
    }

    public static final BlockEntry<Block> END_ALLOY_BLOCK = CCB_REGISTRATE.block("end_alloy_block", Block::new).transform(EndRegistration.endAlloyBlock()).transform(EndRegistration.endAlloyBlockProperties()).register();

    public static final BlockEntry<EndCasingBlock> END_CASING_BLOCK = CCB_REGISTRATE.block("end_casing", EndCasingBlock::new).transform(EndRegistration.endCasing()).transform(EndRegistration.endCasingProperties()).register();
    public static final BlockEntry<EndIncinerationBlowerBlock> END_INCINERATION_BLOWER_BLOCK = CCB_REGISTRATE.block("end_incineration_blower", EndIncinerationBlowerBlock::new).transform(EndRegistration.endIncinerationBlower()).transform(EndRegistration.endComponentWithImpactProperties(4)).register();
    public static final BlockEntry<EndIncinerationBlowerStructuralBlock> END_INCINERATION_BLOWER_STRUCTURAL_BLOCK = CCB_REGISTRATE.block("end_incineration_blower_structural", EndIncinerationBlowerStructuralBlock::new).transform(EndRegistration.endIncinerationBlowerStructural()).transform(EndRegistration.endComponentWithImpactProperties(0)).register();
    public static final BlockEntry<EndSculkSilencerBlock> END_SCULK_SILENCER_BLOCK = CCB_REGISTRATE.block("end_sculk_silencer", EndSculkSilencerBlock::new).transform(EndRegistration.endSculkSilencer()).transform(EndRegistration.endComponentWithImpactProperties(4)).onRegister(MovementBehaviour.movementBehaviour(new EndSculkSilencerMovementBehaviour())).register();
    public static final BlockEntry<EndSculkSilencerStructuralBlock> END_SCULK_SILENCER_STRUCTURAL_BLOCK = CCB_REGISTRATE.block("end_sculk_silencer_structural", EndSculkSilencerStructuralBlock::new).transform(EndRegistration.endSculkSilencerStructural()).transform(EndRegistration.endComponentWithImpactProperties(0)).register();

    static {
        CCBCreativeSectionTracker.set(CCB_REGISTRATE, CCBCreativeTabSection.DECORATIONS);
    }

    public static final BlockEntry<AndesiteCrateBlock> ANDESITE_CRATE_BLOCK = CCB_REGISTRATE.block("andesite_crate", AndesiteCrateBlock::new).transform(CrateRegistration.crate("andesite")).transform(CrateRegistration.andesiteCrateProperties()).register();
    public static final BlockEntry<BrassCrateBlock> BRASS_CRATE_BLOCK = CCB_REGISTRATE.block("brass_crate", BrassCrateBlock::new).transform(CrateRegistration.crate("brass")).transform(CrateRegistration.brassCrateProperties()).register();
    public static final BlockEntry<SturdyCrateBlock> STURDY_CRATE_BLOCK = CCB_REGISTRATE.block("sturdy_crate", SturdyCrateBlock::new).transform(CrateRegistration.uncontainableCrate()).transform(CrateRegistration.sturdyCrateProperties()).register();
    public static final BlockEntry<CardboardCrateBlock> CARDBOARD_CRATE_BLOCK = CCB_REGISTRATE.block("cardboard_crate", CardboardCrateBlock::new).transform(CrateRegistration.crate("cardboard")).transform(CrateRegistration.cardboardCrateProperties()).register();
    public static final BlockEntry<AirVentBlock> AIR_VENT_BLOCK = CCB_REGISTRATE.block("air_vent", AirVentBlock::new).transform(GasPipeRegistration.airVent()).transform(GasPipeRegistration.airVentProperties()).register();

    public static final BlockEntry<Block> OBSIDIAN_BRICKS = CCB_REGISTRATE.block("obsidian_bricks", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<SlabBlock> OBSIDIAN_BRICKS_SLAB = CCB_REGISTRATE.block("obsidian_bricks_slab", properties -> new SlabBlock(Properties.ofFullCopy(OBSIDIAN_BRICKS.get()))).transform(ObsidianRegistration.obsidianAlikeSlabs("obsidian_bricks")).transform(ObsidianRegistration.obsidianSlabProperties()).register();
    public static final BlockEntry<StairBlock> OBSIDIAN_BRICKS_STAIRS = CCB_REGISTRATE.block("obsidian_bricks_stairs", properties -> new StairBlock(OBSIDIAN_BRICKS.get().defaultBlockState(), Properties.ofFullCopy(OBSIDIAN_BRICKS.get()))).transform(ObsidianRegistration.obsidianAlikeStairs("obsidian_bricks")).transform(ObsidianRegistration.obsidianStairsProperties()).register();
    public static final BlockEntry<WallBlock> OBSIDIAN_BRICKS_WALL = CCB_REGISTRATE.block("obsidian_bricks_wall", properties -> new WallBlock(Properties.ofFullCopy(OBSIDIAN_BRICKS.get()).forceSolidOn())).transform(ObsidianRegistration.obsidianAlikeWall("obsidian_bricks")).transform(ObsidianRegistration.obsidianWallProperties()).register();
    public static final BlockEntry<Block> CHISELED_OBSIDIAN_BRICKS = CCB_REGISTRATE.block("chiseled_obsidian_bricks", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("chiseled_obsidian_bricks")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> SMOOTH_OBSIDIAN_BRICKS = CCB_REGISTRATE.block("smooth_obsidian_bricks", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("smooth_obsidian_bricks")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<SlabBlock> SMOOTH_OBSIDIAN_BRICKS_SLAB = CCB_REGISTRATE.block("smooth_obsidian_bricks_slab", properties -> new SlabBlock(Properties.ofFullCopy(SMOOTH_OBSIDIAN_BRICKS.get()))).transform(ObsidianRegistration.obsidianAlikeSlabs("smooth_obsidian_bricks_slab", "smooth_obsidian_bricks")).transform(ObsidianRegistration.obsidianSlabProperties()).register();
    public static final BlockEntry<StairBlock> SMOOTH_OBSIDIAN_BRICKS_STAIRS = CCB_REGISTRATE.block("smooth_obsidian_bricks_stairs", properties -> new StairBlock(SMOOTH_OBSIDIAN_BRICKS.get().defaultBlockState(), Properties.ofFullCopy(SMOOTH_OBSIDIAN_BRICKS.get()))).transform(ObsidianRegistration.obsidianAlikeStairs("smooth_obsidian_bricks")).transform(ObsidianRegistration.obsidianStairsProperties()).register();
    public static final BlockEntry<WallBlock> SMOOTH_OBSIDIAN_BRICKS_WALL = CCB_REGISTRATE.block("smooth_obsidian_bricks_wall", properties -> new WallBlock(Properties.ofFullCopy(SMOOTH_OBSIDIAN_BRICKS.get()).forceSolidOn())).transform(ObsidianRegistration.obsidianAlikeWall("smooth_obsidian_bricks")).transform(ObsidianRegistration.obsidianWallProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_CONCAVE = CCB_REGISTRATE.block("obsidian_bricks_concave", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_concave")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_CONVEX = CCB_REGISTRATE.block("obsidian_bricks_convex", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_convex")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_A = CCB_REGISTRATE.block("obsidian_bricks_a", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_a_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_B = CCB_REGISTRATE.block("obsidian_bricks_b", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_b_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_C = CCB_REGISTRATE.block("obsidian_bricks_c", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_c_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_D = CCB_REGISTRATE.block("obsidian_bricks_d", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_d_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_E = CCB_REGISTRATE.block("obsidian_bricks_e", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_e_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_F = CCB_REGISTRATE.block("obsidian_bricks_f", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_f_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_G = CCB_REGISTRATE.block("obsidian_bricks_g", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_g_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_H = CCB_REGISTRATE.block("obsidian_bricks_h", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_h_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_I = CCB_REGISTRATE.block("obsidian_bricks_i", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_i_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_J = CCB_REGISTRATE.block("obsidian_bricks_j", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_j_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_K = CCB_REGISTRATE.block("obsidian_bricks_k", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_k_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_L = CCB_REGISTRATE.block("obsidian_bricks_l", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_l_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_M = CCB_REGISTRATE.block("obsidian_bricks_m", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_m_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_N = CCB_REGISTRATE.block("obsidian_bricks_n", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_n_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_O = CCB_REGISTRATE.block("obsidian_bricks_o", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_o_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_P = CCB_REGISTRATE.block("obsidian_bricks_p", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_p_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_Q = CCB_REGISTRATE.block("obsidian_bricks_q", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_q_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_R = CCB_REGISTRATE.block("obsidian_bricks_r", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_r_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_S = CCB_REGISTRATE.block("obsidian_bricks_s", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_s_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_T = CCB_REGISTRATE.block("obsidian_bricks_t", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_t_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_U = CCB_REGISTRATE.block("obsidian_bricks_u", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_u_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_V = CCB_REGISTRATE.block("obsidian_bricks_v", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_v_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_W = CCB_REGISTRATE.block("obsidian_bricks_w", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_w_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_X = CCB_REGISTRATE.block("obsidian_bricks_x", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_x_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_Y = CCB_REGISTRATE.block("obsidian_bricks_y", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_y_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();
    public static final BlockEntry<Block> OBSIDIAN_BRICKS_Z = CCB_REGISTRATE.block("obsidian_bricks_z", Block::new).transform(ObsidianRegistration.obsidianAlikeBlocks("obsidian_bricks_z_letter")).transform(ObsidianRegistration.obsidianBlockProperties()).register();

    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS = CCB_REGISTRATE.block("crying_obsidian_bricks", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks")).transform(ObsidianRegistration.cryingObsidianBlockProperties()).register();
    public static final BlockEntry<SlabBlock> CRYING_OBSIDIAN_BRICKS_SLAB = CCB_REGISTRATE.block("crying_obsidian_bricks_slab", properties -> new SlabBlock(Properties.ofFullCopy(CRYING_OBSIDIAN_BRICKS.get()))).transform(ObsidianRegistration.obsidianAlikeSlabs("crying_obsidian_bricks")).transform(ObsidianRegistration.obsidianSlabProperties()).register();
    public static final BlockEntry<StairBlock> CRYING_OBSIDIAN_BRICKS_STAIRS = CCB_REGISTRATE.block("crying_obsidian_bricks_stairs", properties -> new StairBlock(CRYING_OBSIDIAN_BRICKS.get().defaultBlockState(), Properties.ofFullCopy(CRYING_OBSIDIAN_BRICKS.get()))).transform(ObsidianRegistration.obsidianAlikeStairs("crying_obsidian_bricks")).transform(ObsidianRegistration.obsidianStairsProperties()).register();
    public static final BlockEntry<WallBlock> CRYING_OBSIDIAN_BRICKS_WALL = CCB_REGISTRATE.block("crying_obsidian_bricks_wall", properties -> new WallBlock(Properties.ofFullCopy(CRYING_OBSIDIAN_BRICKS.get()).forceSolidOn())).transform(ObsidianRegistration.obsidianAlikeWall("crying_obsidian_bricks")).transform(ObsidianRegistration.obsidianWallProperties()).register();
    public static final BlockEntry<Block> CHISELED_CRYING_OBSIDIAN_BRICKS = CCB_REGISTRATE.block("chiseled_crying_obsidian_bricks", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("chiseled_crying_obsidian_bricks")).transform(ObsidianRegistration.cryingObsidianBlockProperties()).register();
    public static final BlockEntry<Block> SMOOTH_CRYING_OBSIDIAN_BRICKS = CCB_REGISTRATE.block("smooth_crying_obsidian_bricks", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("smooth_crying_obsidian_bricks")).transform(ObsidianRegistration.cryingObsidianBlockProperties()).register();
    public static final BlockEntry<SlabBlock> SMOOTH_CRYING_OBSIDIAN_BRICKS_SLAB = CCB_REGISTRATE.block("smooth_crying_obsidian_bricks_slab", properties -> new SlabBlock(Properties.ofFullCopy(SMOOTH_CRYING_OBSIDIAN_BRICKS.get()))).transform(ObsidianRegistration.obsidianAlikeSlabs("smooth_crying_obsidian_bricks_slab", "smooth_crying_obsidian_bricks")).transform(ObsidianRegistration.obsidianSlabProperties()).register();
    public static final BlockEntry<StairBlock> SMOOTH_CRYING_OBSIDIAN_BRICKS_STAIRS = CCB_REGISTRATE.block("smooth_crying_obsidian_bricks_stairs", properties -> new StairBlock(SMOOTH_CRYING_OBSIDIAN_BRICKS.get().defaultBlockState(), Properties.ofFullCopy(SMOOTH_CRYING_OBSIDIAN_BRICKS.get()))).transform(ObsidianRegistration.obsidianAlikeStairs("smooth_crying_obsidian_bricks")).transform(ObsidianRegistration.obsidianStairsProperties()).register();
    public static final BlockEntry<WallBlock> SMOOTH_CRYING_OBSIDIAN_BRICKS_WALL = CCB_REGISTRATE.block("smooth_crying_obsidian_bricks_wall", properties -> new WallBlock(Properties.ofFullCopy(SMOOTH_CRYING_OBSIDIAN_BRICKS.get()).forceSolidOn())).transform(ObsidianRegistration.obsidianAlikeWall("smooth_crying_obsidian_bricks")).transform(ObsidianRegistration.obsidianWallProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_CONCAVE = CCB_REGISTRATE.block("crying_obsidian_bricks_concave", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_concave")).transform(ObsidianRegistration.cryingObsidianBlockProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_CONVEX = CCB_REGISTRATE.block("crying_obsidian_bricks_convex", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_convex")).transform(ObsidianRegistration.cryingObsidianBlockProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_A = CCB_REGISTRATE.block("crying_obsidian_bricks_a", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_a_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_B = CCB_REGISTRATE.block("crying_obsidian_bricks_b", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_b_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_C = CCB_REGISTRATE.block("crying_obsidian_bricks_c", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_c_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_D = CCB_REGISTRATE.block("crying_obsidian_bricks_d", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_d_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_E = CCB_REGISTRATE.block("crying_obsidian_bricks_e", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_e_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_F = CCB_REGISTRATE.block("crying_obsidian_bricks_f", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_f_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_G = CCB_REGISTRATE.block("crying_obsidian_bricks_g", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_g_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_H = CCB_REGISTRATE.block("crying_obsidian_bricks_h", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_h_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_I = CCB_REGISTRATE.block("crying_obsidian_bricks_i", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_i_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_J = CCB_REGISTRATE.block("crying_obsidian_bricks_j", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_j_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_K = CCB_REGISTRATE.block("crying_obsidian_bricks_k", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_k_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_L = CCB_REGISTRATE.block("crying_obsidian_bricks_l", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_l_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_M = CCB_REGISTRATE.block("crying_obsidian_bricks_m", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_m_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_N = CCB_REGISTRATE.block("crying_obsidian_bricks_n", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_n_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_O = CCB_REGISTRATE.block("crying_obsidian_bricks_o", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_o_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_P = CCB_REGISTRATE.block("crying_obsidian_bricks_p", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_p_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_Q = CCB_REGISTRATE.block("crying_obsidian_bricks_q", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_q_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_R = CCB_REGISTRATE.block("crying_obsidian_bricks_r", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_r_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_S = CCB_REGISTRATE.block("crying_obsidian_bricks_s", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_s_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_T = CCB_REGISTRATE.block("crying_obsidian_bricks_t", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_t_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_U = CCB_REGISTRATE.block("crying_obsidian_bricks_u", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_u_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_V = CCB_REGISTRATE.block("crying_obsidian_bricks_v", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_v_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_W = CCB_REGISTRATE.block("crying_obsidian_bricks_w", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_w_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_X = CCB_REGISTRATE.block("crying_obsidian_bricks_x", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_x_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_Y = CCB_REGISTRATE.block("crying_obsidian_bricks_y", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_y_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();
    public static final BlockEntry<Block> CRYING_OBSIDIAN_BRICKS_Z = CCB_REGISTRATE.block("crying_obsidian_bricks_z", Block::new).transform(ObsidianRegistration.cryingObsidianAlikeBlocks("crying_obsidian_bricks_z_letter")).transform(ObsidianRegistration.cryingObsidianLetterProperties()).register();

    static {
        CCBCreativeSectionTracker.set(CCB_REGISTRATE, CCBCreativeTabSection.CANISTERS);
    }

    public static final BlockEntry<PneumaticEngineBlock> PNEUMATIC_ENGINE_BLOCK = CCB_REGISTRATE.block("pneumatic_engine", PneumaticEngineBlock::new).transform(AirtightMachineRegistration.pneumaticEngine()).transform(AirtightMachineRegistration.pneumaticEngineProperties()).register();

    public static void register() {
    }
}
