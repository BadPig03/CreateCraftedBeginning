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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump.AirtightRegulatorPumpBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightRegulatorPumpScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_regulator_pump", "Regulating Outlet Pressure with Airtight Pressure Regulator Pumps");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos leftTankBottomPos = util.grid().at(5, 1, 5);
        BlockPos pipeFirstPos = leftTankBottomPos.north();
        BlockPos pipeSecondPos = pipeFirstPos.north(2);
        BlockPos encasedPos = pipeSecondPos.north();
        BlockPos eastPipePos = encasedPos.west();
        BlockPos regulatorPos = eastPipePos.west();
        BlockPos drivePos = regulatorPos.south();
        BlockPos westPipePos = regulatorPos.west();
        BlockPos rightTankBottomPos = westPipePos.west();

        Selection leftTankSelection = util.select().fromTo(leftTankBottomPos, leftTankBottomPos.above());
        Selection backPipeSelection = util.select().fromTo(pipeFirstPos, pipeSecondPos);
        Selection encasedSelection = util.select().position(encasedPos);
        Selection regulatorPumpSelection = util.select().fromTo(eastPipePos, westPipePos);
        Selection rightTankSelection = util.select().fromTo(rightTankBottomPos, rightTankBottomPos.above());
        Selection driveSelection = util.select().position(drivePos);
        Selection regulatorSelection = util.select().position(regulatorPos);

        Vec3 regulatorVec = util.vector().centerOf(regulatorPos);
        Vec3 panelVec = util.vector().topOf(regulatorPos);
        Vec3 inputFlowStartVec = util.vector().blockSurface(westPipePos, Direction.WEST);
        Vec3 inletTankVec = util.vector().centerOf(rightTankBottomPos);
        Vec3 outletTankVec = util.vector().centerOf(leftTankBottomPos);

        float slowSpeed = SpeedLevel.SLOW.getSpeedValue();
        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();
        float fastSpeed = SpeedLevel.FAST.getSpeedValue();
        long demonstrationSetPressurePa = GasPressure.pascals(6);
        long loweredSetPressurePa = GasPressure.pascals(2);

        AABB statusArea = new AABB(regulatorVec, regulatorVec);
        AABB connectionArea = new AABB(regulatorVec, regulatorVec).inflate(0.3125);
        AABB forwardFlowArea = new AABB(inputFlowStartVec, inputFlowStartVec);
        AABB inletPressureArea = new AABB(inletTankVec, inletTankVec);
        AABB outletPressureArea = new AABB(outletTankVec, outletTankVec);
        AABB highSpeedFlowArea = new AABB(inputFlowStartVec, inputFlowStartVec);
        AABB targetReachedFlowArea = new AABB(inputFlowStartVec, inputFlowStartVec).inflate(0, 0.3125, 0.3125).expandTowards(3, 0, 0);

        Object statusObject = new Object();
        Object inputConnectionObject = new Object();
        Object outputConnectionObject = new Object();
        Object forwardFlowObject = new Object();
        Object inletPressureObject = new Object();
        Object outletPressureObject = new Object();
        Object highSpeedFlowObject = new Object();
        Object highSpeedInletPressureObject = new Object();
        Object highSpeedOutletPressureObject = new Object();
        Object targetReachedFlowObject = new Object();
        Object targetReachedOutletObject = new Object();
        Object loweredPressureObject = new Object();
        Object loweredInputObject = new Object();
        Object loweredOutputObject = new Object();
        Object directionInputObject = new Object();
        Object directionOutputObject = new Object();

        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        scene.world().setBlock(regulatorPos, CCBBlocks.AIRTIGHT_REGULATOR_PUMP_BLOCK.getDefaultState().setValue(BlockStateProperties.FACING, Direction.EAST), false);
        scene.world().setBlock(drivePos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.NORTH), false);

        scene.idle(20);
        scene.world().showSection(leftTankSelection, Direction.NORTH);

        scene.idle(3);
        scene.world().showSection(backPipeSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(encasedSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(regulatorPumpSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(rightTankSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(driveSelection, Direction.NORTH);

        scene.idle(15);
        scene.world().setKineticSpeed(driveSelection, slowSpeed);
        scene.world().setKineticSpeed(regulatorSelection, slowSpeed);
        scene.effects().rotationSpeedIndicator(regulatorPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, statusObject, statusArea, 3);

        scene.idle(3);
        statusArea = statusArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, statusObject, statusArea, 60);
        scene.overlay().showText(60).text("Airtight Pressure Regulator Pumps require at least Medium rotational speed to operate").colored(PonderPalette.RED).pointAt(regulatorVec).placeNearTarget().attachKeyFrame();

        scene.idle(77);
        scene.world().setKineticSpeed(driveSelection, mediumSpeed);
        scene.world().setKineticSpeed(regulatorSelection, mediumSpeed);
        scene.effects().rotationSpeedIndicator(regulatorPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputConnectionObject, connectionArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputConnectionObject, connectionArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputConnectionObject, connectionArea.move(-0.5, 0, 0), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputConnectionObject, connectionArea.move(0.5, 0, 0), 60);
        scene.overlay().showCenteredScrollInput(regulatorPos, Direction.UP, 60);
        scene.overlay().showControls(panelVec, Pointing.DOWN, 60).scroll();
        scene.overlay().showText(60).text("Use the value panel to set the desired outlet pressure").colored(PonderPalette.BLUE).pointAt(panelVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().modifyBlockEntity(regulatorPos, AirtightRegulatorPumpBlockEntity.class, pump -> pump.setOutletSetPressurePa(demonstrationSetPressurePa));

        scene.idle(67);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, forwardFlowObject, forwardFlowArea, 3);

        scene.idle(3);
        forwardFlowArea = forwardFlowArea.inflate(0, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, forwardFlowObject, forwardFlowArea, 3);

        scene.idle(3);
        forwardFlowArea = forwardFlowArea.expandTowards(3, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, forwardFlowObject, forwardFlowArea, 60);
        scene.overlay().showText(60).text("When the outlet is below the set pressure, the regulator pumps gas from its inlet toward its outlet").pointAt(regulatorVec).placeNearTarget().attachKeyFrame();

        scene.idle(77);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inletPressureObject, inletPressureArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outletPressureObject, outletPressureArea, 3);

        scene.idle(3);
        inletPressureArea = inletPressureArea.inflate(0.5, 0.5, 0.5).expandTowards(0, 1, 0);
        outletPressureArea = outletPressureArea.inflate(0.5, 0.5, 0.5).expandTowards(0, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inletPressureObject, inletPressureArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outletPressureObject, outletPressureArea, 60);
        scene.overlay().showText(60).text("The regulator can raise outlet pressure only within the pressure rise allowed by its current speed").colored(PonderPalette.RED).pointAt(regulatorVec).placeNearTarget().attachKeyFrame();

        scene.idle(69);
        scene.world().setKineticSpeed(driveSelection, fastSpeed);
        scene.world().setKineticSpeed(regulatorSelection, fastSpeed);
        scene.effects().rotationSpeedIndicator(regulatorPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, highSpeedFlowObject, highSpeedFlowArea, 3);

        scene.idle(3);
        highSpeedFlowArea = highSpeedFlowArea.inflate(0, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, highSpeedFlowObject, highSpeedFlowArea, 3);

        scene.idle(3);
        highSpeedFlowArea = highSpeedFlowArea.expandTowards(3, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, highSpeedFlowObject, highSpeedFlowArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, highSpeedInletPressureObject, inletPressureArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, highSpeedOutletPressureObject, outletPressureArea, 60);
        scene.overlay().showText(60).text("Higher rotational speed increases both maximum pressure rise and maximum flow rate").colored(PonderPalette.GREEN).pointAt(regulatorVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, targetReachedFlowObject, targetReachedFlowArea, 24);
        scene.overlay().showText(60).text("Once the outlet reaches the set pressure, the regulator stops adding gas").colored(PonderPalette.GREEN).pointAt(outletTankVec).placeNearTarget().attachKeyFrame();

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, targetReachedOutletObject, outletPressureArea, 40);

        scene.idle(60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, loweredPressureObject, outletPressureArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, loweredInputObject, connectionArea.move(-0.5, 0, 0), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, loweredOutputObject, connectionArea.move(0.5, 0, 0), 60);
        scene.overlay().showCenteredScrollInput(regulatorPos, Direction.UP, 60);
        scene.overlay().showControls(panelVec, Pointing.DOWN, 60).scroll();
        scene.overlay().showText(60).text("Lowering the set pressure below the current outlet pressure does not vent gas or cause backflow").colored(PonderPalette.RED).pointAt(panelVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().modifyBlockEntity(regulatorPos, AirtightRegulatorPumpBlockEntity.class, pump -> pump.setOutletSetPressurePa(loweredSetPressurePa));

        scene.idle(73);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, directionInputObject, connectionArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, directionOutputObject, connectionArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, directionInputObject, connectionArea.move(-0.5, 0, 0), 7);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, directionOutputObject, connectionArea.move(0.5, 0, 0), 7);
        scene.overlay().showText(60).text("Use a Wrench to swap the inlet and outlet sides").colored(PonderPalette.BLUE).pointAt(regulatorVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().topOf(regulatorPos), Pointing.DOWN, 60).rightClick().withItem(wrenchItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(regulatorPos, state -> state.setValue(BlockStateProperties.FACING, Direction.WEST), false);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, directionInputObject, connectionArea.move(0.5, 0, 0), 53);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, directionOutputObject, connectionArea.move(-0.5, 0, 0), 53);

        scene.idle(53);
        scene.markAsFinished();
    }
}
