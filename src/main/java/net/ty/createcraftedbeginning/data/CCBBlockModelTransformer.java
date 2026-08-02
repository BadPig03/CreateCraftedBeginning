package net.ty.createcraftedbeginning.data;

import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockItem;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.processing.AssemblyOperatorBlockItem;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.ModelGen;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IDirectionalPipe;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IDirectionalPipe.DirectionalFacing;
import net.ty.createcraftedbeginning.client.blockextensions.AirtightForgingPressClientExtensions;
import net.ty.createcraftedbeginning.client.blockextensions.AirtightForgingPressStructuralClientExtensions;
import net.ty.createcraftedbeginning.client.blockextensions.AirtightReactorKettleClientExtensions;
import net.ty.createcraftedbeginning.client.blockextensions.AirtightReactorKettleStructuralClientExtensions;
import net.ty.createcraftedbeginning.client.blockextensions.TeslaTurbineClientExtensions;
import net.ty.createcraftedbeginning.client.blockextensions.TeslaTurbineStructuralClientExtensions;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlockItem;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockItem;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeAttachmentModel;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockItem;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralCogBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankCTBehavior;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankItem;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.HorizontalAirtightTankBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.HorizontalAirtightTankCTBehavior;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.HorizontalAirtightTankItem;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentCTBehaviour;
import net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet.BoilerSteamOutletBlock;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankCTBehavior;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankItem;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeModel;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlock;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlock;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlockItem;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock.TeslaTurbineStructuralPosition;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockItem;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.EmptyBreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlockItem;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBBlockModelTransformer {
    private CCBBlockModelTransformer() {
    }

    private static int horizontalRotation(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180;
            case WEST -> 270;
            case EAST -> 90;
            default -> 0;
        };
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> uncontainableCrate() {
        return builder -> builder.blockstate((context, provider) -> {
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/sturdy_crate/block"));
            provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                int rotationY = horizontalRotation(facing);
                return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
            });
        }).item(SturdyCrateBlockItem::new).tag(CCBItemTags.CRATES.tag).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> crate(String type) {
        return builder -> builder.blockstate((context, provider) -> {
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/" + type + "_crate/block"));
            provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                int rotationY = horizontalRotation(facing);
                return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
            });
        }).item().tag(CCBItemTags.CRATES.tag).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> pneumaticEngine() {
        return builder -> builder.blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p))).item().transform(ModelGen.customItemModel("pneumatic_engine", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> photoStressBearing() {
        return builder -> builder.blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p))).item().transform(ModelGen.customItemModel("photo-stress_bearing", "item"));
    }

    @Contract(pure = true)
    public static <B extends CasingBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> endCasing() {
        return casing(() -> CCBSpriteShifts.END_CASING, properties -> properties.rarity(Rarity.UNCOMMON));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> endAlloyBlock() {
        return simpleBlock("end_alloy_block", properties -> properties.rarity(Rarity.UNCOMMON));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> powderedAmethystBlock() {
        return simpleBlock("powdered_amethyst_block", properties -> properties);
    }

    @Contract(pure = true)
    public static <B extends CasingBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> casing(Supplier<CTSpriteShiftEntry> ct, NonNullUnaryOperator<Properties> ip) {
        return builder -> builder.blockstate((context, provider) -> provider.simpleBlock(context.get())).onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(ct.get()))).onRegister(CreateRegistrate.casingConnectivity((block, connectivity) -> connectivity.makeCasing(block, ct.get()))).item().properties(ip).tag(AllItemTags.CASING.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> simpleBlock(String path, NonNullUnaryOperator<Properties> ip) {
        return builder -> builder.blockstate((context, provider) -> provider.simpleBlock(context.get(), provider.models().cubeAll(context.getName(), provider.modLoc("block/" + path)))).item().properties(ip).build();
    }

    @Contract(pure = true)
    public static <B extends AirVentBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airVent() {
        return builder -> builder.blockstate((c, p) -> p.simpleBlock(c.getEntry(), p.models().getExistingFile(p.modLoc("block/air_vent/block")))).onRegister(CreateRegistrate.connectedTextures(AirVentCTBehaviour::new)).item().build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightPipe() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
            Axis axis = state.getValue(AirtightPipeBlock.AXIS);
            int rotationX = axis == Axis.Y ? 0 : 90;
            int rotationY = axis == Axis.X ? 90 : 0;

            return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc("block/airtight_pipe/pipe"))).uvLock(false).rotationX(rotationX).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).onRegister(CCBRegistrate.blockModel(() -> AirtightPipeAttachmentModel::withAO)).item().properties(Properties::fireResistant).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightEncasedPipe() {
        return builder -> builder.blockstate((context, provider) -> {
            var multipart = provider.getMultipartBuilder(context.getEntry());
            multipart.part().modelFile(AssetLookup.partialBaseModel(context, provider)).addModel().end();
            for (Direction direction : Iterate.directions) {
                multipart.part().modelFile(provider.models().getExistingFile(provider.modLoc("block/airtight_encased_pipe/" + direction.getSerializedName()))).addModel().condition(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction), false).end();
            }
        }).item(AirtightEncasedPipeBlockItem::new).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(ModelGen.customItemModel("airtight_encased_pipe", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightCheckValve() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
            Axis axis = state.getValue(AirtightCheckValveBlock.AXIS);
            boolean isInverted = state.getValue(AirtightCheckValveBlock.INVERTED);
            String modelPath = isInverted ? "block/airtight_check_valve/block_inverted" : "block/airtight_check_valve/block";

            if (axis == Axis.Y) {
                DirectionalFacing facing = state.getValue(IDirectionalPipe.DIRECTIONAL_FACING);
                if (facing == DirectionalFacing.EAST || facing == DirectionalFacing.WEST) {
                    modelPath += "_rotated";
                }

                return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc(modelPath))).uvLock(false).rotationX(0).rotationY(0).build();
            }

            int rotationY = axis == Axis.X ? 90 : 180;
            return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc(modelPath))).uvLock(false).rotationX(90).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).onRegister(CCBRegistrate.blockModel(() -> AirtightPipeAttachmentModel::withAO)).item().properties(Properties::fireResistant).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> smartAirtightPipe() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
            Axis axis = state.getValue(SmartAirtightPipeBlock.AXIS);
            String modelPath = "block/smart_airtight_pipe/block";

            if (axis == Axis.Y) {
                DirectionalFacing facing = state.getValue(IDirectionalPipe.DIRECTIONAL_FACING);
                if (facing != DirectionalFacing.NULL) {
                    modelPath += '_' + facing.getSerializedName();
                }

                return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc(modelPath))).uvLock(false).rotationX(0).rotationY(0).build();
            }

            int rotationY = axis == Axis.X ? 90 : 180;
            return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc(modelPath))).uvLock(false).rotationX(90).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).onRegister(CCBRegistrate.blockModel(() -> AirtightPipeAttachmentModel::withAO)).item().properties(Properties::fireResistant).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightPump() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
            Direction direction = state.getValue(BlockStateProperties.FACING);
            int rotationX = switch (direction) {
                case DOWN -> 180;
                case UP -> 0;
                default -> 90;
            };
            int rotationY = direction.getAxis().isVertical() ? 0 : ((int) direction.toYRot() + 180) % 360;

            return ConfiguredModel.builder().modelFile(AssetLookup.partialBaseModel(context, provider)).rotationX(rotationX).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).onRegister(CCBRegistrate.blockModel(() -> AirtightPipeAttachmentModel::withAO)).item().properties(Properties::fireResistant).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airCompressor() {
        return builder -> builder.blockstate((context, provider) -> {
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/air_compressor/block"));
            provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(AirCompressorBlock.HORIZONTAL_FACING);
                int rotationY = horizontalRotation(facing);
                return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
            }, AirCompressorBlock.ACTIVE, BlockStateProperties.WATERLOGGED);
        }).item().properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightTank() {
        return builder -> builder.blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.standardModel(c, p))).onRegister(CreateRegistrate.connectedTextures(AirtightTankCTBehavior::new)).item(AirtightTankItem::new).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> horizontalAirtightTank() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.get()).forAllStates(state -> {
            int rotationY = state.getValue(HorizontalAirtightTankBlock.HORIZONTAL_AXIS) == Axis.X ? 90 : 0;
            return ConfiguredModel.builder().modelFile(AssetLookup.standardModel(context, provider)).rotationY(rotationY).build();
        })).onRegister(CreateRegistrate.connectedTextures(HorizontalAirtightTankCTBehavior::new)).item(HorizontalAirtightTankItem::new).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> creativeAirtightTank() {
        return builder -> builder.blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.standardModel(c, p))).onRegister(CreateRegistrate.connectedTextures(CreativeAirtightTankCTBehavior::new)).item(CreativeAirtightTankItem::new).properties(p -> p.rarity(Rarity.EPIC).fireResistant()).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> gasPackager() {
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
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> gasRepackager() {
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
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> gasFactoryGauge() {
        return builder -> builder.blockstate((c, p) -> p.horizontalFaceBlock(c.get(), AssetLookup.partialBaseModel(c, p))).onRegister(CCBRegistrate.blockModel(() -> GasFactoryGaugeModel::new)).item(FactoryPanelBlockItem::new).model(AssetLookup::customItemModel).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> portableGasInterface() {
        return builder -> builder.blockstate((c, p) -> p.directionalBlock(c.get(), AssetLookup.partialBaseModel(c, p))).item().properties(Properties::fireResistant).tag(AllItemTags.CONTRAPTION_CONTROLLED.tag).transform(ModelGen.customItemModel());
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> emptyBreezeCooler() {
        return b -> b.blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/breeze_cooler/block"));
            p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(EmptyBreezeCoolerBlock.FACING);
                int rotationY = horizontalRotation(facing);
                return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED);
        }).item(BreezeCoolerBlockItem::new).model(AssetLookup.customBlockItemModel("breeze_cooler", "block")).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> breezeCooler() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
            String modelPath = "block/breeze_cooler/" + (state.getValue(BreezeCoolerBlock.ATTACHED) ? "cooler" : "block");
            ModelFile model = provider.models().getExistingFile(provider.modLoc(modelPath));
            Direction facing = state.getValue(BreezeCoolerBlock.FACING);
            int rotationY = horizontalRotation(facing);
            return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).item().model(AssetLookup.customBlockItemModel("breeze_cooler", "block_with_breeze")).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> breezeChamber() {
        return builder -> builder.blockstate((context, provider) -> {
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/breeze_chamber/block"));
            provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(BreezeChamberBlock.FACING);
                int rotationY = horizontalRotation(facing);
                return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED);
        }).item().properties(Properties::fireResistant).model(AssetLookup.customBlockItemModel("breeze_chamber", "item")).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightEngine() {
        return builder -> builder.blockstate((context, provider) -> {
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/airtight_engine/block"));
            Block block = context.get();
            provider.getVariantBuilder(block).forAllStatesExcept(state -> {
                AttachFace face = state.getValue(BlockStateProperties.ATTACH_FACE);
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                int rotationX = face.ordinal() * 90;
                int rotationY = ((int) facing.toYRot() + (face == AttachFace.CEILING ? 180 : 0)) % 360;

                return ConfiguredModel.builder().modelFile(model).rotationX(rotationX).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED);
        }).item().properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> residueOutlet() {
        return builder -> builder.blockstate((context, provider) -> {
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/residue_outlet/block"));
            Block block = context.get();
            provider.getVariantBuilder(block).forAllStatesExcept(state -> {
                AttachFace face = state.getValue(ResidueOutletBlock.FACE);
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                int rotationX = face.ordinal() * 90;
                int rotationY = ((int) facing.toYRot() + (face == AttachFace.CEILING ? 180 : 0)) % 360;
                return ConfiguredModel.builder().modelFile(model).rotationX(rotationX).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED);
        }).item().properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> boilerSteamOutlet() {
        return builder -> builder.blockstate((context, provider) -> {
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/boiler_steam_outlet/block"));
            Block block = context.get();
            provider.getVariantBuilder(block).forAllStatesExcept(state -> {
                AttachFace face = state.getValue(BoilerSteamOutletBlock.FACE);
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                int rotationX = face.ordinal() * 90;
                int rotationY = ((int) facing.toYRot() + 180 + (face == AttachFace.CEILING ? 180 : 0)) % 360;
                return ConfiguredModel.builder().modelFile(model).rotationX(rotationX).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED, BoilerSteamOutletBlock.POWERED, BoilerSteamOutletBlock.OPEN);
        }).item().properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> teslaTurbineNozzle() {
        return builder -> builder.blockstate((context, provider) -> {
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/tesla_turbine_nozzle/block"));
            provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(TeslaTurbineNozzleBlock.FACING);
                int rotationX = switch (facing) {
                    case UP -> -90;
                    case DOWN -> 90;
                    default -> 0;
                };
                int rotationY = horizontalRotation(facing);

                return ConfiguredModel.builder().modelFile(model).rotationX(rotationX).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED, TeslaTurbineNozzleBlock.CLOCKWISE);
        }).item().tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> teslaTurbine() {
        return builder -> builder.clientExtension(() -> TeslaTurbineClientExtensions::new).blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
            Axis axis = state.getValue(BlockStateProperties.AXIS);
            int rotationX = axis == Axis.Y ? 0 : 90;
            int rotationY = switch (axis) {
                case X -> 90;
                case Z -> 180;
                default -> 0;
            };

            return ConfiguredModel.builder().modelFile(AssetLookup.partialBaseModel(context, provider)).uvLock(false).rotationX(rotationX).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).item(TeslaTurbineBlockItem::new).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightReactorKettle() {
        return builder -> builder.clientExtension(() -> AirtightReactorKettleClientExtensions::new).blockstate((context, provider) -> provider.getVariantBuilder(context.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc("block/airtight_reactor_kettle/block"))).build())).item(AirtightReactorKettleBlockItem::new).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightForgingPress() {
        return builder -> builder.clientExtension(() -> AirtightForgingPressClientExtensions::new).blockstate((context, provider) -> provider.getVariantBuilder(context.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc("block/airtight_forging_press/block"))).build())).item(AirtightForgingPressBlockItem::new).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> teslaTurbineStructural() {
        return b -> b.clientExtension(() -> TeslaTurbineStructuralClientExtensions::new).blockstate((c, p) -> p.getVariantBuilder(c.get()).forAllStates(state -> {
            Axis axis = state.getValue(TeslaTurbineStructuralBlock.AXIS);
            TeslaTurbineStructuralPosition position = state.getValue(TeslaTurbineStructuralBlock.STRUCTURAL_POSITION);
            String modelPath = String.format("block/tesla_turbine/%s", position.getSerializedName());
            ModelFile model = p.models().getExistingFile(p.modLoc(modelPath));
            int rotationX = 0;
            int rotationY = 0;
            switch (axis) {
                case X -> {
                    rotationX = 90;
                    rotationY = 90;
                }
                case Z -> rotationX = 90;
            }
            return ConfiguredModel.builder().modelFile(model).rotationX(rotationX).rotationY(rotationY).build();
        })).lang("Tesla Turbine");
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightReactorKettleStructural() {
        return builder -> builder.clientExtension(() -> AirtightReactorKettleStructuralClientExtensions::new).blockstate((context, provider) -> provider.getVariantBuilder(context.get()).forAllStates(state -> {
            String position = state.getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION).getSerializedName();
            String modelPath = String.format("block/airtight_reactor_kettle/%s", position);
            return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc(modelPath))).build();
        })).lang("Airtight Reactor Kettle");
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightReactorKettleStructuralCog() {
        return builder -> builder.clientExtension(() -> AirtightReactorKettleStructuralClientExtensions::new).blockstate((context, provider) -> provider.getVariantBuilder(context.get()).forAllStates(state -> {
            String position = state.getValue(AirtightReactorKettleStructuralCogBlock.STRUCTURAL_POSITION).getSerializedName();
            String modelPath = String.format("block/airtight_reactor_kettle/%s", position);
            return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc(modelPath))).build();
        })).lang("Airtight Reactor Kettle");
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightForgingPressStructural() {
        return builder -> builder.clientExtension(() -> AirtightForgingPressStructuralClientExtensions::new).blockstate((context, provider) -> provider.getVariantBuilder(context.get()).forAllStates(state -> {
            String position = state.getValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION).getSerializedName();
            String modelPath = String.format("block/airtight_forging_press/%s", position);
            return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc(modelPath))).build();
        })).lang("Airtight Forging Press");
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightForgingPressStructuralShaft() {
        return builder -> builder.clientExtension(() -> AirtightForgingPressStructuralClientExtensions::new).blockstate((context, provider) -> provider.getVariantBuilder(context.get()).forAllStates(state -> {
            String position = state.getValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION).getSerializedName();
            String modelPath = String.format("block/airtight_forging_press/%s", position);
            return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc(modelPath))).build();
        })).lang("Airtight Forging Press");
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> gasCanister() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc("block/gas_canister"))).build(), BlockStateProperties.WATERLOGGED)).loot((loot, block) -> loot.add(block, LootTable.lootTable().withPool(LootPool.lootPool().when(ExplosionCondition.survivesExplosion()).setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(CCBItems.GAS_CANISTER.get()).apply(CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY).include(CCBDataComponents.CANISTER_CONTAINER_CONTENTS).include(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES)))))).item().build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> creativeGasCanister() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc("block/creative_gas_canister"))).build(), BlockStateProperties.WATERLOGGED)).loot((loot, block) -> loot.add(block, LootTable.lootTable().withPool(LootPool.lootPool().when(ExplosionCondition.survivesExplosion()).setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(CCBItems.CREATIVE_GAS_CANISTER.get()).apply(CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY).include(CCBDataComponents.CANISTER_CONTAINER_CONTENTS).include(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES)))))).item().build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightHatch() {
        return builder -> builder.blockstate((context, provider) -> provider.horizontalBlock(context.get(), state -> AssetLookup.partialBaseModel(context, provider, state.getValue(AirtightHatchBlock.CANISTER_TYPE).getSerializedName()))).item().properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(itemBuilder -> itemBuilder.model(AssetLookup.customBlockItemModel("_", "block_empty"))).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> gasInjectionChamber() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
            Direction facing = state.getValue(GasInjectionChamberBlock.FACING);
            int rotationY = horizontalRotation(facing);
            return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc("block/gas_injection_chamber/block"))).rotationY(rotationY).build();
        })).item(AssemblyOperatorBlockItem::new).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtightSheetBlock() {
        return builder -> builder.blockstate((c, p) -> p.simpleBlock(c.get(), p.models().cubeAll(c.getName(), p.modLoc("block/airtight_sheet_block")))).item().properties(Properties::fireResistant).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> obsidianAlikeBlocks(String name) {
        return builder -> builder.blockstate((c, p) -> p.simpleBlock(c.get(), p.models().cubeAll(c.getName(), p.modLoc("block/obsidians/" + name)))).item().tag(CCBItemTags.OBSIDIAN_BRICKS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> cryingObsidianAlikeBlocks(String name) {
        return builder -> builder.blockstate((c, p) -> p.simpleBlock(c.get(), p.models().cubeAll(c.getName(), p.modLoc("block/obsidians/" + name)))).item().tag(CCBItemTags.CRYING_OBSIDIAN_BRICKS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends SlabBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> obsidianAlikeSlabs(String name) {
        return obsidianAlikeSlabs(name, name);
    }

    @Contract(pure = true)
    public static <B extends SlabBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> obsidianAlikeSlabs(String sideName, String topName) {
        return builder -> builder.blockstate((context, provider) -> provider.slabBlock(context.get(), provider.models().slab(context.getName(), provider.modLoc("block/obsidians/" + sideName), provider.modLoc("block/obsidians/" + topName), provider.modLoc("block/obsidians/" + topName)), provider.models().slabTop(context.getName() + "_top", provider.modLoc("block/obsidians/" + sideName), provider.modLoc("block/obsidians/" + topName), provider.modLoc("block/obsidians/" + topName)), provider.models().cubeColumn(context.getName() + "_double", provider.modLoc("block/obsidians/" + sideName), provider.modLoc("block/obsidians/" + topName)))).item().tag(ItemTags.SLABS).build();
    }

    @Contract(pure = true)
    public static <B extends StairBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> obsidianAlikeStairs(String name) {
        return builder -> builder.blockstate((c, p) -> p.stairsBlock(c.get(), p.modLoc("block/obsidians/" + name))).item().tag(ItemTags.STAIRS).build();
    }

    @Contract(pure = true)
    public static <B extends WallBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> obsidianAlikeWall(String name) {
        return builder -> builder.blockstate((c, p) -> p.wallBlock(c.get(), name + "_wall", p.modLoc("block/obsidians/" + name))).item().transform(b -> b.model((c, p) -> p.wallInventory(c.getName(), p.modLoc("block/obsidians/" + name)))).tag(ItemTags.WALLS).build();
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> endIncinerationBlower() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc("block/end_incineration_blower/block"))).build())).item().properties(properties -> properties.rarity(Rarity.UNCOMMON)).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> endIncinerationBlowerStructural() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc("block/end_incineration_blower/structural"))).build())).lang("End Incineration Blower");
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> endSculkSilencer() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc("block/end_sculk_silencer/block"))).build())).item().properties(properties -> properties.rarity(Rarity.UNCOMMON)).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> endSculkSilencerStructural() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc("block/end_sculk_silencer/structural"))).build())).lang("End Sculk Silencer");
    }

}
