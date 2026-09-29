package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.content.processing.AssemblyOperatorBlockItem;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.ModelGen;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.ty.createcraftedbeginning.config.CCBStress;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockItem;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerCTBehaviour;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockItem;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralCogBlock;
import net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet.BoilerSteamOutletBlock;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlock;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlockItem;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineStructuralBlock.TeslaTurbineStructuralPosition;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlock;
import net.ty.createcraftedbeginning.foundation.block.CCBSharedProperties;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightMachineRegistration {
    private AirtightMachineRegistration() {
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightFractionationTower() {
        return builder -> builder.blockstate((context, provider) -> provider.simpleBlock(context.getEntry(), AssetLookup.standardModel(context, provider))).onRegister(CreateRegistrate.connectedTextures(AirtightFractionationTowerCTBehaviour::new)).tag(AllBlockTags.NON_MOVABLE.tag).loot((loot, block) -> loot.dropOther(block, CCBBlocks.AIRTIGHT_TANK_BLOCK.get()));
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> pneumaticEngine() {
        return builder -> builder.blockstate((context, provider) -> provider.simpleBlock(context.getEntry(), AssetLookup.partialBaseModel(context, provider))).item().transform(ModelGen.customItemModel("pneumatic_engine", "item"));
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightEngine() {
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
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> residueOutlet() {
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
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> boilerSteamOutlet() {
        return builder -> builder.blockstate((context, provider) -> {
            ModelFile model = provider.models().getExistingFile(provider.modLoc("block/boiler_steam_outlet/block"));
            Block block = context.get();
            provider.getVariantBuilder(block).forAllStatesExcept(state -> {
                AttachFace face = state.getValue(BoilerSteamOutletBlock.FACE);
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                int rotationX = face.ordinal() * 90;
                int rotationY = ((int) facing.toYRot() + 180 + (face == AttachFace.CEILING ? 180 : 0)) % 360;
                return ConfiguredModel.builder().modelFile(model).rotationX(rotationX).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED, BoilerSteamOutletBlock.POWERED);
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
                int rotationY = BlockModelRotation.horizontal(facing);
                return ConfiguredModel.builder().modelFile(model).rotationX(rotationX).rotationY(rotationY).build();
            }, BlockStateProperties.WATERLOGGED, TeslaTurbineNozzleBlock.CLOCKWISE);
        }).item().tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> teslaTurbine() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
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
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightReactorKettle() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc("block/airtight_reactor_kettle/block"))).build())).item(AirtightReactorKettleBlockItem::new).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightForgingPress() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc("block/airtight_forging_press/block"))).build())).item(AirtightForgingPressBlockItem::new).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> teslaTurbineStructural() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.get()).forAllStates(state -> {
            Axis axis = state.getValue(TeslaTurbineStructuralBlock.AXIS);
            TeslaTurbineStructuralPosition position = state.getValue(TeslaTurbineStructuralBlock.STRUCTURAL_POSITION);
            String modelPath = String.format("block/tesla_turbine/%s", position.getSerializedName());
            ModelFile model = provider.models().getExistingFile(provider.modLoc(modelPath));
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
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightReactorKettleStructural() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.get()).forAllStates(state -> {
            String position = state.getValue(AirtightReactorKettleStructuralBlock.STRUCTURAL_POSITION).getSerializedName();
            String modelPath = String.format("block/airtight_reactor_kettle/%s", position);
            return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc(modelPath))).build();
        })).lang("Airtight Reactor Kettle");
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightReactorKettleStructuralCog() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.get()).forAllStates(state -> {
            String position = state.getValue(AirtightReactorKettleStructuralCogBlock.STRUCTURAL_POSITION).getSerializedName();
            String modelPath = String.format("block/airtight_reactor_kettle/%s", position);
            return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc(modelPath))).build();
        })).lang("Airtight Reactor Kettle");
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightForgingPressStructural() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.get()).forAllStates(state -> {
            String position = state.getValue(AirtightForgingPressStructuralBlock.STRUCTURAL_POSITION).getSerializedName();
            String modelPath = String.format("block/airtight_forging_press/%s", position);
            return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc(modelPath))).build();
        })).lang("Airtight Forging Press");
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> airtightForgingPressStructuralShaft() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.get()).forAllStates(state -> {
            String position = state.getValue(AirtightForgingPressStructuralShaftBlock.STRUCTURAL_POSITION).getSerializedName();
            String modelPath = String.format("block/airtight_forging_press/%s", position);
            return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc(modelPath))).build();
        })).lang("Airtight Forging Press");
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> gasInjectionChamber() {
        return builder -> builder.blockstate((context, provider) -> provider.getVariantBuilder(context.getEntry()).forAllStatesExcept(state -> {
            Direction facing = state.getValue(GasInjectionChamberBlock.FACING);
            int rotationY = BlockModelRotation.horizontal(facing);
            return ConfiguredModel.builder().modelFile(provider.models().getExistingFile(provider.modLoc("block/gas_injection_chamber/block"))).rotationY(rotationY).build();
        })).item(AssemblyOperatorBlockItem::new).properties(Properties::fireResistant).tag(CCBItemTags.AIRTIGHT_COMPONENTS.tag).transform(itemBuilder -> itemBuilder.model(AssetLookup::customItemModel)).build();
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> boilerSteamOutletProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::copperMetal).tag(BlockTags.MINEABLE_WITH_PICKAXE).properties(BlockBehaviour.Properties::noOcclusion).tag(CCBBlockTags.AIRTIGHT_COMPONENTS.tag);
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> pneumaticEngineProperties() {
        return builder -> builder.initialProperties(CCBSharedProperties::copperMetal).tag(BlockTags.MINEABLE_WITH_PICKAXE).transform(CCBStress.setCapacity(6)).properties(properties -> properties.mapColor(MapColor.COLOR_ORANGE).noOcclusion());
    }

    @Contract(pure = true)
    public static <B extends Block> @NotNull NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> teslaTurbineProperties() {
        return AirtightBlockProperties.airtightComponentWithCapacity(TeslaTurbineBlock.BASE_STRESS_CAPACITY);
    }
}
