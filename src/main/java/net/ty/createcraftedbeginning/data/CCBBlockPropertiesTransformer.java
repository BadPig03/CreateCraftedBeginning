package net.ty.createcraftedbeginning.data;

import com.simibubi.create.AllDisplaySources;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.ty.createcraftedbeginning.config.CCBStress;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankMovementBehavior;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankMovementBehavior;
import net.ty.createcraftedbeginning.content.airtights.gas.mounted.MountedGasStorageType;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceMovement;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineUtils;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberConductor.BreezeChamber;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberMovementBehaviour;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerConductor;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerMovementBehaviour;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBBlockPropertiesTransformer {
    private CCBBlockPropertiesTransformer() {
    }

    private static <B extends Block> BlockBuilder<B, CCBRegistrate> pickaxeOnly(BlockBuilder<B, CCBRegistrate> builder) {
        return builder.tag(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    private static <B extends Block> BlockBuilder<B, CCBRegistrate> axeOnly(BlockBuilder<B, CCBRegistrate> builder) {
        return builder.tag(BlockTags.MINEABLE_WITH_AXE);
    }

    private static <B extends Block> BlockBuilder<B, CCBRegistrate> axeOrPickaxe(BlockBuilder<B, CCBRegistrate> builder) {
        return builder.tag(BlockTags.MINEABLE_WITH_AXE).tag(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    private static <B extends Block> BlockBuilder<B, CCBRegistrate> applyAirtightComponent(BlockBuilder<B, CCBRegistrate> builder) {
        return pickaxeOnly(builder.initialProperties(CCBSharedProperties::airtightMetal)).properties(properties -> properties.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).requiresCorrectToolForDrops().noOcclusion()).tag(CCBBlockTags.AIRTIGHT_COMPONENTS.tag);
    }

    private static <B extends Block> BlockBuilder<B, CCBRegistrate> applyAirtightMetal(BlockBuilder<B, CCBRegistrate> builder) {
        return pickaxeOnly(builder.initialProperties(CCBSharedProperties::airtightMetal)).properties(properties -> properties.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).requiresCorrectToolForDrops());
    }

    private static <B extends Block> BlockBuilder<B, CCBRegistrate> applyAirtightStructural(BlockBuilder<B, CCBRegistrate> builder) {
        return pickaxeOnly(builder.initialProperties(CCBSharedProperties::airtightMetal)).properties(properties -> properties.mapColor(MapColor.METAL).sound(SoundType.EMPTY).requiresCorrectToolForDrops().noOcclusion()).tag(CCBBlockTags.AIRTIGHT_COMPONENTS.tag);
    }

    private static <B extends Block> BlockBuilder<B, CCBRegistrate> applyBreeze(BlockBuilder<B, CCBRegistrate> builder) {
        return pickaxeOnly(builder.initialProperties(CCBSharedProperties::hardMetal)).properties(properties -> properties.mapColor(MapColor.COLOR_BLUE).noOcclusion());
    }

    private static <B extends Block> BlockBuilder<B, CCBRegistrate> applyEndComponent(BlockBuilder<B, CCBRegistrate> builder) {
        return pickaxeOnly(builder.initialProperties(CCBSharedProperties::obsidian)).properties(properties -> properties.mapColor(MapColor.COLOR_GREEN).noOcclusion()).tag(CCBBlockTags.END_COMPONENTS.tag);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightComponent() {
        return CCBBlockPropertiesTransformer::applyAirtightComponent;
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightComponentWithImpact(double impact) {
        return builder -> applyAirtightComponent(builder).transform(CCBStress.setImpact(impact));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightComponentWithCapacity(double capacity) {
        return builder -> applyAirtightComponent(builder).transform(CCBStress.setCapacity(capacity));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightTank() {
        return builder -> applyAirtightComponent(builder).transform(MountedGasStorageType.mountedGasStorage(CCBMountedStorage.AIRTIGHT_TANK)).onRegister(MovementBehaviour.movementBehaviour(new AirtightTankMovementBehavior()));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> horizontalAirtightTank() {
        return builder -> applyAirtightComponent(builder).transform(MountedGasStorageType.mountedGasStorage(CCBMountedStorage.HORIZONTAL_AIRTIGHT_TANK)).onRegister(MovementBehaviour.movementBehaviour(new AirtightTankMovementBehavior()));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> creativeAirtightTank() {
        return builder -> applyAirtightComponent(builder).transform(MountedGasStorageType.mountedGasStorage(CCBMountedStorage.CREATIVE_AIRTIGHT_TANK)).onRegister(MovementBehaviour.movementBehaviour(new CreativeAirtightTankMovementBehavior()));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> portableGasInterface() {
        return builder -> applyAirtightComponent(builder).onRegister(MovementBehaviour.movementBehaviour(new PortableGasInterfaceMovement()));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> breeze() {
        return CCBBlockPropertiesTransformer::applyBreeze;
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> breezeCooler() {
        return builder -> applyBreeze(builder).onRegister(MovementBehaviour.movementBehaviour(new BreezeCoolerMovementBehaviour())).onRegister(MovingInteractionBehaviour.interactionBehaviour(new BreezeCoolerConductor.BreezeChamber()));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> breezeChamber() {
        return builder -> applyAirtightComponent(builder).onRegister(MovementBehaviour.movementBehaviour(new BreezeChamberMovementBehaviour())).onRegister(MovingInteractionBehaviour.interactionBehaviour(new BreezeChamber()));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> boilerSteamOutlet() {
        return builder -> pickaxeOnly(builder.initialProperties(CCBSharedProperties::copperMetal)).properties(Properties::noOcclusion).tag(CCBBlockTags.AIRTIGHT_COMPONENTS.tag);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightStructural() {
        return CCBBlockPropertiesTransformer::applyAirtightStructural;
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightStructuralWithImpact(double impact) {
        return builder -> applyAirtightStructural(builder).transform(CCBStress.setImpact(impact));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightRedstoneComponent() {
        return builder -> applyAirtightComponent(builder).properties(properties -> properties.isRedstoneConductor((state, level, pos) -> false));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> gasFactoryGauge() {
        return builder -> applyAirtightComponent(builder).properties(properties -> properties.forceSolidOn().noOcclusion()).transform(DisplaySource.displaySource(AllDisplaySources.GAUGE_STATUS));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightMetal() {
        return CCBBlockPropertiesTransformer::applyAirtightMetal;
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> photoStressBearing() {
        return builder -> pickaxeOnly(builder.initialProperties(CCBSharedProperties::stone)).transform(CCBStress.setCapacity(8)).properties(properties -> properties.mapColor(MapColor.COLOR_PURPLE).noOcclusion());
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> endCasing() {
        return builder -> applyEndComponent(builder).tag(AllBlockTags.CASING.tag);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> endComponentWithImpact(double impact) {
        return builder -> applyEndComponent(builder).transform(CCBStress.setImpact(impact));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> andesiteCrate() {
        return builder -> axeOrPickaxe(builder.initialProperties(CCBSharedProperties::stone)).properties(properties -> properties.mapColor(MapColor.PODZOL).sound(SoundType.WOOD)).tag(CCBBlockTags.CRATES.tag).transform(MountedItemStorageType.mountedItemStorage(CCBMountedStorage.ANDESITE_CRATE));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> brassCrate() {
        return builder -> axeOrPickaxe(builder.initialProperties(CCBSharedProperties::stone)).properties(properties -> properties.mapColor(MapColor.TERRACOTTA_BROWN).sound(SoundType.WOOD)).tag(CCBBlockTags.CRATES.tag).transform(MountedItemStorageType.mountedItemStorage(CCBMountedStorage.BRASS_CRATE));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> sturdyCrate() {
        return builder -> pickaxeOnly(builder.initialProperties(CCBSharedProperties::stone)).properties(properties -> properties.mapColor(MapColor.TERRACOTTA_CYAN).sound(SoundType.NETHERITE_BLOCK)).tag(CCBBlockTags.CRATES.tag).transform(MountedItemStorageType.mountedItemStorage(CCBMountedStorage.STURDY_CRATE));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> cardboardCrate() {
        return builder -> axeOnly(builder.initialProperties(CCBSharedProperties::cardboard)).properties(properties -> properties.mapColor(MapColor.COLOR_BROWN).sound(SoundType.CHISELED_BOOKSHELF).ignitedByLava()).tag(CCBBlockTags.CRATES.tag).transform(MountedItemStorageType.mountedItemStorage(CCBMountedStorage.CARDBOARD_CRATE));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airVent() {
        return builder -> pickaxeOnly(builder.initialProperties(CCBSharedProperties::softMetal)).properties(properties -> properties.mapColor(MapColor.DEEPSLATE).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().dynamicShape());
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> endAlloyBlock() {
        return builder -> pickaxeOnly(builder.initialProperties(CCBSharedProperties::obsidian)).properties(properties -> properties.mapColor(MapColor.COLOR_GREEN));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> powderedAmethystBlock() {
        return builder -> builder.tag(BlockTags.MINEABLE_WITH_SHOVEL).tag(BlockTags.CAMEL_SAND_STEP_SOUND_BLOCKS).properties(properties -> properties.mapColor(MapColor.COLOR_PURPLE).strength(0.5f).sound(SoundType.SAND));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> obsidianBlock() {
        return builder -> pickaxeOnly(builder.initialProperties(CCBSharedProperties::obsidian));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> cryingObsidianBlock() {
        return builder -> pickaxeOnly(builder.initialProperties(CCBSharedProperties::cryingObsidian));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> cryingObsidianLetter() {
        return builder -> pickaxeOnly(builder.initialProperties(CCBSharedProperties::cryingObsidian)).tag(BlockTags.ENCHANTMENT_POWER_PROVIDER);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> obsidianSlab() {
        return builder -> pickaxeOnly(builder.initialProperties(CCBSharedProperties::obsidian)).tag(BlockTags.SLABS);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> obsidianStairs() {
        return builder -> builder.initialProperties(CCBSharedProperties::obsidian).tag(BlockTags.STAIRS);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> obsidianWall() {
        return builder -> builder.initialProperties(CCBSharedProperties::obsidian).tag(BlockTags.WALLS);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> pneumaticEngine() {
        return builder -> pickaxeOnly(builder.initialProperties(CCBSharedProperties::copperMetal)).transform(CCBStress.setCapacity(6)).properties(properties -> properties.mapColor(MapColor.COLOR_ORANGE).noOcclusion());
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> teslaTurbine() {
        return airtightComponentWithCapacity(TeslaTurbineUtils.BASE_STRESS_CAPACITY);
    }
}
