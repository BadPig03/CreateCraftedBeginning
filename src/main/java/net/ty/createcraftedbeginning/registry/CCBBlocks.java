package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.api.stress.BlockStressValues;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.config.CCBStress;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlock.AirtightReactorKettleRenderProperties;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralBlock.AirtightReactorKettleStructuralRenderProperties;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralCogBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlock;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterBlock;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterBlock;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlock;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceBlock;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlock;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock.TeslaTurbineRenderProperties;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock.TeslaTurbineStructuralRenderProperties;
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
import net.ty.createcraftedbeginning.content.obsolete.phohostressbearing.PhotoStressBearingBlock;
import net.ty.createcraftedbeginning.content.obsolete.pneumaticengine.PneumaticEngineBlock;
import net.ty.createcraftedbeginning.data.CCBBuilderTransformer;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import net.ty.createcraftedbeginning.data.CCBSharedProperties;
import net.ty.createcraftedbeginning.data.CCBSpriteShifts;

import static com.simibubi.create.api.contraption.storage.item.MountedItemStorageType.mountedItemStorage;
import static net.ty.createcraftedbeginning.data.CCBBuilderTransformer.airtightPropertiesWithoutAirtightComponents;
import static net.ty.createcraftedbeginning.data.CCBBuilderTransformer.airtightPropertiesWithoutOcclusion;
import static net.ty.createcraftedbeginning.data.CCBBuilderTransformer.airtightStructural;
import static net.ty.createcraftedbeginning.data.CCBBuilderTransformer.axeOnly;
import static net.ty.createcraftedbeginning.data.CCBBuilderTransformer.axeOrPickaxe;
import static net.ty.createcraftedbeginning.data.CCBBuilderTransformer.breezes;
import static net.ty.createcraftedbeginning.data.CCBBuilderTransformer.minableWithShovel;
import static net.ty.createcraftedbeginning.data.CCBBuilderTransformer.pickaxeOnly;

@SuppressWarnings("unused")
public class CCBBlocks {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate().setCreativeTab(CCBCreativeTabs.BASE_CREATIVE_TAB);

    public static final BlockEntry<AndesiteCrateBlock> ANDESITE_CRATE_BLOCK = CCB_REGISTRATE.block("andesite_crate", AndesiteCrateBlock::new).initialProperties(CCBSharedProperties::stone).transform(CCBBuilderTransformer.crate("andesite")).transform(axeOrPickaxe()).properties(p -> p.mapColor(MapColor.PODZOL).sound(SoundType.WOOD)).transform(mountedItemStorage(CCBMountedStorage.ANDESITE_CRATE)).register();
    public static final BlockEntry<BrassCrateBlock> BRASS_CRATE_BLOCK = CCB_REGISTRATE.block("brass_crate", BrassCrateBlock::new).initialProperties(CCBSharedProperties::stone).transform(CCBBuilderTransformer.crate("brass")).transform(axeOrPickaxe()).properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN).sound(SoundType.WOOD)).transform(mountedItemStorage(CCBMountedStorage.BRASS_CRATE)).register();
    public static final BlockEntry<SturdyCrateBlock> STURDY_CRATE_BLOCK = CCB_REGISTRATE.block("sturdy_crate", SturdyCrateBlock::new).initialProperties(CCBSharedProperties::stone).transform(CCBBuilderTransformer.uncontainable_crate()).transform(pickaxeOnly()).properties(p -> p.mapColor(MapColor.TERRACOTTA_CYAN).sound(SoundType.NETHERITE_BLOCK)).transform(mountedItemStorage(CCBMountedStorage.STURDY_CRATE)).register();
    public static final BlockEntry<CardboardCrateBlock> CARDBOARD_CRATE_BLOCK = CCB_REGISTRATE.block("cardboard_crate", CardboardCrateBlock::new).initialProperties(CCBSharedProperties::cardboard).transform(CCBBuilderTransformer.crate("cardboard")).transform(axeOnly()).properties(p -> p.mapColor(MapColor.COLOR_BROWN).sound(SoundType.CHISELED_BOOKSHELF).ignitedByLava()).transform(mountedItemStorage(CCBMountedStorage.CARDBOARD_CRATE)).register();

    public static final BlockEntry<ColoredFallingBlock> POWDERED_AMETHYST_BLOCK = CCB_REGISTRATE.block("powdered_amethyst_block", properties -> new ColoredFallingBlock(new ColorRGBA(0xFF8D6ACC), properties)).transform(minableWithShovel()).tag(BlockTags.CAMEL_SAND_STEP_SOUND_BLOCKS).transform(CCBBuilderTransformer.simple_block("powdered_amethyst_block", p -> p)).properties(p -> p.mapColor(MapColor.COLOR_PURPLE).strength(0.5f).sound(SoundType.SAND)).register();

    public static final BlockEntry<Block> AIRTIGHT_SHEET_BLOCK = CCB_REGISTRATE.block("airtight_sheet_block", Block::new).transform(CCBBuilderTransformer.airtight_sheet_block()).transform(airtightPropertiesWithoutAirtightComponents()).register();

    public static final BlockEntry<AirtightPipeBlock> AIRTIGHT_PIPE_BLOCK = CCB_REGISTRATE.block("airtight_pipe", AirtightPipeBlock::new).transform(CCBBuilderTransformer.airtight_pipe()).transform(airtightPropertiesWithoutOcclusion()).register();
    public static final BlockEntry<AirtightEncasedPipeBlock> AIRTIGHT_ENCASED_PIPE_BLOCK = CCB_REGISTRATE.block("airtight_encased_pipe", AirtightEncasedPipeBlock::new).transform(CCBBuilderTransformer.airtight_encased_pipe()).transform(airtightPropertiesWithoutOcclusion()).register();
    public static final BlockEntry<AirtightCheckValveBlock> AIRTIGHT_CHECK_VALVE_BLOCK = CCB_REGISTRATE.block("airtight_check_valve", AirtightCheckValveBlock::new).transform(CCBBuilderTransformer.airtight_check_valve()).transform(airtightPropertiesWithoutOcclusion()).register();
    public static final BlockEntry<SmartAirtightPipeBlock> SMART_AIRTIGHT_PIPE_BLOCK = CCB_REGISTRATE.block("smart_airtight_pipe", SmartAirtightPipeBlock::new).transform(CCBBuilderTransformer.smart_airtight_pipe()).transform(airtightPropertiesWithoutOcclusion()).register();
    public static final BlockEntry<AirtightPumpBlock> AIRTIGHT_PUMP_BLOCK = CCB_REGISTRATE.block("airtight_pump", AirtightPumpBlock::new).transform(CCBStress.setImpact(8.0f)).transform(CCBBuilderTransformer.airtight_pump()).transform(airtightPropertiesWithoutOcclusion()).register();

    public static final BlockEntry<AirtightTankBlock> AIRTIGHT_TANK_BLOCK = CCB_REGISTRATE.block("airtight_tank", AirtightTankBlock::new).transform(CCBBuilderTransformer.airtight_tank()).transform(airtightPropertiesWithoutOcclusion()).register();
    public static final BlockEntry<CreativeAirtightTankBlock> CREATIVE_AIRTIGHT_TANK_BLOCK = CCB_REGISTRATE.block("creative_airtight_tank", CreativeAirtightTankBlock::new).transform(CCBBuilderTransformer.creative_airtight_tank()).transform(airtightPropertiesWithoutOcclusion()).register();

    public static final BlockEntry<EmptyBreezeCoolerBlock> EMPTY_BREEZE_COOLER_BLOCK = CCB_REGISTRATE.block("empty_breeze_cooler", EmptyBreezeCoolerBlock::new).transform(CCBBuilderTransformer.empty_breeze_cooler()).transform(breezes()).register();
    public static final BlockEntry<BreezeCoolerBlock> BREEZE_COOLER_BLOCK = CCB_REGISTRATE.block("breeze_cooler", BreezeCoolerBlock::new).transform(CCBBuilderTransformer.breeze_cooler()).transform(breezes()).register();
    public static final BlockEntry<BreezeChamberBlock> BREEZE_CHAMBER_BLOCK = CCB_REGISTRATE.block("breeze_chamber", BreezeChamberBlock::new).transform(CCBBuilderTransformer.breeze_chamber()).transform(airtightPropertiesWithoutOcclusion()).register();

    public static final BlockEntry<AirtightEngineBlock> AIRTIGHT_ENGINE_BLOCK = CCB_REGISTRATE.block("airtight_engine", AirtightEngineBlock::new).transform(CCBBuilderTransformer.airtight_engine()).transform(CCBStress.setCapacity(1024)).onRegister(BlockStressValues.setGeneratorSpeed(64, true)).transform(airtightPropertiesWithoutOcclusion()).register();
    public static final BlockEntry<ResidueOutletBlock> RESIDUE_OUTLET_BLOCK = CCB_REGISTRATE.block("residue_outlet", ResidueOutletBlock::new).transform(CCBBuilderTransformer.residue_outlet()).transform(airtightPropertiesWithoutOcclusion()).register();

    public static final BlockEntry<AirCompressorBlock> AIR_COMPRESSOR_BLOCK = CCB_REGISTRATE.block("air_compressor", AirCompressorBlock::new).transform(CCBBuilderTransformer.air_compressor()).transform(CCBStress.setImpact(16)).transform(airtightPropertiesWithoutOcclusion()).register();
    public static final BlockEntry<TeslaTurbineBlock> TESLA_TURBINE_BLOCK = CCB_REGISTRATE.block("tesla_turbine", TeslaTurbineBlock::new).clientExtension(() -> TeslaTurbineRenderProperties::new).transform(CCBBuilderTransformer.tesla_turbine()).transform(CCBStress.setCapacity(4096)).onRegister(BlockStressValues.setGeneratorSpeed(256, true)).transform(airtightPropertiesWithoutOcclusion()).register();
    public static final BlockEntry<TeslaTurbineNozzleBlock> TESLA_TURBINE_NOZZLE_BLOCK = CCB_REGISTRATE.block("tesla_turbine_nozzle", TeslaTurbineNozzleBlock::new).transform(CCBBuilderTransformer.tesla_turbine_nozzle()).transform(airtightPropertiesWithoutOcclusion()).register();
    public static final BlockEntry<TeslaTurbineStructuralBlock> TESLA_TURBINE_STRUCTURAL_BLOCK = CCB_REGISTRATE.block("tesla_turbine_structural", TeslaTurbineStructuralBlock::new).clientExtension(() -> TeslaTurbineStructuralRenderProperties::new).transform(CCBBuilderTransformer.tesla_turbine_structural()).transform(airtightStructural()).register();

    public static final BlockEntry<AirtightReactorKettleBlock> AIRTIGHT_REACTOR_KETTLE_BLOCK = CCB_REGISTRATE.block("airtight_reactor_kettle", AirtightReactorKettleBlock::new).clientExtension(() -> AirtightReactorKettleRenderProperties::new).transform(CCBBuilderTransformer.airtight_reactor_kettle()).transform(airtightPropertiesWithoutOcclusion()).register();
    public static final BlockEntry<AirtightReactorKettleStructuralBlock> AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_BLOCK = CCB_REGISTRATE.block("airtight_reactor_kettle_structural", AirtightReactorKettleStructuralBlock::new).clientExtension(() -> AirtightReactorKettleStructuralRenderProperties::new).transform(CCBBuilderTransformer.airtight_reactor_kettle_structural()).lang("Airtight Reactor Kettle").transform(airtightStructural()).register();
    public static final BlockEntry<AirtightReactorKettleStructuralCogBlock> AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_COG_BLOCK = CCB_REGISTRATE.block("airtight_reactor_kettle_structural_cog", AirtightReactorKettleStructuralCogBlock::new).clientExtension(() -> AirtightReactorKettleStructuralRenderProperties::new).transform(CCBStress.setImpact(16)).transform(CCBBuilderTransformer.airtight_reactor_kettle_structural_cog()).lang("Airtight Reactor Kettle").transform(airtightStructural()).register();

    public static final BlockEntry<PortableGasInterfaceBlock> PORTABLE_GAS_INTERFACE_BLOCK = CCB_REGISTRATE.block("portable_gas_interface", PortableGasInterfaceBlock::new).transform(CCBBuilderTransformer.portable_gas_interface()).transform(airtightPropertiesWithoutOcclusion()).register();
    public static final BlockEntry<GasInjectionChamberBlock> GAS_INJECTION_CHAMBER_BLOCK = CCB_REGISTRATE.block("gas_injection_chamber", GasInjectionChamberBlock::new).transform(CCBBuilderTransformer.gas_injection_chamber()).transform(airtightPropertiesWithoutOcclusion()).register();
    public static final BlockEntry<AirtightHatchBlock> AIRTIGHT_HATCH_BLOCK = CCB_REGISTRATE.block("airtight_hatch", AirtightHatchBlock::new).transform(CCBBuilderTransformer.airtight_hatch()).transform(airtightPropertiesWithoutOcclusion()).register();
    public static final BlockEntry<GasCanisterBlock> GAS_CANISTER_BLOCK = CCB_REGISTRATE.block("gas_canister", GasCanisterBlock::new).transform(CCBBuilderTransformer.gas_canister()).transform(airtightPropertiesWithoutAirtightComponents()).register();
    public static final BlockEntry<CreativeGasCanisterBlock> CREATIVE_GAS_CANISTER_BLOCK = CCB_REGISTRATE.block("creative_gas_canister", CreativeGasCanisterBlock::new).transform(CCBBuilderTransformer.creative_gas_canister()).transform(airtightPropertiesWithoutAirtightComponents()).register();

    public static final BlockEntry<AirVentBlock> AIR_VENT_BLOCK = CCB_REGISTRATE.block("air_vent", AirVentBlock::new).transform(pickaxeOnly()).transform(CCBBuilderTransformer.air_vent()).register();

    public static final BlockEntry<Block> END_ALLOY_BLOCK = CCB_REGISTRATE.block("end_alloy_block", Block::new).initialProperties(CCBSharedProperties::obsidian).transform(pickaxeOnly()).transform(CCBBuilderTransformer.simple_block("end_alloy_block", p -> p.rarity(Rarity.UNCOMMON))).properties(p -> p.mapColor(MapColor.COLOR_GREEN)).register();
    public static final BlockEntry<EndCasingBlock> END_CASING_BLOCK = CCB_REGISTRATE.block("end_casing", EndCasingBlock::new).initialProperties(CCBSharedProperties::obsidian).transform(pickaxeOnly()).transform(CCBBuilderTransformer.casing(() -> CCBSpriteShifts.END_CASING, p -> p.rarity(Rarity.UNCOMMON))).properties(p -> p.mapColor(MapColor.COLOR_GREEN)).register();
    public static final BlockEntry<EndIncinerationBlowerBlock> END_INCINERATION_BLOWER_BLOCK = CCB_REGISTRATE.block("end_incineration_blower", EndIncinerationBlowerBlock::new).transform(pickaxeOnly()).transform(CCBStress.setImpact(4)).transform(CCBBuilderTransformer.end_incineration_blower()).register();
    public static final BlockEntry<EndIncinerationBlowerStructuralBlock> END_INCINERATION_BLOWER_STRUCTURAL_BLOCK = CCB_REGISTRATE.block("end_incineration_blower_structural", EndIncinerationBlowerStructuralBlock::new).transform(pickaxeOnly()).transform(CCBStress.setImpact(0)).transform(CCBBuilderTransformer.end_incineration_blower_structural()).register();

    public static final BlockEntry<PhotoStressBearingBlock> PHOTO_STRESS_BEARING_BLOCK = CCB_REGISTRATE.block("photo-stress_bearing", PhotoStressBearingBlock::new).initialProperties(CCBSharedProperties::stone).transform(CCBStress.setCapacity(8)).transform(pickaxeOnly()).transform(CCBBuilderTransformer.photo_stress_bearing()).properties(p -> p.mapColor(MapColor.COLOR_PURPLE).noOcclusion()).register();
    public static final BlockEntry<PneumaticEngineBlock> PNEUMATIC_ENGINE_BLOCK = CCB_REGISTRATE.block("pneumatic_engine", PneumaticEngineBlock::new).initialProperties(CCBSharedProperties::copperMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.pneumatic_engine()).transform(CCBStress.setCapacity(6)).properties(p -> p.mapColor(MapColor.COLOR_ORANGE).noOcclusion()).register();

    public static void register() {
    }
}
