package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.AllDisplaySources;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockItem;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.ModelGen;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasunpackager.GasUnpackagerBlock;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceMovement;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasLogisticsRegistration {
    private GasLogisticsRegistration() {
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> gasPackager() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStates(state -> {
            String suffix;
            if (state.getValue(PackagerBlock.LINKED)) {
                suffix = "linked";
            }
            else if (state.getValue(PackagerBlock.POWERED)) {
                suffix = "powered";
            }
            else {
                suffix = "";
            }

            Direction facing = state.getValue(PackagerBlock.FACING);
            boolean isVertical = facing.getAxis() == Axis.Y;
            ModelFile model = isVertical ? AssetLookup.partialBaseModel(context, provider, "vertical", suffix) : AssetLookup.partialBaseModel(context, provider, suffix);
            int rotationY = isVertical ? 0 : (int) facing.toYRot();
            return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
        })).item().model(AssetLookup::customItemModel).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> gasUnpackager() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStates(state -> {
            Direction facing = state.getValue(GasUnpackagerBlock.FACING);
            boolean isVertical = facing.getAxis() == Axis.Y;
            String modelName = isVertical ? "block_vertical" : "block";
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/gas_unpackager/" + modelName));
            int rotationY = isVertical ? 0 : (int) facing.toYRot();
            return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
        })).item().model(AssetLookup.customBlockItemModel("gas_unpackager", "item")).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> gasRepackager() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStates(state -> {
            String suffix = state.getValue(PackagerBlock.POWERED) ? "powered" : "";
            Direction facing = state.getValue(PackagerBlock.FACING);
            boolean isVertical = facing.getAxis() == Axis.Y;
            ModelFile model = isVertical ? AssetLookup.partialBaseModel(context, provider, "vertical", suffix) : AssetLookup.partialBaseModel(context, provider, suffix);
            int rotationY = isVertical ? 0 : (int) facing.toYRot();
            return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
        })).item().model(AssetLookup::customItemModel).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> gasFactoryGauge() {
        return builder -> builder.blockstate((context, provider) -> provider.horizontalFaceBlock(context.get(), AssetLookup.partialBaseModel(context, provider))).item(FactoryPanelBlockItem::new).model(AssetLookup::customItemModel).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> portableGasInterface() {
        return builder -> builder.blockstate((context, provider) -> provider.directionalBlock(context.get(), AssetLookup.partialBaseModel(context, provider))).item().properties(Properties::fireResistant).tag(AllItemTags.CONTRAPTION_CONTROLLED.tag).transform(ModelGen.customItemModel());
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> gasFactoryGaugeProperties() {
        return builder -> AirtightBlockProperties.applyComponent(builder).properties(properties -> properties.forceSolidOn().noOcclusion()).transform(DisplaySource.displaySource(AllDisplaySources.GAUGE_STATUS));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> portableGasInterfaceProperties() {
        return builder -> AirtightBlockProperties.applyComponent(builder).onRegister(MovementBehaviour.movementBehaviour(new PortableGasInterfaceMovement()));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightRedstoneComponentProperties() {
        return builder -> AirtightBlockProperties.applyComponent(builder).properties(properties -> properties.isRedstoneConductor((state, level, pos) -> false));
    }
}
