package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankCTBehaviour;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankItem;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankMovementBehaviour;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.HorizontalAirtightTankBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.HorizontalAirtightTankCTBehaviour;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.HorizontalAirtightTankItem;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankCTBehaviour;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankItem;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankMovementBehaviour;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageType;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasStorageRegistration {
    private GasStorageRegistration() {
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightTank() {
        return builder -> builder.blockstate((context, provider) -> provider.simpleBlock(context.getEntry(), AssetLookup.standardModel(context, provider))).onRegister(CreateRegistrate.connectedTextures(AirtightTankCTBehaviour::new)).item(AirtightTankItem::new).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> horizontalAirtightTank() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.get()).forAllStates(state -> {
            int rotationY = state.getValue(HorizontalAirtightTankBlock.HORIZONTAL_AXIS) == Axis.X ? 90 : 0;
            return ConfiguredModel.builder().modelFile(AssetLookup.standardModel(context, provider)).rotationY(rotationY).build();
        })).onRegister(CreateRegistrate.connectedTextures(HorizontalAirtightTankCTBehaviour::new)).item(HorizontalAirtightTankItem::new).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> creativeAirtightTank() {
        return builder -> builder.blockstate((context, provider) -> provider.simpleBlock(context.getEntry(), AssetLookup.standardModel(context, provider))).onRegister(CreateRegistrate.connectedTextures(CreativeAirtightTankCTBehaviour::new)).item(CreativeAirtightTankItem::new).properties(properties -> properties.rarity(Rarity.EPIC).fireResistant()).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> gasCanister() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc("block/gas_canister"))).build(), BlockStateProperties.WATERLOGGED)).loot((loot, block) -> loot.add(block, LootTable.lootTable().withPool(LootPool.lootPool().when(ExplosionCondition.survivesExplosion()).setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(CCBItems.GAS_CANISTER.get()).apply(CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY).include(CCBDataComponents.CANISTER_CONTAINER_CONTENTS)))))).item().build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> creativeGasCanister() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc("block/creative_gas_canister"))).build(), BlockStateProperties.WATERLOGGED)).loot((loot, block) -> loot.add(block, LootTable.lootTable().withPool(LootPool.lootPool().when(ExplosionCondition.survivesExplosion()).setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(CCBItems.CREATIVE_GAS_CANISTER.get()).apply(CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY).include(CCBDataComponents.CANISTER_CONTAINER_CONTENTS)))))).item().build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightHatch() {
        return builder -> builder.blockstate((context, provider) -> provider.horizontalBlock(context.get(), state -> AssetLookup.partialBaseModel(context, provider, state.getValue(AirtightHatchBlock.CANISTER_TYPE).getSerializedName()))).item().properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(itemBuilder -> itemBuilder.model(AssetLookup.customBlockItemModel("_", "block_empty"))).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightSheetBlock() {
        return builder -> builder.blockstate((context, provider) -> provider.simpleBlock(context.get(), provider.models().cubeAll(context.getName(), provider.modLoc("block/airtight_sheet_block")))).item().properties(Properties::fireResistant).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightTankProperties() {
        return builder -> AirtightBlockProperties.applyComponent(builder).transform(MountedGasStorageType.mountedGasStorage(CCBMountedStorage.AIRTIGHT_TANK)).onRegister(MovementBehaviour.movementBehaviour(new AirtightTankMovementBehaviour()));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> horizontalAirtightTankProperties() {
        return builder -> AirtightBlockProperties.applyComponent(builder).transform(MountedGasStorageType.mountedGasStorage(CCBMountedStorage.HORIZONTAL_AIRTIGHT_TANK)).onRegister(MovementBehaviour.movementBehaviour(new AirtightTankMovementBehaviour()));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> creativeAirtightTankProperties() {
        return builder -> AirtightBlockProperties.applyComponent(builder).transform(MountedGasStorageType.mountedGasStorage(CCBMountedStorage.CREATIVE_AIRTIGHT_TANK)).onRegister(MovementBehaviour.movementBehaviour(new CreativeAirtightTankMovementBehaviour()));
    }
}
