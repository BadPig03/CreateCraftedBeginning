package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleScenes {
    public static void placement(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_reactor_kettle_placement", "Setting Up an Airtight Reactor Kettle");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos centerPos = util.grid().at(3, 1, 3);
        BlockPos leftUpPos = centerPos.above(2).east().south();
        BlockPos industrialIronPos = leftUpPos.below(2);
        BlockPos realCenterPos = centerPos.above();
        BlockPos rightDownPos = centerPos.west().north();
        BlockPos bottomFarPos = rightDownPos.east(2).south(2);
        BlockPos cogCenterPos = centerPos.above(2);
        BlockPos westCogPos = cogCenterPos.west(2);
        BlockPos southCogPos = cogCenterPos.south(2);
        BlockPos eastCogPos = cogCenterPos.east(2);
        BlockPos northCogPos = cogCenterPos.north(2);
        BlockPos motorPos = westCogPos.above();
        BlockPos funnelPos = rightDownPos.west();
        BlockPos hatchPos = funnelPos.south(2);
        BlockPos fluidPipePos = rightDownPos.north();
        BlockPos airtightPipePos = fluidPipePos.east(2);

        Selection kettleSelection = util.select().fromTo(leftUpPos, rightDownPos);
        Selection industrialIronSelection = util.select().position(industrialIronPos);
        Selection westCogSelection = util.select().position(westCogPos);
        Selection southCogSelection = util.select().position(southCogPos);
        Selection eastCogSelection = util.select().position(eastCogPos);
        Selection northCogSelection = util.select().position(northCogPos);
        Selection motorSelection = util.select().position(motorPos);
        Selection kettleCogsSelection = util.select().fromTo(leftUpPos, rightDownPos.above(2));
        Selection fluidPipeSelection = util.select().position(fluidPipePos);
        Selection airtightPipeSelection = util.select().position(airtightPipePos);
        Selection funnelSelection = util.select().position(funnelPos);
        Selection hatchSelection = util.select().position(hatchPos);

        Vec3 realCenterVec = util.vector().centerOf(realCenterPos);
        Vec3 westCogVec = util.vector().centerOf(westCogPos);
        Vec3 bottomCenterVec = util.vector().centerOf(centerPos);
        Vec3 fluidPipeVec = util.vector().centerOf(fluidPipePos);
        Vec3 airtightPipeVec = util.vector().centerOf(airtightPipePos);
        Vec3 hatchVec = util.vector().centerOf(hatchPos);

        AABB kettleArea = new AABB(realCenterVec, realCenterVec);
        AABB cogArea = new AABB(westCogVec, westCogVec);
        AABB bottomArea = new AABB(util.vector().centerOf(rightDownPos), util.vector().centerOf(bottomFarPos));
        AABB fluidPipeArea = new AABB(fluidPipeVec, fluidPipeVec);
        AABB airtightPipeArea = new AABB(airtightPipeVec, airtightPipeVec);

        Object kettleObject = new Object();
        Object cogObject = new Object();
        Object bottomObject = new Object();
        Object fluidPipeObject = new Object();
        Object airtightPipeObject = new Object();

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();
        float fastSpeed = SpeedLevel.FAST.getSpeedValue();

        scene.idle(20);
        scene.world().setBlock(industrialIronPos, AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState(), false);
        scene.world().showSection(industrialIronSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, kettleObject, kettleArea, 3);

        scene.idle(3);
        kettleArea = kettleArea.inflate(1.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, kettleObject, kettleArea, 60);
        scene.overlay().showText(60).text("An Airtight Reactor Kettle automatically forms its 3x3x3 structure when placed").pointAt(realCenterVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, kettleObject, kettleArea, 60);
        scene.overlay().showText(60).text("The entire 3x3x3 area must be clear before placement").colored(PonderPalette.RED).pointAt(realCenterVec).placeNearTarget().attachKeyFrame();

        scene.idle(45);
        scene.world().hideSection(industrialIronSelection, Direction.UP);

        scene.idle(20);
        scene.world().showSection(kettleSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(northCogSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(eastCogSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(southCogSelection, Direction.NORTH);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(motorSelection, Direction.EAST);
        scene.world().showSection(westCogSelection, Direction.EAST);

        scene.idle(15);
        scene.world().setKineticSpeed(motorSelection, mediumSpeed);
        scene.world().setKineticSpeed(westCogSelection, mediumSpeed);
        scene.world().setKineticSpeed(eastCogSelection, mediumSpeed);
        scene.world().setKineticSpeed(northCogSelection, mediumSpeed);
        scene.world().setKineticSpeed(southCogSelection, mediumSpeed);
        scene.world().setKineticSpeed(kettleCogsSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, cogObject, cogArea, 3);

        scene.idle(3);
        cogArea = cogArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, cogObject, cogArea, 60);
        scene.overlay().showText(60).text("Supply rotational power through the cogwheel interfaces on top").pointAt(westCogVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, cogObject, cogArea, 60);
        scene.overlay().showText(60).text("The Reactor Kettle requires at least Fast speed to operate").colored(PonderPalette.RED).pointAt(westCogVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setKineticSpeed(motorSelection, fastSpeed);
        scene.world().setKineticSpeed(westCogSelection, fastSpeed);
        scene.world().setKineticSpeed(eastCogSelection, fastSpeed);
        scene.world().setKineticSpeed(northCogSelection, fastSpeed);
        scene.world().setKineticSpeed(southCogSelection, fastSpeed);
        scene.world().setKineticSpeed(kettleCogsSelection, -fastSpeed);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.overlay().showText(60).text("Higher rotational speed shortens the processing time of timed Reactor Kettle recipes").colored(PonderPalette.GREEN).pointAt(westCogVec).placeNearTarget().attachKeyFrame();

        scene.idle(77);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, bottomObject, bottomArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, bottomObject, bottomArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("The bottom 3x3 layer is the shared Item, Fluid, and Gas interface").pointAt(bottomCenterVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(fluidPipeSelection, Direction.SOUTH);
        scene.world().showSection(airtightPipeSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, fluidPipeObject, fluidPipeArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, airtightPipeObject, airtightPipeArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, fluidPipeObject, fluidPipeArea.inflate(0.5), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, airtightPipeObject, airtightPipeArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("Fluid and gas networks can connect to any block in this layer").colored(PonderPalette.GREEN).pointAt(bottomCenterVec).placeNearTarget().attachKeyFrame();

        scene.idle(85);
        scene.world().showSection(funnelSelection, Direction.EAST);
        scene.world().showSection(hatchSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().showText(60).text("With its windows open, Items can also be dropped directly into the Kettle").colored(PonderPalette.BLUE).pointAt(hatchVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_reactor_kettle_processing", "Processing with the Airtight Reactor Kettle");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos centerPos = util.grid().at(3, 1, 3);
        BlockPos corePos = centerPos.above(2);
        BlockPos leftUpPos = centerPos.above(3).east().south();
        BlockPos rightDownPos = centerPos.above().west().north();
        BlockPos filterPos = rightDownPos.east();
        BlockPos cogPos = centerPos.above(3).west(2);
        BlockPos motorPos = cogPos.above();
        BlockPos inputFunnelPos = rightDownPos.west();
        BlockPos inputBeltLeftPos = inputFunnelPos.below();
        BlockPos inputBeltRightPos = inputBeltLeftPos.west();
        BlockPos inputMotorPos = inputBeltRightPos.north();
        BlockPos outputFunnelPos = centerPos.above().east().north(2);
        BlockPos outputBeltRightPos = outputFunnelPos.below().west();
        BlockPos outputBeltLeftPos = outputBeltRightPos.east(3);
        BlockPos outputMotorPos = outputBeltLeftPos.south();
        BlockPos blazePos = centerPos.east().north();
        BlockPos breezePos = centerPos.north();
        BlockPos secondCoolerPos = breezePos.west();
        BlockPos thirdCoolerPos = secondCoolerPos.south();

        Selection kettleSelection = util.select().fromTo(leftUpPos, rightDownPos);
        Selection kettleCogsSelection = util.select().fromTo(leftUpPos, rightDownPos.above(2));
        Selection sourceSelection = util.select().fromTo(cogPos, motorPos);
        Selection inputFunnelSelection = util.select().fromTo(inputFunnelPos, inputMotorPos);
        Selection outputFunnelSelection = util.select().fromTo(outputBeltLeftPos, outputBeltRightPos.above());
        Selection outputMotorSelection = util.select().position(outputMotorPos);
        Selection ironSelection = util.select().fromTo(secondCoolerPos, centerPos.south().east());

        Vec3 coreVec = util.vector().centerOf(corePos);
        Vec3 filterVec = util.vector().blockSurface(filterPos, Direction.NORTH).add(0, 0, -0.0625);
        Vec3 thermoregulatorVec = util.vector().centerOf(centerPos);

        AABB thermoregulatorArea = new AABB(util.vector().centerOf(centerPos.north().west()), util.vector().centerOf(centerPos.south().east()));

        Object thermoregulatorObject = new Object();

        ItemStack breezeCoreItem = new ItemStack(CCBItems.BREEZE_CORE.asItem());
        ItemStack stoneItem = new ItemStack(Blocks.STONE);
        ItemStack ultrawarmAirCanister = new ItemStack(CCBItems.GAS_CANISTER.asItem());
        ultrawarmAirCanister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.ULTRAWARM_AIR.get(), 1));

        float fastSpeed = SpeedLevel.FAST.getSpeedValue();
        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(ironSelection, Direction.DOWN);
        scene.world().showSection(kettleSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(sourceSelection, Direction.EAST);

        scene.idle(15);
        scene.world().setKineticSpeed(sourceSelection, fastSpeed);
        scene.world().setKineticSpeed(kettleCogsSelection, -fastSpeed);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(20);
        scene.overlay().showText(60).text("The Reactor Kettle can process Items, Fluids, and Gases together").pointAt(coreVec).placeNearTarget().attachKeyFrame();

        scene.idle(70);
        scene.world().setBlock(inputMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);
        scene.world().showSection(inputFunnelSelection, Direction.EAST);
        scene.world().setKineticSpeed(inputFunnelSelection, -mediumSpeed);

        scene.idle(10);
        scene.overlay().showText(60).text("This recipe consumes Stone, produces Natural Air, and returns the Breeze Core").pointAt(coreVec).placeNearTarget().attachKeyFrame();

        scene.idle(30);
        scene.world().createItemOnBeltLike(inputBeltRightPos, Direction.UP, breezeCoreItem.copy());

        scene.idle(13);
        scene.world().removeItemsFromBelt(inputBeltLeftPos);
        scene.world().flapFunnel(inputFunnelPos, false);
        scene.world().modifyBlockEntity(corePos, AirtightReactorKettleBlockEntity.class, kettle -> kettle.getAvailableItems().insertItem(0, breezeCoreItem.copy(), false));

        scene.idle(10);
        scene.world().createItemOnBeltLike(inputBeltRightPos, Direction.UP, stoneItem.copy());

        scene.idle(13);
        scene.world().removeItemsFromBelt(inputBeltLeftPos);
        scene.world().flapFunnel(inputFunnelPos, false);
        scene.world().modifyBlockEntity(corePos, AirtightReactorKettleBlockEntity.class, kettle -> kettle.getAvailableItems().insertItem(1, stoneItem.copy(), false));

        scene.idle(14);
        scene.overlay().showText(60).text("Stored gas or gas-processing recipes make the Kettle close its windows automatically").pointAt(coreVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().modifyBlockEntity(corePos, AirtightReactorKettleBlockEntity.class, AirtightReactorKettleBlockEntity::startProcessInPonderLevel);

        scene.idle(70);
        scene.world().setBlock(outputMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.NORTH), false);
        scene.world().showSection(outputFunnelSelection, Direction.SOUTH);
        scene.world().showSection(outputMotorSelection, Direction.NORTH);

        scene.idle(15);
        scene.world().setKineticSpeed(outputFunnelSelection, -mediumSpeed);
        scene.world().setKineticSpeed(outputMotorSelection, -mediumSpeed);
        scene.world().flapFunnel(outputFunnelPos, true);
        scene.world().modifyBlockEntity(corePos, AirtightReactorKettleBlockEntity.class, kettle -> kettle.getAvailableItems().extractItem(27, 1, false));
        scene.world().createItemOnBeltLike(outputFunnelPos.below(), Direction.UP, breezeCoreItem.copy());
        scene.overlay().showText(60).text("Finished products are extracted through the bottom interfaces").colored(PonderPalette.OUTPUT).pointAt(coreVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("The Reactor Kettle waits until all recipe outputs can fit").colored(PonderPalette.RED).pointAt(coreVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showFilterSlotInput(filterVec, Direction.NORTH, 60);
        scene.overlay().showText(60).text("When multiple recipes match, the Recipe Filter selects by primary output").colored(PonderPalette.BLUE).pointAt(filterVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(coreVec, Pointing.DOWN, 60).withItem(ultrawarmAirCanister.copy());
        scene.overlay().showText(60).text("Some gas-fed recipes require a minimum input pressure").colored(PonderPalette.RED).pointAt(coreVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Temperature-sensitive recipes require the Kettle to reach the required temperature").colored(PonderPalette.RED).pointAt(thermoregulatorVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().hideSection(inputFunnelSelection, Direction.WEST);
        scene.world().hideSection(outputFunnelSelection, Direction.EAST);
        scene.world().hideSection(outputMotorSelection, Direction.EAST);

        scene.idle(20);
        scene.world().setBlock(blazePos, AllBlocks.BLAZE_BURNER.getDefaultState(), true);
        scene.world().modifyBlock(blazePos, state -> state.setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.KINDLED), false);
        scene.world().setBlock(breezePos, CCBBlocks.BREEZE_COOLER_BLOCK.getDefaultState(), true);
        setChilledCooler(scene, breezePos);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, thermoregulatorObject, thermoregulatorArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, thermoregulatorObject, thermoregulatorArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("Thermoregulators in the 3x3 area directly beneath the Kettle are added together").pointAt(thermoregulatorVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Burning Blaze Burners add heat, while Chilled Breeze Coolers add cooling").pointAt(thermoregulatorVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(blazePos, AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState(), true);
        scene.overlay().showText(60).text("Enough combined cooling reaches Superchilled conditions, enabling Superchilled recipes").colored(PonderPalette.GREEN).pointAt(thermoregulatorVec).placeNearTarget().attachKeyFrame();

        scene.idle(10);
        scene.world().setBlock(secondCoolerPos, CCBBlocks.BREEZE_COOLER_BLOCK.getDefaultState(), true);
        setChilledCooler(scene, secondCoolerPos);

        scene.idle(8);
        scene.world().setBlock(thirdCoolerPos, CCBBlocks.BREEZE_COOLER_BLOCK.getDefaultState(), true);
        setChilledCooler(scene, thirdCoolerPos);

        scene.idle(42);
        scene.markAsFinished();
    }

    private static void setChilledCooler(CreateSceneBuilder scene, BlockPos coolerPos) {
        scene.world().modifyBlock(coolerPos, state -> state.setValue(BreezeCoolerBlock.FROST_LEVEL, FrostLevel.CHILLED), false);
        scene.world().modifyBlockEntity(coolerPos, BreezeCoolerBlockEntity.class, BreezeCoolerBlockEntity::switchToChilledState);
    }
}
