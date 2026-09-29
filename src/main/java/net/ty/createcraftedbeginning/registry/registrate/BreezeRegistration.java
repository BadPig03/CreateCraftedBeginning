package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberConductor.BreezeChamber;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberMovementBehaviour;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockItem;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerConductor;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerMovementBehaviour;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.EmptyBreezeCoolerBlock;
import net.ty.createcraftedbeginning.foundation.block.CCBSharedProperties;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BreezeRegistration {
    private BreezeRegistration() {
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> emptyBreezeCooler() {
        return builder -> builder.blockstate((context, provider) -> {
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/breeze_cooler/block"));
            provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(EmptyBreezeCoolerBlock.FACING);
                int rotationY = BlockModelRotation.horizontal(facing);
                return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED);
        }).item(BreezeCoolerBlockItem::new).model(AssetLookup.customBlockItemModel("breeze_cooler", "block")).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> breezeCooler() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
            String modelPath = "block/breeze_cooler/block";
            ModelFile model = provider.models().getExistingFile(provider.modLoc(modelPath));
            Direction facing = state.getValue(BreezeCoolerBlock.FACING);
            int rotationY = BlockModelRotation.horizontal(facing);
            return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).item().model(AssetLookup.customBlockItemModel("breeze_cooler", "block_with_breeze")).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> breezeChamber() {
        return builder -> builder.blockstate((context, provider) -> {
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/breeze_chamber/block"));
            provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(BreezeChamberBlock.FACING);
                int rotationY = BlockModelRotation.horizontal(facing);
                return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED);
        }).item().properties(Properties::fireResistant).model(AssetLookup.customBlockItemModel("breeze_chamber", "item")).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> breezeProperties() {
        return BreezeRegistration::applyBreeze;
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> breezeCoolerProperties() {
        return builder -> applyBreeze(builder).onRegister(MovementBehaviour.movementBehaviour(new BreezeCoolerMovementBehaviour())).onRegister(MovingInteractionBehaviour.interactionBehaviour(new BreezeCoolerConductor.BreezeChamber()));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> breezeChamberProperties() {
        return builder -> AirtightBlockProperties.applyComponent(builder).onRegister(MovementBehaviour.movementBehaviour(new BreezeChamberMovementBehaviour())).onRegister(MovingInteractionBehaviour.interactionBehaviour(new BreezeChamber()));
    }

    private static <B extends Block> BlockBuilder<B, CreateRegistrate> applyBreeze(BlockBuilder<B, CreateRegistrate> builder) {
        return builder.initialProperties(CCBSharedProperties::hardMetal).tag(BlockTags.MINEABLE_WITH_PICKAXE).properties(properties -> properties.mapColor(MapColor.COLOR_BLUE).noOcclusion());
    }
}
