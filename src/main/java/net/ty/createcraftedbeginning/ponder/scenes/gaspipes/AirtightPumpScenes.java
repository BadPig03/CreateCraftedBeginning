package net.ty.createcraftedbeginning.ponder.scenes.gaspipes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPumpScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_pump", "Boosting Pressure with Airtight Pumps");
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

        Vec3 pumpVec = util.vector().centerOf(pumpPos);
        Vec3 pipeFirstVec = util.vector().centerOf(pipeFirstPos);
        Vec3 inputFlowStartVec = util.vector().blockSurface(pipeRightPos, Direction.WEST);
        Vec3 backpressureFrontStartVec = util.vector().blockSurface(encasedPos, Direction.WEST);

        float slowSpeed = SpeedLevel.SLOW.getSpeedValue();
        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();
        float fastSpeed = SpeedLevel.FAST.getSpeedValue();

        AABB pumpStatusArea = new AABB(pumpVec, pumpVec);
        AABB connectionArea = new AABB(pumpVec, pumpVec).inflate(0.3125);
        AABB performanceArea = new AABB(pumpVec, pumpVec);
        AABB forwardFlowArea = new AABB(inputFlowStartVec, inputFlowStartVec);
        AABB backpressureBackArea = new AABB(pipeFirstVec, pipeFirstVec);
        AABB backpressureFrontArea = new AABB(backpressureFrontStartVec, backpressureFrontStartVec);

        Object pumpStatusObject = new Object();
        Object inputConnectionObject = new Object();
        Object outputConnectionObject = new Object();
        Object performanceObject = new Object();
        Object forwardFlowObject = new Object();
        Object backpressureBackObject = new Object();
        Object backpressureFrontObject = new Object();
        Object reversedInputConnectionObject = new Object();
        Object reversedOutputConnectionObject = new Object();
        Object wrenchInputConnectionObject = new Object();
        Object wrenchOutputConnectionObject = new Object();

        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

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

        scene.idle(15);
        scene.world().setKineticSpeed(cogSelection, slowSpeed);
        scene.world().setKineticSpeed(pumpSelection, -slowSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pumpStatusObject, pumpStatusArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, pumpStatusObject, pumpStatusArea.inflate(0.5), 60);
        scene.overlay().showText(60).text("Airtight Pumps require at least Medium rotational speed to operate").colored(PonderPalette.RED).pointAt(pumpVec).placeNearTarget().attachKeyFrame();

        scene.idle(77);
        scene.world().setKineticSpeed(cogSelection, mediumSpeed);
        scene.world().setKineticSpeed(pumpSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputConnectionObject, connectionArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputConnectionObject, connectionArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputConnectionObject, connectionArea.move(-0.5, 0, 0), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputConnectionObject, connectionArea.move(0.5, 0, 0), 60);
        scene.overlay().showText(60).text("When active, the pump provides a pressure boost from its inlet toward its outlet").pointAt(pumpVec).placeNearTarget().attachKeyFrame();

        scene.idle(77);
        scene.world().setKineticSpeed(cogSelection, fastSpeed);
        scene.world().setKineticSpeed(pumpSelection, -fastSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, performanceObject, performanceArea, 3);

        scene.idle(3);
        performanceArea = performanceArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, performanceObject, performanceArea, 60);
        scene.overlay().showText(60).text("Higher rotational speed increases both maximum pressure boost and maximum flow rate").colored(PonderPalette.GREEN).pointAt(pumpVec).placeNearTarget().attachKeyFrame();

        scene.idle(65);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, forwardFlowObject, forwardFlowArea, 3);

        scene.idle(3);
        forwardFlowArea = forwardFlowArea.inflate(0, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, forwardFlowObject, forwardFlowArea, 3);

        scene.idle(3);
        forwardFlowArea = forwardFlowArea.expandTowards(3, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, forwardFlowObject, forwardFlowArea, 18);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, backpressureBackObject, backpressureBackArea, 3);

        scene.idle(3);
        backpressureBackArea = backpressureBackArea.inflate(0.3125, 0.3125, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, backpressureBackObject, backpressureBackArea, 3);

        scene.idle(3);
        backpressureBackArea = backpressureBackArea.expandTowards(0, 0, -2.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, backpressureBackObject, backpressureBackArea, 69);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, backpressureFrontObject, backpressureFrontArea, 3);

        scene.idle(3);
        backpressureFrontArea = backpressureFrontArea.inflate(0, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, backpressureFrontObject, backpressureFrontArea, 3);

        scene.idle(3);
        backpressureFrontArea = backpressureFrontArea.expandTowards(-1.5, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, backpressureFrontObject, backpressureFrontArea, 60);
        scene.overlay().showText(60).text("Opposing pressure reduces flow; at the pump's maximum boost, flow stops").colored(PonderPalette.RED).pointAt(util.vector().blockSurface(pumpPos, Direction.EAST)).placeNearTarget().attachKeyFrame();

        scene.idle(75);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, reversedInputConnectionObject, connectionArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, reversedOutputConnectionObject, connectionArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, reversedInputConnectionObject, connectionArea.move(-0.5, 0, 0), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, reversedOutputConnectionObject, connectionArea.move(0.5, 0, 0), 60);
        scene.overlay().showText(60).text("Reversing the shaft rotation does not reverse the pumping direction").colored(PonderPalette.RED).pointAt(pumpVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().setKineticSpeed(cogSelection, -fastSpeed);
        scene.world().setKineticSpeed(pumpSelection, fastSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);

        scene.idle(53);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, wrenchInputConnectionObject, connectionArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, wrenchOutputConnectionObject, connectionArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, wrenchInputConnectionObject, connectionArea.move(-0.5, 0, 0), 7);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, wrenchOutputConnectionObject, connectionArea.move(0.5, 0, 0), 7);
        scene.overlay().showText(60).text("Use a Wrench to swap the inlet and outlet sides").colored(PonderPalette.BLUE).pointAt(pumpVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().topOf(pumpPos), Pointing.DOWN, 60).rightClick().withItem(wrenchItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(pumpPos, state -> state.setValue(AirtightPumpBlock.FACING, Direction.WEST), false);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, wrenchInputConnectionObject, connectionArea.move(0.5, 0, 0), 53);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, wrenchOutputConnectionObject, connectionArea.move(-0.5, 0, 0), 53);

        scene.idle(53);
        scene.markAsFinished();
    }
}
