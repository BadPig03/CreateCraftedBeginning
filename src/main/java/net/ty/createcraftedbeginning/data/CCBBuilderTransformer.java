package net.ty.createcraftedbeginning.data;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.processing.AssemblyOperatorBlockItem;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightintakeport.AirtightIntakePortBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeAttachmentModel;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankCTBehavior;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankItem;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankMovementBehavior;
import net.ty.createcraftedbeginning.content.airtights.checkvalve.CheckValveBlock;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankCTBehavior;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankItem;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankMovementBehavior;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlock;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceMovement;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberConductor;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberMovementBehaviour;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockItem;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerConductor;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerMovementBehaviour;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.EmptyBreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.crates.UncontainableBlockItem;
import net.ty.createcraftedbeginning.api.gas.interfaces.IDirectionalPipe;
import net.ty.createcraftedbeginning.api.gas.interfaces.IDirectionalPipe.DirectionalFacing;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

import static com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour.interactionBehaviour;
import static com.simibubi.create.api.behaviour.movement.MovementBehaviour.movementBehaviour;
import static com.simibubi.create.foundation.data.CreateRegistrate.casingConnectivity;
import static com.simibubi.create.foundation.data.CreateRegistrate.connectedTextures;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static net.ty.createcraftedbeginning.api.gas.MountedGasStorageType.mountedGasStorage;

@SuppressWarnings("removal")
public class CCBBuilderTransformer {
    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> uncontainable_crate() {
        return b -> b.blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/sturdy_crate/block"));
            p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                int rotationY = 0;
                switch (facing) {
                    case SOUTH:
                        rotationY = 180;
                        break;
                    case WEST:
                        rotationY = 270;
                        break;
                    case EAST:
                        rotationY = 90;
                        break;
                    default:
                        break;
                }
                return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
            });
        }).tag(CCBTags.CCBBlockTags.CRATES.tag).item(UncontainableBlockItem::new).tag(CCBTags.CCBItemTags.CRATES.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> crate(String type) {
        return b -> b.blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/" + type + "_crate/block"));
            p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                int rotationY = 0;
                switch (facing) {
                    case SOUTH:
                        rotationY = 180;
                        break;
                    case WEST:
                        rotationY = 270;
                        break;
                    case EAST:
                        rotationY = 90;
                        break;
                    default:
                        break;
                }
                return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
            });
        }).tag(CCBTags.CCBBlockTags.CRATES.tag).item().tag(CCBTags.CCBItemTags.CRATES.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> pneumatic_engine() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p))).item().transform(customItemModel("pneumatic_engine", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> photo_stress_bearing() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p))).item().transform(customItemModel("photo-stress_bearing", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> cinder_incineration_blower() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p))).item().transform(customItemModel("cinder_incineration_blower", "item"));
    }

    @Contract(pure = true)
    public static <B extends CasingBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> casing(Supplier<CTSpriteShiftEntry> ct) {
        return b -> b.initialProperties(SharedProperties::stone).blockstate((c, p) -> p.simpleBlock(c.get())).onRegister(connectedTextures(() -> new EncasedCTBehaviour(ct.get()))).onRegister(casingConnectivity((block, cc) -> cc.makeCasing(block, ct.get()))).tag(AllTags.AllBlockTags.CASING.tag).item().properties(Item.Properties::fireResistant).tag(AllTags.AllItemTags.CASING.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> simple_block(String path) {
        return b -> b.blockstate((c, p) -> p.simpleBlock(c.get(), p.models().cubeAll(c.getName(), p.modLoc("block/" + path)))).item().build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_pipe() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
            Direction.Axis axis = state.getValue(AirtightPipeBlock.AXIS);
            return ConfiguredModel.builder().modelFile(p.models().getExistingFile(p.modLoc("block/airtight_pipe/pipe"))).uvLock(false).rotationX(axis == Direction.Axis.Y ? 0 : 90).rotationY(axis == Direction.Axis.X ? 90 : 0).build();
        }, BlockStateProperties.WATERLOGGED)).onRegister(CCBRegistrate.blockModel(() -> AirtightPipeAttachmentModel::withAO)).item().properties(Item.Properties::fireResistant).transform(ib -> ib.model(AssetLookup::customItemModel)).tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_encased_pipe() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p))).item().properties(Item.Properties::fireResistant).tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(customItemModel("airtight_encased_pipe", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> check_valve() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
            Direction.Axis axis = state.getValue(CheckValveBlock.AXIS);
            boolean inverted = state.getValue(CheckValveBlock.INVERTED);
            String modelPath = inverted ? "block/check_valve/block_inverted" : "block/check_valve/block";
            int rotationX = 0;
            int rotationY = 0;
            if (axis == Direction.Axis.Y) {
                DirectionalFacing directionalFacing = state.getValue(IDirectionalPipe.DIRECTIONAL_FACING);
                if (directionalFacing == DirectionalFacing.EAST || directionalFacing == DirectionalFacing.WEST) {
                    modelPath += "_rotated";
                }
            } else {
                switch (axis) {
                    case X -> {
                        rotationX = 90;
                        rotationY = 90;
                    }
                    case Z -> {
                        rotationX = 90;
                        rotationY = 180;
                    }
                }
            }
            return ConfiguredModel.builder().modelFile(p.models().getExistingFile(p.modLoc(modelPath))).uvLock(false).rotationX(rotationX).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).onRegister(CCBRegistrate.blockModel(() -> AirtightPipeAttachmentModel::withAO)).item().properties(Item.Properties::fireResistant).transform(ib -> ib.model(AssetLookup::customItemModel)).tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> smart_airtight_pipe() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
            Direction.Axis axis = state.getValue(SmartAirtightPipeBlock.AXIS);
            String modelPath = "block/smart_airtight_pipe/block";
            int rotationX = 0;
            int rotationY = 0;
            if (axis == Direction.Axis.Y) {
                DirectionalFacing directionalFacing = state.getValue(IDirectionalPipe.DIRECTIONAL_FACING);
                if (directionalFacing != DirectionalFacing.NULL) {
                    modelPath += "_" + directionalFacing.getSerializedName();
                }
            } else {
                switch (axis) {
                    case X -> {
                        rotationX = 90;
                        rotationY = 90;
                    }
                    case Z -> {
                        rotationX = 90;
                        rotationY = 180;
                    }
                }
            }
            return ConfiguredModel.builder().modelFile(p.models().getExistingFile(p.modLoc(modelPath))).uvLock(false).rotationX(rotationX).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).onRegister(CCBRegistrate.blockModel(() -> AirtightPipeAttachmentModel::withAO)).item().properties(Item.Properties::fireResistant).transform(ib -> ib.model(AssetLookup::customItemModel)).tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_pump() {
        return b -> b.blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
            Direction dir = state.getValue(BlockStateProperties.FACING);
            return ConfiguredModel.builder().modelFile(getBlockModel(c, p).apply(state)).rotationX(dir == Direction.DOWN ? 180 : dir.getAxis().isHorizontal() ? 90 : 0).rotationY(dir.getAxis().isVertical() ? 0 : (((int) dir.toYRot()) + 180) % 360).build();
        }, BlockStateProperties.WATERLOGGED)).onRegister(CCBRegistrate.blockModel(() -> AirtightPipeAttachmentModel::withAO)).item().properties(Item.Properties::fireResistant).transform(ib -> ib.model(AssetLookup::customItemModel)).tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_tank() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.standardModel(c, p))).transform(mountedGasStorage(CCBMountedStorage.GAS_TANK)).onRegister(connectedTextures(AirtightTankCTBehavior::new)).onRegister(movementBehaviour(new AirtightTankMovementBehavior())).item(AirtightTankItem::new).properties(Item.Properties::fireResistant).tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> creative_airtight_tank() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.standardModel(c, p))).transform(mountedGasStorage(CCBMountedStorage.GAS_TANK)).onRegister(connectedTextures(CreativeAirtightTankCTBehavior::new)).onRegister(movementBehaviour(new CreativeAirtightTankMovementBehavior())).item(CreativeAirtightTankItem::new).properties(p -> p.rarity(Rarity.EPIC)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> portable_gas_interface() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.directionalBlock(c.get(), AssetLookup.partialBaseModel(c, p))).onRegister(movementBehaviour(new PortableGasInterfaceMovement())).item().properties(Item.Properties::fireResistant).tag(AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag).transform(customItemModel());
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> empty_breeze_cooler() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/breeze_cooler/block"));
            p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(EmptyBreezeCoolerBlock.FACING);
                int rotationY = 0;
                switch (facing) {
                    case SOUTH:
                        rotationY = 180;
                        break;
                    case WEST:
                        rotationY = 270;
                        break;
                    case EAST:
                        rotationY = 90;
                        break;
                    default:
                        break;
                }
                return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED);
        }).item(BreezeCoolerBlockItem::new).model(AssetLookup.customBlockItemModel("breeze_cooler", "block")).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> breeze_cooler() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
            String modelPath = "block/breeze_cooler/" + (state.getValue(BreezeCoolerBlock.COOLER) ? "cooler" : "block");
            ModelFile model = p.models().getExistingFile(p.modLoc(modelPath));
            Direction facing = state.getValue(BreezeCoolerBlock.FACING);
            int rotationY = 0;
            switch (facing) {
                case SOUTH:
                    rotationY = 180;
                    break;
                case WEST:
                    rotationY = 270;
                    break;
                case EAST:
                    rotationY = 90;
                    break;
                default:
                    break;
            }
            return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
        }, BlockStateProperties.WATERLOGGED)).onRegister(movementBehaviour(new BreezeCoolerMovementBehaviour())).onRegister(interactionBehaviour(new BreezeCoolerConductor.BreezeChamber())).item().model(AssetLookup.customBlockItemModel("breeze_cooler", "block_with_breeze")).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> breeze_chamber() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/breeze_chamber/block"));
            p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(BreezeChamberBlock.FACING);
                int rotationY = 0;
                switch (facing) {
                    case SOUTH:
                        rotationY = 180;
                        break;
                    case WEST:
                        rotationY = 270;
                        break;
                    case EAST:
                        rotationY = 90;
                        break;
                    default:
                        break;
                }
                return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED);
        }).onRegister(movementBehaviour(new BreezeChamberMovementBehaviour())).onRegister(interactionBehaviour(new BreezeChamberConductor.BreezeChamber())).item().properties(Item.Properties::fireResistant).model(AssetLookup.customBlockItemModel("breeze_chamber", "item")).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_engine() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/airtight_engine/block"));
            Block block = c.get();
            p.getVariantBuilder(block).forAllStatesExcept(state -> {
                int rotationX = state.getValue(BlockStateProperties.ATTACH_FACE).ordinal() * 90;
                int rotationY = (((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()) + (state.getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.CEILING ? 180 : 0)) % 360;
                return ConfiguredModel.builder().modelFile(model).rotationX(rotationX).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED);
        }).item().properties(Item.Properties::fireResistant).tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> condensate_drain() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/condensate_drain/block"));
            Block block = c.get();
            p.getVariantBuilder(block).forAllStatesExcept(state -> {
                int rotationX = state.getValue(BlockStateProperties.ATTACH_FACE).ordinal() * 90;
                int rotationY = (((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()) + (state.getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.CEILING ? 180 : 0)) % 360;
                return ConfiguredModel.builder().modelFile(model).rotationX(rotationX).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED);
        }).item().properties(Item.Properties::fireResistant).tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_intake_port() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/airtight_intake_port/block"));
            p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(AirtightIntakePortBlock.FACING);
                int rotationX = 0;
                int rotationY = 0;
                switch (facing) {
                    case UP:
                        rotationX = 270;
                        break;
                    case DOWN:
                        rotationX = 90;
                        break;
                    case SOUTH:
                        rotationY = 180;
                        break;
                    case WEST:
                        rotationY = 270;
                        break;
                    case EAST:
                        rotationY = 90;
                        break;
                    default:
                        break;
                }
                return ConfiguredModel.builder().modelFile(model).rotationX(rotationX).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED);
        }).item().properties(Item.Properties::fireResistant).tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> air_compressor() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/air_compressor/block"));
            p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(AirCompressorBlock.HORIZONTAL_FACING);
                int rotationY = 0;
                switch (facing) {
                    case SOUTH:
                        rotationY = 180;
                        break;
                    case WEST:
                        rotationY = 270;
                        break;
                    case EAST:
                        rotationY = 90;
                        break;
                    default:
                        break;
                }
                return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED);
        }).item().properties(Item.Properties::fireResistant).tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> gas_injection_chamber() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/gas_injection_chamber/block"));
            p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(GasInjectionChamberBlock.FACING);
                int rotationY = 0;
                switch (facing) {
                    case SOUTH:
                        rotationY = 180;
                        break;
                    case WEST:
                        rotationY = 270;
                        break;
                    case EAST:
                        rotationY = 90;
                        break;
                    default:
                        break;
                }
                return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
            });
        }).item(AssemblyOperatorBlockItem::new).properties(Item.Properties::fireResistant).tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_sheet_block() {
        return b -> b.blockstate((c, p) -> p.simpleBlock(c.get(), p.models().cubeAll(c.getName(), p.modLoc("block/airtight_sheet_block")))).item().properties(Item.Properties::fireResistant).build();
    }

    @Contract(pure = true)
    private static <T extends Block> @NotNull Function<BlockState, ModelFile> getBlockModel(DataGenContext<Block, T> c, RegistrateBlockstateProvider p) {
        return $ -> AssetLookup.partialBaseModel(c, p);
    }
}
