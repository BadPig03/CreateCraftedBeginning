package net.ty.createcraftedbeginning.data;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.ModelGen;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.ty.createcraftedbeginning.content.aircompressor.AirCompressorBlock;
import net.ty.createcraftedbeginning.content.airtightintakeport.AirtightIntakePortBlock;
import net.ty.createcraftedbeginning.content.airtightpipe.AirtightPipeAttachmentModel;
import net.ty.createcraftedbeginning.content.airtighttank.AirtightTankCTBehavior;
import net.ty.createcraftedbeginning.content.airtighttank.AirtightTankItem;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlockItem;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberConductor;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberMovementBehaviour;
import net.ty.createcraftedbeginning.content.sturdycrate.UncontainableBlockItem;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour.interactionBehaviour;
import static com.simibubi.create.api.behaviour.movement.MovementBehaviour.movementBehaviour;
import static com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType.mountedFluidStorage;
import static com.simibubi.create.foundation.data.CreateRegistrate.casingConnectivity;
import static com.simibubi.create.foundation.data.CreateRegistrate.connectedTextures;

@SuppressWarnings("removal")
public class CCBBuilderTransformer {
    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> uncontainable_crate() {
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

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> crate(String type) {
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

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> pneumatic_engine() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p))).item().transform(ModelGen.customItemModel("pneumatic_engine", "item"));
    }

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> photo_stress_bearing() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p))).item().transform(ModelGen.customItemModel("photo-stress_bearing", "item"));
    }

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> cinder_incineration_blower() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p))).item().transform(ModelGen.customItemModel("cinder_incineration_blower", "item"));
    }

    public static <B extends CasingBlock> NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> casing(Supplier<CTSpriteShiftEntry> ct) {
        return b -> b.initialProperties(SharedProperties::stone).blockstate((c, p) -> p.simpleBlock(c.get())).onRegister(connectedTextures(() -> new EncasedCTBehaviour(ct.get()))).onRegister(casingConnectivity((block, cc) -> cc.makeCasing(block, ct.get()))).tag(AllTags.AllBlockTags.CASING.tag).item().properties(Item.Properties::fireResistant).tag(AllTags.AllItemTags.CASING.tag).build();
    }

    public static <B extends Block> NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> simple_block(String path) {
        return b -> b.blockstate((c, p) -> p.simpleBlock(c.get(), p.models().cubeAll(c.getName(), p.modLoc("block/" + path)))).item().build();
    }

    public static <B extends Block> NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_tank() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.standardModel(c, p))).transform(mountedFluidStorage(CCBMountedStorage.AIRTIGHT_TANK)).onRegister(connectedTextures(AirtightTankCTBehavior::new)).item(AirtightTankItem::new).tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    public static <B extends Block> NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_pipe() {
        return b -> b.properties(BlockBehaviour.Properties::noOcclusion).addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
            Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);
            return ConfiguredModel.builder().modelFile(p.models().getExistingFile(p.modLoc("block/airtight_pipe/pipe"))).uvLock(false).rotationX(axis == Direction.Axis.Y ? 0 : 90).rotationY(axis == Direction.Axis.X ? 90 : 0).build();
        }, BlockStateProperties.WATERLOGGED)).onRegister(CCBRegistrate.blockModel(() -> AirtightPipeAttachmentModel::withAO)).item().transform(ib -> ib.model(AssetLookup::customItemModel)).tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    public static <B extends Block> NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_pump() {
        return b -> b.blockstate((c, p) -> p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
            Direction dir = state.getValue(BlockStateProperties.FACING);
            return ConfiguredModel.builder().modelFile(getBlockModel(c, p).apply(state)).rotationX(dir == Direction.DOWN ? 180 : dir.getAxis().isHorizontal() ? 90 : 0).rotationY(dir.getAxis().isVertical() ? 0 : (((int) dir.toYRot()) + 180) % 360).build();
        }, BlockStateProperties.WATERLOGGED)).onRegister(CCBRegistrate.blockModel(() -> AirtightPipeAttachmentModel::withAO)).item().transform(ib -> ib.model(AssetLookup::customItemModel)).tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    public static <B extends Block> NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> airtight_intake_port() {
        return b -> b.addLayer(() -> RenderType::cutout).blockstate((c, p) -> {
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
        }).item().tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    public static <B extends Block> NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> air_compressor() {
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
        }).item().tag(CCBTags.CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    public static <B extends Block> NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> empty_breeze_chamber() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/breeze_chamber/block"));
            p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(model).build(), BlockStateProperties.WATERLOGGED);
        }).item(BreezeChamberBlockItem::new).model(AssetLookup.customBlockItemModel("breeze_chamber", "block")).build();
    }

    public static <B extends Block> NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> breeze_chamber() {
        return b -> b.addLayer(() -> RenderType::cutoutMipped).loot((lt, block) -> lt.add(block, BreezeChamberBlock.buildLootTable())).blockstate((c, p) -> {
            AtomicReference<ModelFile> model = new AtomicReference<>();
            p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> {
                boolean cooler = state.getValue(BreezeChamberBlock.COOLER);
                if (cooler) {
                    model.set(p.models().getExistingFile(p.modLoc("block/breeze_chamber/cooler")));
                }else {
                    model.set(p.models().getExistingFile(p.modLoc("block/breeze_chamber/block")));
                }
                return ConfiguredModel.builder().modelFile(model.get()).build();
            }, BlockStateProperties.WATERLOGGED);
        }).onRegister(movementBehaviour(new BreezeChamberMovementBehaviour())).onRegister(interactionBehaviour(new BreezeChamberConductor.BreezeChamber())).item().model(AssetLookup.customBlockItemModel("breeze_chamber", "block_with_breeze")).build();
    }

    public static <B extends Block> NonNullUnaryOperator<BlockBuilder<B, CCBRegistrate>> gas_injection_chamber() {
        return b -> b.addLayer(() -> RenderType::translucent).blockstate((c, p) -> {
            ModelFile model = p.models().getExistingFile(p.modLoc("block/gas_injection_chamber/block"));
            p.getVariantBuilder(c.getEntry()).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(model).build());
        }).item().transform(ib -> ib.model(AssetLookup::customItemModel)).build();
    }

    private static <T extends Block> Function<BlockState, ModelFile> getBlockModel(DataGenContext<Block, T> c, RegistrateBlockstateProvider p) {
        return $ -> AssetLookup.partialBaseModel(c, p);
    }
}
