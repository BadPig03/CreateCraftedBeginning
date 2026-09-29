package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.ModelGen;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlockItem;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightMeterCTBehaviour;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeCTBehaviour;
import net.ty.createcraftedbeginning.content.airtights.airtightvalve.AirtightValveBlock;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentCTBehaviour;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlock;
import net.ty.createcraftedbeginning.foundation.block.CCBSharedProperties;
import net.ty.createcraftedbeginning.gas.network.DirectionalGasPipe;
import net.ty.createcraftedbeginning.gas.network.DirectionalGasPipe.DirectionalFacing;
import net.ty.createcraftedbeginning.registry.CCBDisplaySources;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPipeRegistration {
    private GasPipeRegistration() {
    }

    @Contract(pure = true)
    public static <B extends AirVentBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airVent() {
        return builder -> builder.blockstate((context, provider) -> provider.simpleBlock(context.getEntry(), provider.models().getExistingFile(provider.modLoc("block/air_vent/block")))).onRegister(CreateRegistrate.connectedTextures(AirVentCTBehaviour::new)).item().build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightPipe() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
            Axis axis = state.getValue(AirtightPipeBlock.AXIS);
            int rotationX = axis == Axis.Y ? 0 : 90;
            int rotationY = axis == Axis.X ? 90 : 0;
            String modelPath = state.getValue(AirtightPipeBlock.CASED) ? "block/airtight_pipe/casing" : "block/airtight_pipe/pipe";
            return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc(modelPath))).uvLock(false).rotationX(rotationX).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).onRegister(CreateRegistrate.connectedTextures(AirtightPipeCTBehaviour::new)).item().properties(Properties::fireResistant).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightMeter(String modelName) {
        return builder -> builder.blockstate((context, provider) -> {
            BlockModelProvider models = provider.models();
            ModelFile horizontalModel = models.getExistingFile(provider.modLoc("block/airtight_meters/" + modelName));
            ModelFile verticalModel = models.getExistingFile(provider.modLoc("block/airtight_meters/" + modelName + "_vertical"));
            provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
                Axis axis = state.getValue(BlockStateProperties.AXIS);
                ModelFile model = axis == Axis.Y ? verticalModel : horizontalModel;
                int rotationY = axis == Axis.Z ? 270 : 0;
                return ConfiguredModel.builder().modelFile(model).uvLock(false).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED);
        }).onRegister(CreateRegistrate.connectedTextures(AirtightMeterCTBehaviour::new)).item().properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(itemBuilder -> itemBuilder.model(AssetLookup.customGenericItemModel("airtight_meters", modelName + "_item"))).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightEncasedPipe() {
        return builder -> builder.blockstate((context, provider) -> {
            BlockModelProvider models = provider.models();
            MultiPartBlockStateBuilder multipart = provider.getMultipartBuilder(context.getEntry());
            multipart.part().modelFile(AssetLookup.partialBaseModel(context, provider)).addModel().end();
            for (Direction direction : Iterate.directions) {
                multipart.part().modelFile(models.getExistingFile(provider.modLoc("block/airtight_encased_pipe/" + direction.getSerializedName()))).addModel().condition(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction), false).end();
            }
        }).item(AirtightEncasedPipeBlockItem::new).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(ModelGen.customItemModel("airtight_encased_pipe", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightCheckValve() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
            Axis axis = state.getValue(AirtightCheckValveBlock.AXIS);
            boolean isInverted = state.getValue(AirtightCheckValveBlock.INVERTED);
            String modelPath = isInverted ? "block/airtight_check_valve/block_inverted" : "block/airtight_check_valve/block";
            ModelFile model = provider.models().getExistingFile(provider.modLoc(modelPath));
            if (axis == Axis.Y) {
                DirectionalFacing facing = state.getValue(DirectionalGasPipe.DIRECTIONAL_FACING);
                int rotationY = facing == DirectionalFacing.EAST || facing == DirectionalFacing.WEST ? 90 : 0;
                return ConfiguredModel.builder().modelFile(model).uvLock(false).rotationY(rotationY).build();
            }

            int rotationY = axis == Axis.X ? 90 : 180;
            return ConfiguredModel.builder().modelFile(model).uvLock(false).rotationX(90).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).item().properties(Properties::fireResistant).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> smartAirtightPipe() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
            Axis axis = state.getValue(SmartAirtightPipeBlock.AXIS);
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/smart_airtight_pipe/block"));
            if (axis == Axis.Y) {
                DirectionalFacing facing = state.getValue(DirectionalGasPipe.DIRECTIONAL_FACING);
                int rotationY = BlockModelRotation.horizontal(DirectionalFacing.getDirection(facing));
                return ConfiguredModel.builder().modelFile(model).uvLock(false).rotationY(rotationY).build();
            }

            int rotationY = axis == Axis.X ? 90 : 180;
            return ConfiguredModel.builder().modelFile(model).uvLock(false).rotationX(90).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).item().properties(Properties::fireResistant).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightPump() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
            Direction direction = state.getValue(BlockStateProperties.FACING);
            int rotationX = switch (direction) {
                case DOWN -> 180;
                case UP -> 0;
                default -> 90;
            };
            int rotationY = direction.getAxis().isVertical() ? 0 : ((int) direction.toYRot() + 180) % 360;

            return ConfiguredModel.builder().modelFile(AssetLookup.partialBaseModel(context, provider)).rotationX(rotationX).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).item().properties(Properties::fireResistant).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightRegulatorPump() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
            Axis gasAxis = state.getValue(BlockStateProperties.FACING).getAxis();
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/airtight_regulator_pump/block"));
            if (gasAxis == Axis.Y) {
                DirectionalFacing facing = state.getValue(DirectionalGasPipe.DIRECTIONAL_FACING);
                int rotationY = BlockModelRotation.horizontal(DirectionalFacing.getDirection(facing));
                return ConfiguredModel.builder().modelFile(model).uvLock(false).rotationY(rotationY).build();
            }

            int rotationY = gasAxis == Axis.X ? 90 : 180;
            return ConfiguredModel.builder().modelFile(model).uvLock(false).rotationX(90).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).item().properties(Properties::fireResistant).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightValve() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
            Axis gasAxis = state.getValue(AirtightValveBlock.AXIS);
            String stateName = state.getValue(AirtightValveBlock.OPEN) ? "open" : "closed";
            String modelPath = "block/airtight_valve/block_" + stateName;
            ModelFile model = provider.models().getExistingFile(provider.modLoc(modelPath));
            if (gasAxis == Axis.Y) {
                DirectionalFacing facing = state.getValue(DirectionalGasPipe.DIRECTIONAL_FACING);
                int rotationY = BlockModelRotation.horizontal(DirectionalFacing.getDirection(facing));
                return ConfiguredModel.builder().modelFile(model).uvLock(false).rotationY(rotationY).build();
            }

            int rotationY = gasAxis == Axis.X ? 90 : 180;
            return ConfiguredModel.builder().modelFile(model).uvLock(false).rotationX(90).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).item().properties(Properties::fireResistant).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airVentProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::softMetal).tag(BlockTags.MINEABLE_WITH_PICKAXE).properties(properties -> properties.mapColor(MapColor.DEEPSLATE).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().dynamicShape());
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightFlowmeterProperties() {
        return builder -> AirtightBlockProperties.applyComponent(builder).transform(DisplaySource.displaySource(CCBDisplaySources.AIRTIGHT_FLOWMETER_FLOW_RATE));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightManometerProperties() {
        return builder -> AirtightBlockProperties.applyComponent(builder).transform(DisplaySource.displaySource(CCBDisplaySources.AIRTIGHT_MANOMETER_MAX_PRESSURE)).transform(DisplaySource.displaySource(CCBDisplaySources.AIRTIGHT_MANOMETER_PRESSURE_DIFFERENCE));
    }
}
