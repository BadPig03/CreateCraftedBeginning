package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlockItem;
import net.ty.createcraftedbeginning.foundation.block.CCBSharedProperties;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CrateRegistration {
    private CrateRegistration() {
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> uncontainableCrate() {
        return builder -> builder.blockstate((context, provider) -> {
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/sturdy_crate/block"));
            provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                int rotationY = BlockModelRotation.horizontal(facing);
                return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
            });
        }).item(SturdyCrateBlockItem::new).properties(Properties::fireResistant).tag(CCBItemTags.CRATES.tag).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> crate(String type) {
        return builder -> builder.blockstate((context, provider) -> {
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/" + type + "_crate/block"));
            provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                int rotationY = BlockModelRotation.horizontal(facing);
                return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
            });
        }).item().tag(CCBItemTags.CRATES.tag).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> andesiteCrateProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::stone).tag(BlockTags.MINEABLE_WITH_AXE).tag(BlockTags.MINEABLE_WITH_PICKAXE).properties(properties -> properties.mapColor(MapColor.PODZOL).sound(SoundType.WOOD)).tag(CCBBlockTags.CRATES.tag).transform(MountedItemStorageType.mountedItemStorage(CCBMountedStorage.ANDESITE_CRATE));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> brassCrateProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::stone).tag(BlockTags.MINEABLE_WITH_AXE).tag(BlockTags.MINEABLE_WITH_PICKAXE).properties(properties -> properties.mapColor(MapColor.TERRACOTTA_BROWN).sound(SoundType.WOOD)).tag(CCBBlockTags.CRATES.tag).transform(MountedItemStorageType.mountedItemStorage(CCBMountedStorage.BRASS_CRATE));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> sturdyCrateProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::stone).tag(BlockTags.MINEABLE_WITH_PICKAXE).properties(properties -> properties.mapColor(MapColor.TERRACOTTA_CYAN).sound(SoundType.NETHERITE_BLOCK)).tag(CCBBlockTags.CRATES.tag).transform(MountedItemStorageType.mountedItemStorage(CCBMountedStorage.STURDY_CRATE));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> cardboardCrateProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::cardboard).tag(BlockTags.MINEABLE_WITH_AXE).properties(properties -> properties.mapColor(MapColor.COLOR_BROWN).sound(SoundType.CHISELED_BOOKSHELF).ignitedByLava()).tag(CCBBlockTags.CRATES.tag).transform(MountedItemStorageType.mountedItemStorage(CCBMountedStorage.CARDBOARD_CRATE));
    }
}
