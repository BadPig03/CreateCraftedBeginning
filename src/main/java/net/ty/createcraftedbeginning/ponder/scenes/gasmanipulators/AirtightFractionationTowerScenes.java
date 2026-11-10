package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerInventory;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerSerialization;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeConnections;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.gas.multiblock.GasTankMultiblockConnectivity;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightFractionationTowerScenes {
    private static final String COMPOUND_KEY_TANK_CONTENT = "TankContent";
    private static final int PREVIEW_FLUID_CAPACITY = 10000;
    private static final long PREVIEW_GAS_VOLUME = 10000;

    public static void placement(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_fractionation_tower_placement", "Building an Airtight Fractionation Tower");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.75F);
        scene.showBasePlate();

        BlockPos origin = util.grid().at(2, 1, 2);
        BlockPos centerPos = origin.east().south();
        BlockPos middlePos = origin.east().above().south();
        BlockPos topPos = origin.east().above(2).south();
        BlockPos middleTankOriginPos = origin.above(4);
        BlockPos topTankOriginPos = origin.above(8);
        BlockPos pipePos = origin.west().south();
        BlockPos fluidPipePos = origin.north().east();
        BlockPos funnelPos = origin.north().east(2);
        BlockPos relocatedPipePos = origin.west().south(2);
        BlockPos middleFunnelPos = origin.east(2).above().north();

        Selection bottomSelection = util.select().fromTo(origin, origin.east(2).south(2));
        Selection middleSelection = util.select().fromTo(origin.above(), origin.east(2).above().south(2));
        Selection topSelection = util.select().fromTo(origin.above(2), origin.east(2).above(2).south(2));
        Selection extensionSelection = util.select().fromTo(origin.above(3), origin.east(2).above(8).south(2));
        Selection fluidPipeSelection = util.select().position(fluidPipePos);
        Selection funnelSelection = util.select().position(funnelPos);
        Selection relocatedPipeSelection = util.select().position(relocatedPipePos);
        Selection middleFunnelSelection = util.select().position(middleFunnelPos);
        Selection[] extensionLayerSelections = {util.select().fromTo(origin.above(3), origin.east(2).above(3).south(2)), util.select().fromTo(origin.above(4), origin.east(2).above(4).south(2)), util.select().fromTo(origin.above(5), origin.east(2).above(5).south(2)), util.select().fromTo(origin.above(6), origin.east(2).above(6).south(2)), util.select().fromTo(origin.above(7), origin.east(2).above(7).south(2)), util.select().fromTo(origin.above(8), origin.east(2).above(8).south(2))};

        Vec3 bottomVec = util.vector().centerOf(centerPos);
        Vec3 middleVec = util.vector().centerOf(middlePos);
        Vec3 topVec = util.vector().centerOf(topPos);
        Vec3 panelVec = util.vector().blockSurface(origin, Direction.NORTH);
        Vec3 middleFunnelVec = util.vector().centerOf(middleFunnelPos);

        AABB towerStartArea = new AABB(bottomVec, bottomVec).inflate(0.5);
        AABB bottomArea = new AABB(bottomVec, bottomVec);
        AABB middleArea = new AABB(middleVec, middleVec);
        AABB topArea = new AABB(topVec, topVec);

        Object bottomObject = new Object();
        Object towerObject = new Object();
        Object middleObject = new Object();
        Object topObject = new Object();

        ItemStack panelItem = new ItemStack(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get());
        ItemStack naturalAirCanister = new ItemStack(CCBItems.GAS_CANISTER.get());
        naturalAirCanister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        ItemStack ironIngotItem = new ItemStack(Items.IRON_INGOT);

        configureTanks(scene, util, origin, 3);

        scene.idle(20);
        scene.world().showSection(bottomSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, bottomObject, bottomArea, 3);

        scene.idle(3);
        bottomArea = bottomArea.inflate(1.5, 0.5, 1.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, bottomObject, bottomArea, 60);
        scene.overlay().showText(60).text("Each layer requires a solid 3x3 square of tanks").pointAt(bottomArea.getCenter()).placeNearTarget().attachKeyFrame();

        scene.idle(57);
        scene.world().showSection(middleSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(topSelection, Direction.DOWN);

        scene.idle(20);
        AABB towerArea = bottomArea;
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, towerObject, towerStartArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, towerObject, towerArea, 27);

        scene.idle(2);
        for (BlockPos pos : middleSelection) {
            scene.effects().indicateSuccess(pos.immutable());
        }
        towerArea = towerArea.expandTowards(0, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, towerObject, towerArea, 30);

        scene.idle(5);
        for (BlockPos pos : topSelection) {
            scene.effects().indicateSuccess(pos.immutable());
        }
        towerArea = towerArea.expandTowards(0, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, towerObject, towerArea, 60);
        scene.overlay().showText(60).text("The tower requires between 3 and 9 tank layers").pointAt(towerArea.getCenter()).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        configureTanks(scene, util, origin, 4);
        configureTanks(scene, util, middleTankOriginPos, 4);
        configureTanks(scene, util, topTankOriginPos, 1);
        AABB extensionArea = towerArea;
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, towerObject, extensionArea, 20);
        for (int layerIndex = 0; layerIndex < extensionLayerSelections.length; layerIndex++) {
            Selection layerSelection = extensionLayerSelections[layerIndex];
            scene.world().showSection(layerSelection, Direction.DOWN);
            if (layerIndex < extensionLayerSelections.length - 1) {
                scene.idle(3);
            }
            else {
                scene.idle(20);
            }
            for (BlockPos pos : layerSelection) {
                scene.effects().indicateSuccess(pos.immutable());
            }
            extensionArea = extensionArea.expandTowards(0, 1, 0);
            scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, towerObject, extensionArea, 30);
        }

        scene.idle(38);
        scene.world().hideSection(extensionSelection, Direction.UP);

        scene.idle(20);
        scene.world().setBlocks(extensionSelection, Blocks.AIR.defaultBlockState(), false);
        configureTanks(scene, util, origin, 3);
        scene.world().modifyBlockEntity(origin, AirtightTankBlockEntity.class, tank -> tank.getTankInventory().fill(new GasStack(CCBGases.NATURAL_AIR.get(), 1000), GasAction.EXECUTE));
        scene.overlay().showControls(topVec, Pointing.DOWN, 60).withItem(naturalAirCanister.copy());
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, towerObject, new AABB(middleVec, middleVec), 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, towerObject, towerArea, 60);
        scene.overlay().showText(60).text("Stored gas prevents the tanks from being assembled").colored(PonderPalette.RED).pointAt(towerArea.getCenter()).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(panelVec, Pointing.RIGHT, 60).rightClick().withItem(panelItem.copy()).showing(AllIcons.I_MTD_CLOSE);

        scene.idle(80);
        scene.world().modifyBlockEntity(origin, AirtightTankBlockEntity.class, tank -> tank.getTankInventory().drain(1000, GasAction.EXECUTE));
        scene.overlay().showControls(panelVec, Pointing.RIGHT, 60).rightClick().withItem(panelItem.copy());
        scene.overlay().showText(60).text("Right-click empty tanks with the Instrument Panel to assemble them").colored(PonderPalette.BLUE).pointAt(panelVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        assembleTower(scene, util, origin);
        scene.effects().indicateSuccess(origin);

        scene.idle(70);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, bottomObject, new AABB(bottomVec, bottomVec), 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, bottomObject, bottomArea, 60);
        scene.overlay().showText(60).text("Each layer shares its Item, Fluid, and Gas storage").pointAt(bottomArea.getCenter().add(0, -1, 0)).placeNearTarget().attachKeyFrame();

        scene.idle(77);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, middleObject, middleArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, topObject, topArea, 3);

        scene.idle(3);
        middleArea = middleArea.inflate(1.5, 0.5, 1.5);
        topArea = topArea.inflate(1.5, 0.5, 1.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, middleObject, middleArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, topObject, topArea, 60);
        scene.overlay().showText(60).text("Contents do not move between layers on their own").pointAt(middleArea.getCenter()).placeNearTarget().attachKeyFrame();

        scene.idle(77);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, bottomObject, new AABB(bottomVec, bottomVec), 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, topObject, new AABB(topVec, topVec), 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, bottomObject, bottomArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, topObject, topArea, 60);
        scene.overlay().showText(60).text("Only the top and bottom layers accept external inputs").colored(PonderPalette.INPUT).pointAt(bottomArea.getCenter()).placeNearTarget().attachKeyFrame();

        scene.idle(40);
        scene.world().showSection(fluidPipeSelection, Direction.SOUTH);

        scene.idle(20);
        scene.world().showSection(funnelSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().showText(60).text("Any block in a layer provides the same storage access").colored(PonderPalette.GREEN).pointAt(bottomVec).placeNearTarget().attachKeyFrame();

        scene.idle(27);
        scene.world().setBlock(pipePos, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(relocatedPipePos, AirtightPipeConnections.straight(CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState(), Axis.X), false);
        scene.world().showSection(relocatedPipeSelection, Direction.EAST);

        scene.idle(70);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, middleObject, new AABB(middleVec, middleVec), 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, middleObject, middleArea, 60);
        scene.overlay().showText(60).text("Intermediate layers are output-only interfaces").colored(PonderPalette.RED).pointAt(middleArea.getCenter()).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().setBlock(middleFunnelPos, AllBlocks.ANDESITE_FUNNEL.getDefaultState().setValue(FunnelBlock.FACING, Direction.NORTH).setValue(FunnelBlock.EXTRACTING, false), false);
        scene.world().showSection(middleFunnelSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().showControls(middleFunnelVec, Pointing.RIGHT, 30).withItem(ironIngotItem.copy()).showing(AllIcons.I_MTD_CLOSE);

        scene.idle(33);
        scene.markAsFinished();
    }

    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_fractionation_tower_processing", "Fractionating and Condensing with an Airtight Fractionation Tower");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos origin = util.grid().at(2, 2, 2);
        BlockPos bottomPos = origin.east().south();
        BlockPos middlePos = origin.east().above().south();
        BlockPos topPos = origin.east().above(2).south();
        BlockPos burnerPos = origin.below();
        BlockPos inputPos = origin.west().south();
        BlockPos firstOutputPos = origin.north().east().above();
        BlockPos secondOutputPos = origin.east().above(2).north();
        BlockPos brokenPos = origin.east();
        BlockPos condensationInputPos = origin.west().above(2).south();
        BlockPos lowerOutputPos = origin.east().north();
        BlockPos inputFunnelPos = origin.east().north();

        Selection towerSelection = util.select().fromTo(origin, origin.east(2).above(2).south(2));
        Selection remainingTowerSelection = towerSelection.copy().substract(util.select().position(brokenPos));
        Selection heatSelection = util.select().fromTo(origin.below(), origin.east(2).below().south(2));
        Selection inputSelection = util.select().position(inputPos);
        Selection firstOutputSelection = util.select().position(firstOutputPos);
        Selection secondOutputSelection = util.select().position(secondOutputPos);
        Selection condensationInputSelection = util.select().position(condensationInputPos);
        Selection lowerOutputSelection = util.select().position(lowerOutputPos);
        Selection inputFunnelSelection = util.select().position(inputFunnelPos);

        Vec3 bottomVec = util.vector().centerOf(bottomPos);
        Vec3 middleVec = util.vector().centerOf(middlePos);
        Vec3 topVec = util.vector().centerOf(topPos);
        Vec3 burnerVec = util.vector().centerOf(burnerPos);
        Vec3 inputVec = util.vector().centerOf(inputPos);
        Vec3 firstOutputVec = util.vector().centerOf(firstOutputPos);
        Vec3 secondOutputVec = util.vector().centerOf(secondOutputPos);
        Vec3 brokenVec = util.vector().centerOf(brokenPos);
        Vec3 inputFunnelVec = util.vector().centerOf(inputFunnelPos);

        AABB heatArea = new AABB(bottomVec.add(0, -1, 0), bottomVec.add(0, -1, 0));
        AABB bottomArea = new AABB(bottomVec, bottomVec);
        AABB middleArea = new AABB(middleVec, middleVec);
        AABB topArea = new AABB(topVec, topVec);
        AABB towerArea = new AABB(middleVec, middleVec);

        Object heatObject = new Object();
        Object inputObject = new Object();
        Object firstOutputObject = new Object();
        Object secondOutputObject = new Object();
        Object towerObject = new Object();

        ItemStack brimstoneBucket = new ItemStack(CCBFluids.BRIMSTONE.get().getBucket());
        ItemStack ultrawarmAirCanister = new ItemStack(CCBItems.GAS_CANISTER.get());
        ultrawarmAirCanister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.ULTRAWARM_AIR.get(), 1));
        ItemStack moistAirCanister = new ItemStack(CCBItems.GAS_CANISTER.get());
        moistAirCanister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.MOIST_AIR.get(), 1));
        ItemStack naturalAirCanister = new ItemStack(CCBItems.GAS_CANISTER.get());
        naturalAirCanister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());
        ItemStack blazeCakeItem = new ItemStack(AllItems.BLAZE_CAKE.asItem());
        ItemStack ironIngotItem = new ItemStack(Items.IRON_INGOT);
        ItemStack lavaBucketItem = new ItemStack(Items.LAVA_BUCKET);
        ItemStack packedIceItem = new ItemStack(Items.PACKED_ICE);
        ItemStack waterBucketItem = new ItemStack(Items.WATER_BUCKET);
        ItemStack panelItem = new ItemStack(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get());
        ItemStack airtightTankItem = new ItemStack(CCBBlocks.AIRTIGHT_TANK_BLOCK.get());

        configureTanks(scene, util, origin, 3);
        assembleTower(scene, util, origin);

        scene.idle(20);
        scene.world().showSection(heatSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(towerSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, heatObject, heatArea, 3);

        scene.idle(3);
        heatArea = heatArea.inflate(1.5, 0.5, 1.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, heatObject, heatArea, 60);
        scene.overlay().showText(60).text("Temperature effects combine within the 3x3 area beneath the tower").pointAt(heatArea.getCenter()).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Brimstone fractionation requires Superheated conditions").pointAt(burnerVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(burnerVec, Pointing.RIGHT, 60).rightClick().withItem(blazeCakeItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(burnerPos, state -> state.setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SEETHING), false);

        scene.idle(70);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputObject, bottomArea, 3);

        scene.idle(3);
        bottomArea = bottomArea.inflate(1.5, 0.5, 1.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputObject, bottomArea, 60);
        scene.overlay().showText(60).text("Fractionation uses the bottom layer for ingredients").colored(PonderPalette.INPUT).pointAt(bottomArea.getCenter()).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().showSection(inputSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().showControls(inputVec, Pointing.RIGHT, 53).withItem(brimstoneBucket.copy());
        setLayerContents(scene, util, bottomPos, ItemStack.EMPTY, new FluidStack(CCBFluids.BRIMSTONE.get().getSource(), 100), GasStack.EMPTY);

        scene.idle(53);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, firstOutputObject, middleArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, secondOutputObject, topArea, 3);

        scene.idle(3);
        middleArea = middleArea.inflate(1.5, 0.5, 1.5);
        topArea = topArea.inflate(1.5, 0.5, 1.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, firstOutputObject, middleArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, secondOutputObject, topArea, 60);

        scene.overlay().showText(60).text("Fractionation assigns products to layers above the input").colored(PonderPalette.OUTPUT).pointAt(middleArea.getCenter()).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        setLayerContents(scene, util, bottomPos, ItemStack.EMPTY, FluidStack.EMPTY, GasStack.EMPTY);
        setLayerContents(scene, util, middlePos, ItemStack.EMPTY, new FluidStack(Fluids.LAVA, 500), GasStack.EMPTY);
        setLayerContents(scene, util, topPos, ItemStack.EMPTY, FluidStack.EMPTY, new GasStack(CCBGases.ULTRAWARM_AIR.get(), 500));
        scene.overlay().showControls(firstOutputVec, Pointing.UP, 53).withItem(lavaBucketItem.copy());
        scene.overlay().showControls(secondOutputVec, Pointing.RIGHT, 53).withItem(ultrawarmAirCanister.copy());

        scene.idle(73);
        scene.overlay().showText(60).text("Each product must be extracted from its assigned layer").colored(PonderPalette.OUTPUT).pointAt(middleVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().showSection(firstOutputSelection, Direction.SOUTH);

        scene.idle(20);
        scene.world().showSection(secondOutputSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().showControls(firstOutputVec, Pointing.UP, 53).withItem(lavaBucketItem.copy());
        scene.overlay().showControls(secondOutputVec, Pointing.RIGHT, 53).withItem(ultrawarmAirCanister.copy());

        scene.idle(53);
        scene.world().hideSection(inputSelection, Direction.WEST);
        scene.world().hideSection(firstOutputSelection, Direction.NORTH);
        scene.world().hideSection(secondOutputSelection, Direction.NORTH);
        setLayerContents(scene, util, middlePos, ItemStack.EMPTY, FluidStack.EMPTY, GasStack.EMPTY);
        setLayerContents(scene, util, topPos, ItemStack.EMPTY, FluidStack.EMPTY, GasStack.EMPTY);

        scene.idle(20);
        scene.world().setBlock(inputPos, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(firstOutputPos, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(secondOutputPos, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(burnerPos, CCBBlocks.BREEZE_COOLER_BLOCK.getDefaultState(), true);
        scene.overlay().showText(60).text("Moist Air condensation requires Chilled conditions").pointAt(burnerVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(burnerVec, Pointing.RIGHT, 60).rightClick().withItem(packedIceItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(burnerPos, state -> state.setValue(BreezeCoolerBlock.FROST_LEVEL, FrostLevel.CHILLED), false);
        scene.world().modifyBlockEntity(burnerPos, BreezeCoolerBlockEntity.class, BreezeCoolerBlockEntity::switchToChilledState);

        scene.idle(70);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputObject, new AABB(topVec, topVec), 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputObject, topArea, 60);
        scene.overlay().showText(60).text("Condensation uses the top layer for ingredients").colored(PonderPalette.INPUT).pointAt(topArea.getCenter()).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().setBlock(condensationInputPos, AirtightPipeConnections.straight(CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState(), Axis.X), false);
        scene.world().showSection(condensationInputSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().showControls(inputVec.add(0, 2, 0), Pointing.RIGHT, 53).withItem(moistAirCanister.copy());
        setLayerContents(scene, util, topPos, ItemStack.EMPTY, FluidStack.EMPTY, new GasStack(CCBGases.MOIST_AIR.get(), 10000));

        scene.idle(53);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, firstOutputObject, new AABB(middleVec, middleVec), 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, secondOutputObject, new AABB(bottomVec, bottomVec), 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, firstOutputObject, middleArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, secondOutputObject, bottomArea, 60);
        scene.overlay().showText(60).text("Condensation assigns products to layers below the input").colored(PonderPalette.OUTPUT).pointAt(middleArea.getCenter()).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        setLayerContents(scene, util, topPos, ItemStack.EMPTY, FluidStack.EMPTY, GasStack.EMPTY);
        setLayerContents(scene, util, middlePos, ItemStack.EMPTY, FluidStack.EMPTY, new GasStack(CCBGases.NATURAL_AIR.get(), 9800));
        setLayerContents(scene, util, bottomPos, ItemStack.EMPTY, new FluidStack(Fluids.WATER, 100), GasStack.EMPTY);
        scene.overlay().showControls(firstOutputVec, Pointing.RIGHT, 53).withItem(naturalAirCanister.copy());
        scene.overlay().showControls(firstOutputVec.add(0, -1, 0), Pointing.UP, 53).withItem(waterBucketItem.copy());

        scene.idle(73);
        scene.overlay().showText(60).text("Fluid and Gas products use separate transport networks").colored(PonderPalette.OUTPUT).pointAt(middleVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().setBlock(firstOutputPos, AirtightPipeConnections.straight(CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState(), Axis.Z), false);
        scene.world().setBlock(origin.east().north(), AllBlocks.GLASS_FLUID_PIPE.getDefaultState().setValue(GlassFluidPipeBlock.AXIS, Axis.Z), false);
        scene.world().showSection(firstOutputSelection, Direction.SOUTH);

        scene.idle(20);
        scene.world().showSection(lowerOutputSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().showControls(firstOutputVec, Pointing.RIGHT, 53).withItem(naturalAirCanister.copy());
        scene.overlay().showControls(firstOutputVec.add(0, -1, 0), Pointing.UP, 53).withItem(waterBucketItem.copy());

        scene.idle(73);
        scene.world().hideSection(condensationInputSelection, Direction.WEST);
        scene.world().hideSection(firstOutputSelection, Direction.NORTH);
        scene.world().hideSection(lowerOutputSelection, Direction.NORTH);

        scene.idle(20);
        scene.world().setBlock(origin.west().south().above(2), Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(firstOutputPos, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(origin.east().north(), Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(burnerPos, AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState(), true);
        scene.world().setBlock(inputFunnelPos, AllBlocks.ANDESITE_FUNNEL.getDefaultState().setValue(FunnelBlock.FACING, Direction.NORTH).setValue(FunnelBlock.EXTRACTING, false), false);
        scene.world().showSection(inputFunnelSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().showControls(inputFunnelVec, Pointing.RIGHT, 30).withItem(ironIngotItem.copyWithCount(4));
        scene.world().flapFunnel(origin.east().north(), false);
        setLayerContents(scene, util, bottomPos, ironIngotItem.copyWithCount(4), new FluidStack(Fluids.WATER, 100), GasStack.EMPTY);

        scene.idle(40);
        scene.world().hideSection(inputFunnelSelection, Direction.NORTH);

        scene.idle(20);
        scene.world().setBlock(origin.east().north(), Blocks.AIR.defaultBlockState(), false);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, towerObject, towerArea, 3);

        scene.idle(3);
        towerArea = towerArea.inflate(1.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, towerObject, towerArea, 60);
        scene.overlay().showText(60).text("Removing any tower block dismantles the entire structure").colored(PonderPalette.RED).pointAt(towerArea.getCenter()).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Stored Items drop alongside the Instrument Panel at the removal point").pointAt(brokenVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Dismantling destroys stored Fluids and releases stored Gases").colored(PonderPalette.RED).pointAt(middleVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(brokenVec, Pointing.RIGHT, 30).rightClick().whileSneaking().withItem(wrenchItem.copy());

        scene.idle(7);
        scene.world().destroyBlock(brokenPos);
        scene.world().replaceBlocks(remainingTowerSelection, CCBBlocks.AIRTIGHT_TANK_BLOCK.getDefaultState(), false);
        scene.addInstruction(ponderScene -> {
            Level level = ponderScene.getWorld();
            for (BlockPos pos : remainingTowerSelection) {
                if (!(level.getBlockEntity(pos) instanceof AirtightTankBlockEntity tank)) {
                    continue;
                }

                GasTankMultiblockConnectivity.formMultiblock(tank, level);
            }
            ponderScene.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
        });
        scene.world().createItemEntity(brokenVec, new Vec3(-0.1, 0.15, -0.1), panelItem.copy());
        scene.world().createItemEntity(brokenVec, new Vec3(0.1, 0.15, -0.1), ironIngotItem.copyWithCount(4));
        scene.world().createItemEntity(brokenVec, new Vec3(0, 0.15, -0.15), airtightTankItem.copy());
        scene.effects().emitParticles(middleVec, scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0, 0.04, -0.05)), 1, 40);

        scene.idle(53);
        scene.markAsFinished();
    }

    private static void configureTanks(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos origin, int height) {
        Selection tanks = util.select().fromTo(origin, origin.east(2).above(height - 1).south(2));
        for (BlockPos pos : tanks) {
            scene.world().modifyBlockEntity(pos.immutable(), AirtightTankBlockEntity.class, tank -> {
                tank.setWidth(3);
                tank.setHeight(height);
                tank.setController(origin);
                tank.notifyMultiUpdated();
            });
        }
        scene.addInstruction(ponderScene -> ponderScene.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw));
    }

    private static void assembleTower(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos origin) {
        Selection tower = util.select().fromTo(origin, origin.east(2).above(2).south(2));
        scene.world().modifyBlocks(tower, state -> CCBBlocks.AIRTIGHT_FRACTIONATION_TOWER_BLOCK.get().withPropertiesOf(state), false);
        scene.world().modifyBlockEntityNBT(tower, AirtightFractionationTowerBlockEntity.class, compoundTag -> {
            compoundTag.putLong(AirtightFractionationTowerSerialization.COMPOUND_KEY_ORIGIN, origin.asLong());
            compoundTag.putInt(AirtightFractionationTowerSerialization.COMPOUND_KEY_HEIGHT, 3);
        }, true);
    }

    private static void setLayerContents(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos center, ItemStack item, FluidStack fluid, GasStack gas) {
        Selection layerSelection = util.select().position(center);

        scene.world().modifyBlockEntityNBT(layerSelection, AirtightFractionationTowerBlockEntity.class, compoundTag -> {
            Provider provider = scene.world().getHolderLookupProvider();
            ItemStackHandler items = new ItemStackHandler(1);
            items.setStackInSlot(0, item.copy());
            FluidTank fluidTank = new FluidTank(PREVIEW_FLUID_CAPACITY);
            fluidTank.setFluid(fluid.copy());
            GasTank gasTank = new GasTank(PREVIEW_GAS_VOLUME);
            gasTank.fill(gas.copy(), GasAction.EXECUTE);
            CompoundTag inventory = new CompoundTag();
            inventory.putInt(AirtightFractionationTowerInventory.COMPOUND_KEY_ITEM_SLOTS, 1);
            inventory.putInt(AirtightFractionationTowerInventory.COMPOUND_KEY_FLUID_TANKS, 1);
            inventory.putInt(AirtightFractionationTowerInventory.COMPOUND_KEY_GAS_TANKS, 1);
            inventory.putInt(AirtightFractionationTowerInventory.COMPOUND_KEY_FLUID_CAPACITY, PREVIEW_FLUID_CAPACITY);
            inventory.putLong(AirtightFractionationTowerInventory.COMPOUND_KEY_GAS_VOLUME, PREVIEW_GAS_VOLUME);
            inventory.put(AirtightFractionationTowerInventory.COMPOUND_KEY_ITEMS, items.serializeNBT(provider));
            CompoundTag fluidTag = new CompoundTag();
            fluidTag.put(COMPOUND_KEY_TANK_CONTENT, fluidTank.writeToNBT(provider, new CompoundTag()));
            ListTag fluids = new ListTag();
            fluids.add(fluidTag);
            inventory.put(AirtightFractionationTowerInventory.COMPOUND_KEY_LAYER_FLUID_TANKS, fluids);
            CompoundTag gasTag = new CompoundTag();
            gasTag.put(COMPOUND_KEY_TANK_CONTENT, gasTank.write(provider, new CompoundTag()));
            ListTag gases = new ListTag();
            gases.add(gasTag);
            inventory.put(AirtightFractionationTowerInventory.COMPOUND_KEY_LAYER_GAS_TANKS, gases);
            compoundTag.put(AirtightFractionationTowerInventory.COMPOUND_KEY_INVENTORY, inventory);
        });
    }
}
