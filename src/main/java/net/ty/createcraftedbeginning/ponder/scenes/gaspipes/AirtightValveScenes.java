package net.ty.createcraftedbeginning.ponder.scenes.gaspipes;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airtightvalve.AirtightValveBlock;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightValveScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_valve", "Operating Airtight Valves");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos leftTankBottomPos = util.grid().at(5, 1, 5);
        BlockPos pipeFirstPos = leftTankBottomPos.north();
        BlockPos pipeSecondPos = pipeFirstPos.north(2);
        BlockPos encasedPos = pipeSecondPos.north();
        BlockPos eastPipePos = encasedPos.west();
        BlockPos valvePos = eastPipePos.west();
        BlockPos drivePos = valvePos.south();
        BlockPos westPipePos = valvePos.west();
        BlockPos rightTankBottomPos = westPipePos.west();

        Selection leftTankSelection = util.select().fromTo(leftTankBottomPos, leftTankBottomPos.above());
        Selection backPipeSelection = util.select().fromTo(pipeFirstPos, pipeSecondPos);
        Selection encasedSelection = util.select().position(encasedPos);
        Selection valvePipeSelection = util.select().fromTo(eastPipePos, westPipePos);
        Selection rightTankSelection = util.select().fromTo(rightTankBottomPos, rightTankBottomPos.above());
        Selection driveSelection = util.select().position(drivePos);
        Selection valveSelection = util.select().position(valvePos);

        Vec3 valveVec = util.vector().centerOf(valvePos);
        Vec3 driveVec = util.vector().centerOf(drivePos);

        AABB connectionArea = new AABB(valveVec, valveVec);
        AABB openArea = new AABB(valveVec, valveVec);

        Object westClosedObject = new Object();
        Object eastClosedObject = new Object();
        Object openObject = new Object();

        float slowSpeed = SpeedLevel.MEDIUM.getSpeedValue() / 2;

        scene.idle(20);
        scene.world().showSection(leftTankSelection, Direction.NORTH);

        scene.idle(3);
        scene.world().showSection(backPipeSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(encasedSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(valvePipeSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(rightTankSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, westClosedObject, connectionArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, eastClosedObject, connectionArea, 3);

        scene.idle(3);
        connectionArea = connectionArea.inflate(0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, westClosedObject, connectionArea.move(-0.5, 0, 0), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, eastClosedObject, connectionArea.move(0.5, 0, 0), 60);
        scene.overlay().showText(60).text("A closed Airtight Valve completely separates the two sides of a gas line").colored(PonderPalette.RED).pointAt(valveVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(driveSelection, Direction.NORTH);

        scene.idle(20);
        scene.overlay().showText(60).text("Rotating its shaft in one direction gradually opens the valve").pointAt(driveVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().setKineticSpeed(driveSelection, slowSpeed);
        scene.world().setKineticSpeed(valveSelection, slowSpeed);
        scene.effects().rotationSpeedIndicator(valvePos);

        scene.idle(73);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, westClosedObject, connectionArea.move(-0.5, 0, 0), 80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, eastClosedObject, connectionArea.move(0.5, 0, 0), 80);
        scene.overlay().showText(60).text("The gas line remains isolated until the valve is fully open").colored(PonderPalette.RED).pointAt(valveVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setKineticSpeed(driveSelection, 0);
        scene.world().setKineticSpeed(valveSelection, 0);
        scene.world().modifyBlock(valvePos, state -> state.setValue(AirtightValveBlock.OPEN, true), false);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, westClosedObject, connectionArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, eastClosedObject, connectionArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, openObject, openArea, 3);

        scene.idle(3);
        openArea = openArea.inflate(0.5, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, openObject, openArea, 3);

        scene.idle(3);
        openArea = openArea.inflate(1.5, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, openObject, openArea, 177);
        scene.overlay().showText(60).text("Once fully open, gas can flow through the valve in either direction").colored(PonderPalette.GREEN).pointAt(valveVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setKineticSpeed(driveSelection, -slowSpeed);
        scene.world().setKineticSpeed(valveSelection, -slowSpeed);
        scene.effects().rotationSpeedIndicator(valvePos);
        scene.overlay().showText(60).text("Reversing the rotation gradually closes the valve again").pointAt(driveVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, openObject, openArea, 80);
        scene.overlay().showText(60).text("The gas line remains connected until the valve is fully closed").colored(PonderPalette.GREEN).pointAt(valveVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setKineticSpeed(driveSelection, 0);
        scene.world().setKineticSpeed(valveSelection, 0);
        scene.world().modifyBlock(valvePos, state -> state.setValue(AirtightValveBlock.OPEN, false), false);
        connectionArea = new AABB(valveVec, valveVec);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, westClosedObject, connectionArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, eastClosedObject, connectionArea, 3);

        scene.idle(3);
        connectionArea = connectionArea.inflate(0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, westClosedObject, connectionArea.move(-0.5, 0, 0), 40);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, eastClosedObject, connectionArea.move(0.5, 0, 0), 40);

        scene.idle(40);
        scene.markAsFinished();
    }
}
