package net.ty.createcraftedbeginning.ponder.scenes.gaspipes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class AirtightPumpScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_pump", "Transport gases using Airtight Pumps");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos leftTankBottomPos = util.grid().at(5, 1, 5);
        BlockPos leftTankTopPos = leftTankBottomPos.above();
        BlockPos pipeFirstPos = leftTankBottomPos.north();
        BlockPos pipeSecondPos = pipeFirstPos.north(2);
        BlockPos encasedPos = pipeSecondPos.north();
        BlockPos pipeLeftPos = encasedPos.west();
        BlockPos pumpPos = pipeLeftPos.west();
        BlockPos cogPos = pumpPos.south();
        BlockPos motorPos = cogPos.east();
        BlockPos pipeRightPos = pumpPos.west();
        BlockPos rightTankBottomPos = pipeRightPos.west();
        BlockPos rightTankTopPos = rightTankBottomPos.above();

        Selection leftTankSelection = util.select().fromTo(leftTankBottomPos, leftTankTopPos);
        Selection rightTankSelection = util.select().fromTo(rightTankBottomPos, rightTankTopPos);
        Selection pipeBackSelection = util.select().fromTo(pipeFirstPos, pipeSecondPos);
        Selection encasedSelection = util.select().position(encasedPos);
        Selection pipeFrontSelection = util.select().fromTo(pipeLeftPos, pipeRightPos);
        Selection cogSelection = util.select().fromTo(cogPos, motorPos);
        Selection pumpSelection = util.select().position(pumpPos);

        float slowSpeed = SpeedLevel.SLOW.getSpeedValue();
        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();
        float fastSpeed = SpeedLevel.FAST.getSpeedValue();

        AABB backArea = new AABB(util.vector().centerOf(pipeFirstPos), util.vector().centerOf(pipeFirstPos));
        AABB frontArea = new AABB(util.vector().centerOf(encasedPos), util.vector().centerOf(encasedPos));
        AABB pumpArea = new AABB(util.vector().centerOf(pumpPos), util.vector().centerOf(pumpPos));

        Object backObject = new Object();
        Object frontObject = new Object();
        Object pumpObject = new Object();

        scene.idle(20);
        scene.world().showSection(leftTankSelection, Direction.NORTH);

        scene.idle(3);
        scene.world().showSection(pipeBackSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(encasedSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(pipeFrontSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(rightTankSelection, Direction.EAST);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST), false);
        scene.world().showSection(cogSelection, Direction.NORTH);
        scene.world().setKineticSpeed(cogSelection, mediumSpeed);
        scene.world().setKineticSpeed(pumpSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, backObject, backArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, frontObject, frontArea, 3);

        scene.idle(3);
        backArea = backArea.inflate(0.5, 0.5, 0.5);
        frontArea = frontArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, backObject, backArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, frontObject, frontArea, 3);

        scene.idle(3);
        backArea = backArea.expandTowards(0, 0, -3);
        frontArea = frontArea.expandTowards(-3, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, backObject, backArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, frontObject, frontArea, 60);
        scene.overlay().showText(60).text("Airtight Pumps works similar to Mechanical Pumps").colored(PonderPalette.INPUT).pointAt(Vec3.atCenterOf(pumpPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pumpObject, pumpArea, 3);

        scene.idle(3);
        pumpArea = pumpArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pumpObject, pumpArea, 3);

        scene.idle(3);
        pumpArea = pumpArea.expandTowards(0, 0, 1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pumpObject, pumpArea, 60);
        scene.world().setKineticSpeed(cogSelection, slowSpeed);
        scene.world().setKineticSpeed(pumpSelection, -slowSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.overlay().showText(60).text("But Airtight Pumps must be rotating with at least medium rotational speed").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(pumpPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setKineticSpeed(cogSelection, fastSpeed);
        scene.world().setKineticSpeed(pumpSelection, -fastSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.overlay().showText(60).text("And the Pumps affect pipes connected up to 32 blocks away").pointAt(Vec3.atCenterOf(pumpPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
