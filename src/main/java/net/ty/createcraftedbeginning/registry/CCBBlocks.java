package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.config.CCBStress;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightintakeport.AirtightIntakePortBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlock;
import net.ty.createcraftedbeginning.content.airtights.checkvalve.CheckValveBlock;
import net.ty.createcraftedbeginning.content.airtights.condensatedrain.CondensateDrainBlock;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlock;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlock;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceBlock;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.EmptyBreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.cinderincinerationblower.CinderIncinerationBlowerBlock;
import net.ty.createcraftedbeginning.content.crates.andesitecrate.AndesiteCrateBlock;
import net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateBlock;
import net.ty.createcraftedbeginning.content.crates.cardboardcrate.CardboardCrateBlock;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlock;
import net.ty.createcraftedbeginning.content.phohostressbearing.PhotoStressBearingBlock;
import net.ty.createcraftedbeginning.content.pneumaticengine.PneumaticEngineBlock;
import net.ty.createcraftedbeginning.data.CCBBuilderTransformer;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import net.ty.createcraftedbeginning.data.CCBSharedProperties;

import static com.simibubi.create.api.contraption.storage.item.MountedItemStorageType.mountedItemStorage;
import static net.ty.createcraftedbeginning.data.CCBTagGen.axeOnly;
import static net.ty.createcraftedbeginning.data.CCBTagGen.axeOrPickaxe;
import static net.ty.createcraftedbeginning.data.CCBTagGen.mineableWithShovel;
import static net.ty.createcraftedbeginning.data.CCBTagGen.pickaxeOnly;

@SuppressWarnings("unused")
public class CCBBlocks {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate();

    public static final BlockEntry<AndesiteCrateBlock> ANDESITE_CRATE_BLOCK = CCB_REGISTRATE.block("andesite_crate", AndesiteCrateBlock::new).initialProperties(CCBSharedProperties::stone).transform(CCBBuilderTransformer.crate("andesite")).transform(axeOrPickaxe()).properties(p -> p.mapColor(MapColor.PODZOL).sound(SoundType.WOOD)).transform(mountedItemStorage(CCBMountedStorage.ANDESITE_CRATE)).register();
    public static final BlockEntry<BrassCrateBlock> BRASS_CRATE_BLOCK = CCB_REGISTRATE.block("brass_crate", BrassCrateBlock::new).initialProperties(CCBSharedProperties::stone).transform(CCBBuilderTransformer.crate("brass")).transform(axeOrPickaxe()).properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN).sound(SoundType.WOOD)).transform(mountedItemStorage(CCBMountedStorage.BRASS_CRATE)).register();
    public static final BlockEntry<SturdyCrateBlock> STURDY_CRATE_BLOCK = CCB_REGISTRATE.block("sturdy_crate", SturdyCrateBlock::new).initialProperties(CCBSharedProperties::stone).transform(CCBBuilderTransformer.uncontainable_crate()).transform(pickaxeOnly()).properties(p -> p.mapColor(MapColor.TERRACOTTA_CYAN).sound(SoundType.NETHERITE_BLOCK)).transform(mountedItemStorage(CCBMountedStorage.STURDY_CRATE)).register();
    public static final BlockEntry<CardboardCrateBlock> CARDBOARD_CRATE_BLOCK = CCB_REGISTRATE.block("cardboard_crate", CardboardCrateBlock::new).initialProperties(CCBSharedProperties::cardboard).transform(CCBBuilderTransformer.crate("cardboard")).transform(axeOnly()).properties(p -> p.mapColor(MapColor.COLOR_BROWN).sound(SoundType.CHISELED_BOOKSHELF).ignitedByLava()).transform(mountedItemStorage(CCBMountedStorage.CARDBOARD_CRATE)).register();

    public static final BlockEntry<PneumaticEngineBlock> PNEUMATIC_ENGINE_BLOCK = CCB_REGISTRATE.block("pneumatic_engine", PneumaticEngineBlock::new).initialProperties(CCBSharedProperties::copperMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.pneumatic_engine()).transform(CCBStress.setCapacity(6f)).properties(p -> p.mapColor(MapColor.COLOR_ORANGE).noOcclusion()).register();
    public static final BlockEntry<AirtightPipeBlock> AIRTIGHT_PIPE_BLOCK = CCB_REGISTRATE.block("airtight_pipe", AirtightPipeBlock::new).initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.airtight_pipe()).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).noOcclusion()).register();
    public static final BlockEntry<AirtightEncasedPipeBlock> AIRTIGHT_ENCASED_PIPE_BLOCK = CCB_REGISTRATE.block("airtight_encased_pipe", AirtightEncasedPipeBlock::new).initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.airtight_encased_pipe()).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).noOcclusion()).register();
    public static final BlockEntry<CheckValveBlock> CHECK_VALVE_BLOCK = CCB_REGISTRATE.block("check_valve", CheckValveBlock::new).initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.check_valve()).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).noOcclusion()).register();
    public static final BlockEntry<SmartAirtightPipeBlock> SMART_AIRTIGHT_PIPE_BLOCK = CCB_REGISTRATE.block("smart_airtight_pipe", SmartAirtightPipeBlock::new).initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.smart_airtight_pipe()).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).noOcclusion()).register();
    public static final BlockEntry<AirtightPumpBlock> AIRTIGHT_PUMP_BLOCK = CCB_REGISTRATE.block("airtight_pump", AirtightPumpBlock::new).initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).transform(CCBStress.setImpact(8f)).transform(CCBBuilderTransformer.airtight_pump()).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).noOcclusion()).register();
    public static final BlockEntry<AirtightTankBlock> AIRTIGHT_TANK_BLOCK = CCB_REGISTRATE.block("airtight_tank", AirtightTankBlock::new).initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.airtight_tank()).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).isRedstoneConductor((p1, p2, p3) -> true).noOcclusion()).register();
    public static final BlockEntry<CreativeAirtightTankBlock> CREATIVE_AIRTIGHT_TANK_BLOCK = CCB_REGISTRATE.block("creative_airtight_tank", CreativeAirtightTankBlock::new).initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.creative_airtight_tank()).properties(p -> p.mapColor(MapColor.COLOR_PURPLE).sound(SoundType.HEAVY_CORE).noOcclusion()).register();
    public static final BlockEntry<PortableGasInterfaceBlock> PORTABLE_GAS_INTERFACE = CCB_REGISTRATE.block("portable_gas_interface", PortableGasInterfaceBlock::new).initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.portable_gas_interface()).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).noOcclusion()).register();
    public static final BlockEntry<EmptyBreezeCoolerBlock> EMPTY_BREEZE_COOLER_BLOCK = CCB_REGISTRATE.block("empty_breeze_cooler", EmptyBreezeCoolerBlock::new).initialProperties(CCBSharedProperties::hardMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.empty_breeze_cooler()).properties(p -> p.mapColor(MapColor.COLOR_BLUE).noOcclusion()).register();
    public static final BlockEntry<BreezeCoolerBlock> BREEZE_COOLER_BLOCK = CCB_REGISTRATE.block("breeze_cooler", BreezeCoolerBlock::new).initialProperties(CCBSharedProperties::hardMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.breeze_cooler()).properties(p -> p.mapColor(MapColor.COLOR_BLUE).noOcclusion()).register();
    public static final BlockEntry<BreezeChamberBlock> BREEZE_CHAMBER_BLOCK = CCB_REGISTRATE.block("breeze_chamber", BreezeChamberBlock::new).initialProperties(CCBSharedProperties::hardMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.breeze_chamber()).properties(p -> p.mapColor(MapColor.COLOR_BLUE).noOcclusion()).register();
    public static final BlockEntry<AirtightEngineBlock> AIRTIGHT_ENGINE_BLOCK = CCB_REGISTRATE.block("airtight_engine", AirtightEngineBlock::new).initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.airtight_engine()).transform(CCBStress.setCapacity(4096f)).onRegister(BlockStressValues.setGeneratorSpeed(96, true)).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).noOcclusion()).register();
    public static final BlockEntry<CondensateDrainBlock> CONDENSATE_DRAIN_BLOCK = CCB_REGISTRATE.block("condensate_drain", CondensateDrainBlock::new).initialProperties(CCBSharedProperties::airtightMetal).transform(CCBBuilderTransformer.condensate_drain()).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).noOcclusion()).register();

    public static final BlockEntry<AirCompressorBlock> AIR_COMPRESSOR_BLOCK = CCB_REGISTRATE.block("air_compressor", AirCompressorBlock::new).initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.air_compressor()).transform(CCBStress.setImpact(16f)).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).noOcclusion()).register();
    public static final BlockEntry<GasInjectionChamberBlock> GAS_INJECTION_CHAMBER_BLOCK = CCB_REGISTRATE.block("gas_injection_chamber", GasInjectionChamberBlock::new).initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.gas_injection_chamber()).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).noOcclusion()).register();
    public static final BlockEntry<AirtightIntakePortBlock> AIRTIGHT_INTAKE_PORT_BLOCK = CCB_REGISTRATE.block("airtight_intake_port", AirtightIntakePortBlock::new).initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.airtight_intake_port()).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).noOcclusion()).register();
    public static final BlockEntry<Block> AIRTIGHT_SHEET_BLOCK = CCB_REGISTRATE.block("airtight_sheet_block", Block::new).initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.airtight_sheet_block()).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE)).register();

    public static final BlockEntry<Block> CINDER_ALLOY_BLOCK = CCB_REGISTRATE.block("cinder_alloy_block", Block::new).initialProperties(CCBSharedProperties::softMetal).transform(pickaxeOnly()).transform(CCBBuilderTransformer.simple_block("cinder_alloy_block")).properties(p -> p.mapColor(MapColor.COLOR_BROWN)).register();
    public static final BlockEntry<CasingBlock> CINDER_CASING_BLOCK = CCB_REGISTRATE.block("cinder_casing", CasingBlock::new).initialProperties(CCBSharedProperties::softMetal).transform(axeOrPickaxe()).transform(CCBBuilderTransformer.casing(() -> CCBSpriteShifts.CINDER_CASING)).properties(p -> p.mapColor(MapColor.COLOR_BROWN)).register();
    public static final BlockEntry<CinderIncinerationBlowerBlock> CINDER_INCINERATION_BLOWER_BLOCK = CCB_REGISTRATE.block("cinder_incineration_blower", CinderIncinerationBlowerBlock::new).initialProperties(CCBSharedProperties::softMetal).transform(CCBStress.setImpact(4f)).transform(pickaxeOnly()).transform(CCBBuilderTransformer.cinder_incineration_blower()).properties(p -> p.mapColor(MapColor.COLOR_YELLOW).noOcclusion()).register();
    public static final BlockEntry<ColoredFallingBlock> POWDERED_AMETHYST_BLOCK = CCB_REGISTRATE.block("powdered_amethyst_block", properties -> new ColoredFallingBlock(new ColorRGBA(0xFF8D6ACC), properties)).transform(mineableWithShovel()).tag(BlockTags.CAMEL_SAND_STEP_SOUND_BLOCKS).transform(CCBBuilderTransformer.simple_block("powdered_amethyst_block")).properties(p -> p.mapColor(MapColor.COLOR_PURPLE).strength(0.5f).sound(SoundType.SAND)).register();
    public static final BlockEntry<PhotoStressBearingBlock> PHOTO_STRESS_BEARING_BLOCK = CCB_REGISTRATE.block("photo-stress_bearing", PhotoStressBearingBlock::new).initialProperties(CCBSharedProperties::stone).transform(CCBStress.setCapacity(8f)).transform(pickaxeOnly()).transform(CCBBuilderTransformer.photo_stress_bearing()).properties(p -> p.mapColor(MapColor.COLOR_PURPLE).noOcclusion()).register();

    public static void register() {
    }
}
