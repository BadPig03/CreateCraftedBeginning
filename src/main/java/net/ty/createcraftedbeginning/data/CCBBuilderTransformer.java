package net.ty.createcraftedbeginning.data;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.processing.AssemblyOperatorBlockItem;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition.Builder;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.ty.createcraftedbeginning.api.gas.gases.IDirectionalPipe;
import net.ty.createcraftedbeginning.api.gas.gases.IDirectionalPipe.DirectionalFacing;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeAttachmentModel;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockItem;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralCogBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankCTBehavior;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankItem;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankMovementBehavior;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentCTBehaviour;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankCTBehavior;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankItem;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankMovementBehavior;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlock;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceMovement;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlockItem;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock.TeslaTurbineStructuralPosition;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberConductor.BreezeChamber;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberMovementBehaviour;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockItem;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerConductor;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerMovementBehaviour;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.EmptyBreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlockItem;
import net.ty.createcraftedbeginning.content.obsolete.airtightintakeport.AirtightIntakePortBlock;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

import static com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour.interactionBehaviour;
import static com.simibubi.create.api.behaviour.movement.MovementBehaviour.movementBehaviour;
import static com.simibubi.create.foundation.data.CreateRegistrate.casingConnectivity;
import static com.simibubi.create.foundation.data.CreateRegistrate.connectedTextures;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static net.ty.createcraftedbeginning.api.gas.gases.MountedGasStorageType.mountedGasStorage;

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
        }).tag(CCBBlockTags.CRATES.tag).item(SturdyCrateBlockItem::new).tag(CCBItemTags.CRATES.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
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
        }).tag(CCBBlockTags.CRATES.tag).item().tag(CCBItemTags.CRATES.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> pneumatic_engine() {
        return b -> b.blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p))).item().transform(customItemModel("pneumatic_engine", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> photo_stress_bearing() {
        return b -> b.blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p))).item().transform(customItemModel("photo-stress_bearing", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> cinder_incineration_blower() {
        return b -> b.blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p))).item().transform(customItemModel("cinder_incineration_blower", "item"));
    }

    @Contract(pure = true)
    public static <B extends CasingBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> casing(Supplier<CTSpriteShiftEntry> ct) {
        return b -> b.blockstate((c, p) -> p.simpleBlock(c.get())).onRegister(connectedTextures(() -> new EncasedCTBehaviour(ct.get()))).onRegister(casingConnectivity((block, cc) -> cc.makeCasing(block, ct.get()))).tag(AllBlockTags.CASING.tag).item().tag(AllItemTags.CASING.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> simple_block(String path) {
        return b -> b.blockstate((c, p) -> p.simpleBlock(c.get(), p.models().cubeAll(c.getName(), p.modLoc("block/" + path)))).item().build();
    }

    @Contract(pure = true)
    public static <B extends AirVentBlock> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> air_vent() {
        return b -> b.blockstate((c, p) -> p.simpleBlock(c.getEntry(), p.models().getExistingFile(p.modLoc("block/air_vent/block")))).onRegister(connectedTextures(AirVentCTBehaviour::new)).properties(p -> p.mapColor(MapColor.DEEPSLATE).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().dynamicShape()).item().build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_pipe() {
        return b -> b.blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
            Axis axis = state.getValue(AirtightPipeBlock.AXIS);
            return ConfiguredModel.builder().modelFile(p.models().getExistingFile(p.modLoc("block/airtight_pipe/pipe"))).uvLock(false).rotationX(axis == Axis.Y ? 0 : 90).rotationY(axis == Axis.X ? 90 : 0).build();
        }, BlockStateProperties.WATERLOGGED)).onRegister(CCBRegistrate.blockModel(() -> AirtightPipeAttachmentModel::withAO)).item().properties(Properties::fireResistant).transform(ib -> ib.model(AssetLookup::customItemModel)).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_encased_pipe() {
        return b -> b.blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p))).item().properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(customItemModel("airtight_encased_pipe", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_check_valve() {
        return b -> b.blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
            Axis axis = state.getValue(AirtightCheckValveBlock.AXIS);
            boolean inverted = state.getValue(AirtightCheckValveBlock.INVERTED);
            String modelPath = inverted ? "block/airtight_check_valve/block_inverted" : "block/airtight_check_valve/block";
            int rotationX = 0;
            int rotationY = 0;
            if (axis == Axis.Y) {
                DirectionalFacing directionalFacing = state.getValue(IDirectionalPipe.DIRECTIONAL_FACING);
                if (directionalFacing == DirectionalFacing.EAST || directionalFacing == DirectionalFacing.WEST) {
                    modelPath += "_rotated";
                }
            }
            else {
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
        }, BlockStateProperties.WATERLOGGED)).onRegister(CCBRegistrate.blockModel(() -> AirtightPipeAttachmentModel::withAO)).item().properties(Properties::fireResistant).transform(ib -> ib.model(AssetLookup::customItemModel)).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> smart_airtight_pipe() {
        return b -> b.blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
            Axis axis = state.getValue(SmartAirtightPipeBlock.AXIS);
            String modelPath = "block/smart_airtight_pipe/block";
            int rotationX = 0;
            int rotationY = 0;
            if (axis == Axis.Y) {
                DirectionalFacing directionalFacing = state.getValue(IDirectionalPipe.DIRECTIONAL_FACING);
                if (directionalFacing != DirectionalFacing.NULL) {
                    modelPath += '_' + directionalFacing.getSerializedName();
                }
            }
            else {
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
        }, BlockStateProperties.WATERLOGGED)).onRegister(CCBRegistrate.blockModel(() -> AirtightPipeAttachmentModel::withAO)).item().properties(Properties::fireResistant).transform(ib -> ib.model(AssetLookup::customItemModel)).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_pump() {
        return b -> b.blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
            Direction dir = state.getValue(BlockStateProperties.FACING);
            return ConfiguredModel.builder().modelFile(getBlockModel(c, p).apply(state)).rotationX(dir == Direction.DOWN ? 180 : dir.getAxis().isHorizontal() ? 90 : 0).rotationY(dir.getAxis().isVertical() ? 0 : ((int) dir.toYRot() + 180) % 360).build();
        }, BlockStateProperties.WATERLOGGED)).onRegister(CCBRegistrate.blockModel(() -> AirtightPipeAttachmentModel::withAO)).item().properties(Properties::fireResistant).transform(ib -> ib.model(AssetLookup::customItemModel)).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    private static <T extends Block> @NotNull Function<BlockState, ModelFile> getBlockModel(DataGenContext<Block, T> c, RegistrateBlockstateProvider p) {
        return $ -> AssetLookup.partialBaseModel(c, p);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> air_compressor() {
        return b -> b.blockstate((c, p) -> {
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
        }).item().properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_tank() {
        return b -> b.blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.standardModel(c, p))).transform(mountedGasStorage(CCBMountedStorage.AIRTIGHT_TANK)).onRegister(connectedTextures(AirtightTankCTBehavior::new)).onRegister(movementBehaviour(new AirtightTankMovementBehavior())).item(AirtightTankItem::new).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> creative_airtight_tank() {
        return b -> b.blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.standardModel(c, p))).transform(mountedGasStorage(CCBMountedStorage.CREATIVE_AIRTIGHT_TANK)).onRegister(connectedTextures(CreativeAirtightTankCTBehavior::new)).onRegister(movementBehaviour(new CreativeAirtightTankMovementBehavior())).item(CreativeAirtightTankItem::new).properties(p -> p.rarity(Rarity.EPIC).fireResistant()).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> portable_gas_interface() {
        return b -> b.blockstate((c, p) -> p.directionalBlock(c.get(), AssetLookup.partialBaseModel(c, p))).onRegister(movementBehaviour(new PortableGasInterfaceMovement())).item().properties(Properties::fireResistant).tag(AllItemTags.CONTRAPTION_CONTROLLED.tag).transform(customItemModel());
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> empty_breeze_cooler() {
        return b -> b.blockstate((c, p) -> {
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
        return b -> b.blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
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
        return b -> b.blockstate((c, p) -> {
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
        }).onRegister(movementBehaviour(new BreezeChamberMovementBehaviour())).onRegister(interactionBehaviour(new BreezeChamber())).item().properties(Properties::fireResistant).model(AssetLookup.customBlockItemModel("breeze_chamber", "item")).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_engine() {
        return b -> b.blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/airtight_engine/block"));
            Block block = c.get();
            p.getVariantBuilder(block).forAllStatesExcept(state -> {
                int rotationX = state.getValue(BlockStateProperties.ATTACH_FACE).ordinal() * 90;
                int rotationY = ((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() + (state.getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.CEILING ? 180 : 0)) % 360;
                return ConfiguredModel.builder().modelFile(model).rotationX(rotationX).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED);
        }).item().properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> residue_outlet() {
        return b -> b.blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/residue_outlet/block"));
            Block block = c.get();
            p.getVariantBuilder(block).forAllStatesExcept(state -> {
                int rotationX = state.getValue(BlockStateProperties.ATTACH_FACE).ordinal() * 90;
                int rotationY = ((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() + (state.getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.CEILING ? 180 : 0)) % 360;
                return ConfiguredModel.builder().modelFile(model).rotationX(rotationX).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED);
        }).item().properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> tesla_turbine_nozzle() {
        return b -> b.blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/tesla_turbine_nozzle/block"));
            p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(TeslaTurbineNozzleBlock.FACING);
                int rotationX = 0;
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
                    case UP:
                        rotationX = -90;
                        break;
                    case DOWN:
                        rotationX = 90;
                        break;
                }
                return ConfiguredModel.builder().modelFile(model).rotationX(rotationX).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED, TeslaTurbineNozzleBlock.CLOCKWISE);
        }).item().tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> tesla_turbine() {
        return b -> b.blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
            Axis axis = state.getValue(BlockStateProperties.AXIS);
            return ConfiguredModel.builder().modelFile(AssetLookup.partialBaseModel(c, p)).uvLock(false).rotationX(axis == Axis.Y ? 0 : 90).rotationY(axis == Axis.X ? 90 : axis == Axis.Z ? 180 : 0).build();
        }, BlockStateProperties.WATERLOGGED)).item(TeslaTurbineBlockItem::new).transform(ib -> ib.model(AssetLookup::customItemModel)).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_reactor_kettle() {
        return b -> b.blockstate((c, p) -> p.getVariantBuilder(c.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(p.models().getExistingFile(p.modLoc("block/airtight_reactor_kettle/block"))).build())).item(AirtightReactorKettleBlockItem::new).transform(ib -> ib.model(AssetLookup::customItemModel)).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> tesla_turbine_structural() {
        return b -> b.blockstate((c, p) -> p.getVariantBuilder(c.get()).forAllStates(state -> {
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
        })).properties(BlockBehaviour.Properties::noOcclusion);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_reactor_kettle_structural() {
        return b -> b.blockstate((c, p) -> p.getVariantBuilder(c.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(p.models().getExistingFile(p.modLoc(String.format("block/airtight_reactor_kettle/%s", state.getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION).getSerializedName())))).build())).properties(BlockBehaviour.Properties::noOcclusion);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_reactor_kettle_structural_cog() {
        return b -> b.blockstate((c, p) -> p.getVariantBuilder(c.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(p.models().getExistingFile(p.modLoc(String.format("block/airtight_reactor_kettle/%s", state.getValue(AirtightReactorKettleStructuralCogBlock.STRUCTURAL_POSITION).getSerializedName())))).build())).properties(BlockBehaviour.Properties::noOcclusion);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> gas_canister() {
        return b -> b.blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/gas_canister"));
            p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(model).build(), BlockStateProperties.WATERLOGGED);
        }).loot((lt, block) -> {
            LootTable.Builder builder = LootTable.lootTable();
            Builder survivesExplosion = ExplosionCondition.survivesExplosion();
            lt.add(block, builder.withPool(LootPool.lootPool().when(survivesExplosion).setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(CCBItems.GAS_CANISTER.get()).apply(CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY).include(CCBDataComponents.CANISTER_CONTAINER_CONTENTS).include(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES)))));
        }).item().build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_hatch() {
        return b -> b.blockstate((c, p) -> p.horizontalBlock(c.get(), s -> AssetLookup.partialBaseModel(c, p, s.getValue(AirtightHatchBlock.OCCUPIED) ? "occupied" : "empty"))).item().properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(bi -> bi.model(AssetLookup.customBlockItemModel("_", "block_empty"))).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_intake_port() {
        return b -> b.blockstate((c, p) -> {
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
        }).item().properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> gas_injection_chamber() {
        return b -> b.blockstate((c, p) -> {
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
        }).item(AssemblyOperatorBlockItem::new).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_sheet_block() {
        return b -> b.blockstate((c, p) -> p.simpleBlock(c.get(), p.models().cubeAll(c.getName(), p.modLoc("block/airtight_sheet_block")))).item().properties(Properties::fireResistant).build();
    }

    @Contract(pure = true)
    public static <T extends Block, P> @NotNull NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> axeOrPickaxe() {
        return b -> b.tag(BlockTags.MINEABLE_WITH_AXE).tag(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    @Contract(pure = true)
    public static <T extends Block, P> @NotNull NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> axeOnly() {
        return b -> b.tag(BlockTags.MINEABLE_WITH_AXE);
    }

    @Contract(pure = true)
    public static <T extends Block, P> @NotNull NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> minableWithShovel() {
        return b -> b.tag(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    @Contract(pure = true)
    public static <T extends Block, P> @NotNull NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> breezes() {
        return b -> b.initialProperties(CCBSharedProperties::hardMetal).transform(pickaxeOnly()).properties(p -> p.mapColor(MapColor.COLOR_BLUE).noOcclusion());
    }

    @Contract(pure = true)
    public static <T extends Block, P> @NotNull NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> pickaxeOnly() {
        return b -> b.tag(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    @Contract(pure = true)
    public static <T extends Block, P> @NotNull NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> airtightPropertiesWithoutAirtightComponents() {
        return b -> b.initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).requiresCorrectToolForDrops());
    }

    @Contract(pure = true)
    public static <T extends Block, P> @NotNull NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> airtightPropertiesWithoutOcclusion() {
        return b -> b.initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.HEAVY_CORE).requiresCorrectToolForDrops().noOcclusion()).tag(CCBBlockTags.AIRTIGHT_COMPONENTS.tag);
    }

    @Contract(pure = true)
    public static <T extends Block, P> @NotNull NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> airtightStructural() {
        return b -> b.initialProperties(CCBSharedProperties::airtightMetal).transform(pickaxeOnly()).properties(p -> p.mapColor(MapColor.METAL).sound(SoundType.EMPTY).requiresCorrectToolForDrops().noOcclusion()).tag(CCBBlockTags.AIRTIGHT_COMPONENTS.tag);
    }
}
