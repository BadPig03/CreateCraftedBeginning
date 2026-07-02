package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
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
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineScenes {
    public static void settingUp(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("tesla_turbine_setting_up", "Setting Up Tesla Turbines");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos leftCenter = util.grid().at(4, 1, 2);
        BlockPos leftLU = leftCenter.east().south();
        BlockPos leftRD = leftCenter.west().north();
        BlockPos leftLDNozzle = leftCenter.east(2).north();
        BlockPos leftLUNozzle = leftCenter.south(2).east();
        BlockPos leftRUNozzle = leftCenter.west(2).south();
        BlockPos leftRDNozzle = leftCenter.north(2).west();
        BlockPos leftVirtualNozzle = leftCenter.west(2).north();
        BlockPos rightCenter = leftCenter.south(3).west(2).above();
        BlockPos rightLU = rightCenter.east().above();
        BlockPos rightRD = rightCenter.west().below();
        BlockPos rightLeftNozzle = rightLU.above();
        BlockPos rightRightNozzle = rightLeftNozzle.west(2);

        Selection leftSelection = util.select().fromTo(leftLU, leftRD);
        Selection leftLDNozzleSelection = util.select().position(leftLDNozzle);
        Selection leftLUNozzleSelection = util.select().position(leftLUNozzle);
        Selection leftRUNozzleSelection = util.select().position(leftRUNozzle);
        Selection leftRDNozzleSelection = util.select().position(leftRDNozzle);
        Selection rightSelection = util.select().fromTo(rightLU, rightRD);
        Selection rightNozzlesSelection = util.select().fromTo(rightLeftNozzle, rightRightNozzle);

        Vec3 rightCenterVec = util.vector().centerOf(rightCenter);
        Vec3 leftRDVec = util.vector().centerOf(leftRD);
        Vec3 leftVec = util.vector().centerOf(leftCenter);
        Vec3 leftRDNozzleVec = util.vector().centerOf(leftRDNozzle);
        Vec3 leftVirtualNozzleVec = util.vector().centerOf(leftVirtualNozzle);
        Vec3 rightNozzleVec = util.vector().centerOf(rightRightNozzle);
        Vec3 leftLDNozzleVec = util.vector().centerOf(leftLDNozzle);
        Vec3 leftLUNozzleVec = util.vector().centerOf(leftLUNozzle);
        Vec3 leftRUNozzleVec = util.vector().centerOf(leftRUNozzle);
        Vec3 rightLeftNozzleVec = util.vector().centerOf(rightLeftNozzle);

        AABB leftArea = new AABB(leftVec, leftVec);
        AABB rightArea = new AABB(rightCenterVec, rightCenterVec);
        AABB leftLDNozzleArea = new AABB(leftLDNozzleVec, leftLDNozzleVec);
        AABB leftLUNozzleArea = new AABB(leftLUNozzleVec, leftLUNozzleVec);
        AABB leftRUNozzleArea = new AABB(leftRUNozzleVec, leftRUNozzleVec);
        AABB leftRDNozzleArea = new AABB(leftRDNozzleVec, leftRDNozzleVec);
        AABB leftVirtualNozzleArea = new AABB(leftVirtualNozzleVec, leftVirtualNozzleVec);
        AABB rightLeftNozzleArea = new AABB(rightLeftNozzleVec, rightLeftNozzleVec);
        AABB rightRightNozzleArea = new AABB(rightNozzleVec, rightNozzleVec);

        Object leftObject = new Object();
        Object rightObject = new Object();
        Object leftLDNozzleObject = new Object();
        Object leftLUNozzleObject = new Object();
        Object leftRUNozzleObject = new Object();
        Object leftRDNozzleObject = new Object();
        Object leftVirualObject = new Object();
        Object rightLeftNozzleObject = new Object();
        Object rightRightNozzleObject = new Object();

        ItemStack rotorItem = new ItemStack(CCBItems.TESLA_TURBINE_ROTOR.asItem());

        scene.idle(20);
        scene.world().showSection(rightSelection, Direction.NORTH);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightObject, rightArea, 3);

        scene.idle(3);
        rightArea = rightArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightObject, rightArea, 3);

        scene.idle(3);
        rightArea = rightArea.inflate(1, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightObject, rightArea, 60);
        scene.overlay().showText(60).text("Tesla Turbines require a 3x3 space for placement").pointAt(rightCenterVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(leftSelection, Direction.DOWN);

        scene.idle(20);
        rightArea = new AABB(rightCenterVec, rightCenterVec);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightObject, rightArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftObject, leftArea, 3);

        scene.idle(3);
        leftArea = leftArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftObject, leftArea, 3);
        rightArea = rightArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightObject, rightArea, 3);

        scene.idle(3);
        leftArea = leftArea.inflate(1, 0, 1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftObject, leftArea, 60);
        rightArea = rightArea.inflate(1, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightObject, rightArea, 60);
        scene.overlay().showText(60).text("And can be oriented horizontally or vertically").pointAt(leftRDVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(leftCenter, Direction.UP), Pointing.DOWN, 67).rightClick().withItem(rotorItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(leftCenter, s -> s.setValue(TeslaTurbineBlock.ROTOR, 1), false);
        scene.overlay().showText(60).text("Requires at least one Tesla Turbine Rotor to function...").colored(PonderPalette.RED).pointAt(leftVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(leftLDNozzleSelection, Direction.WEST);
        scene.world().showSection(leftLUNozzleSelection, Direction.NORTH);
        scene.world().showSection(leftRUNozzleSelection, Direction.EAST);
        scene.world().showSection(leftRDNozzleSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftLDNozzleObject, leftLDNozzleArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftLUNozzleObject, leftLUNozzleArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftRUNozzleObject, leftRUNozzleArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftRDNozzleObject, leftRDNozzleArea, 3);

        scene.idle(3);
        leftLDNozzleArea = leftLDNozzleArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftLDNozzleObject, leftLDNozzleArea, 60);
        leftLUNozzleArea = leftLUNozzleArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftLUNozzleObject, leftLUNozzleArea, 60);
        leftRUNozzleArea = leftRUNozzleArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftRUNozzleObject, leftRUNozzleArea, 60);
        leftRDNozzleArea = leftRDNozzleArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftRDNozzleObject, leftRDNozzleArea, 60);
        scene.overlay().showText(60).text("...with up to 4 Tesla Turbine Nozzles attached laterally").colored(PonderPalette.GREEN).pointAt(leftRDNozzleVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        leftRDNozzleArea = new AABB(leftRDNozzleVec, leftRDNozzleVec);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftRDNozzleObject, leftRDNozzleArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftVirualObject, leftVirtualNozzleArea, 3);

        scene.idle(3);
        leftRDNozzleArea = leftRDNozzleArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftRDNozzleObject, leftRDNozzleArea, 60);
        leftVirtualNozzleArea = leftVirtualNozzleArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftVirualObject, leftVirtualNozzleArea, 60);
        scene.overlay().showText(60).text("Adjacent nozzles cannot occupy the same corner position").colored(PonderPalette.RED).pointAt(leftVirtualNozzleVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(rightNozzlesSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightLeftNozzleObject, rightLeftNozzleArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightRightNozzleObject, rightRightNozzleArea, 3);

        scene.idle(3);
        rightLeftNozzleArea = rightLeftNozzleArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightLeftNozzleObject, rightLeftNozzleArea, 60);
        rightRightNozzleArea = rightRightNozzleArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightRightNozzleObject, rightRightNozzleArea, 60);
        scene.overlay().showText(60).text("Nozzle placement determines the direction and capacity of Stress generated").colored(PonderPalette.MEDIUM).pointAt(rightNozzleVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void generating(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("tesla_turbine_generating_rotational_force", "Generating Rotational Force via Tesla Turbines");
        scene.scaleSceneView(0.8f);
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos ironBlockPos = util.grid().at(4, 1, 2);
        BlockPos speedometerPos = ironBlockPos.above();
        BlockPos shaftPos = speedometerPos.south();
        BlockPos centerPos = shaftPos.south();
        BlockPos leftUpPos = centerPos.east().above();
        BlockPos rightDownPos = centerPos.west().below();
        BlockPos leftUpNozzlePos = leftUpPos.above();
        BlockPos leftUpPumpPos = leftUpNozzlePos.above();
        BlockPos leftUpPipePos = leftUpPumpPos.above();
        BlockPos leftUpCogPos = leftUpPumpPos.south();
        BlockPos leftUpMotorPos = leftUpCogPos.above();
        BlockPos rightUpNozzlePos = leftUpNozzlePos.west(2);
        BlockPos rightUpPumpPos = rightUpNozzlePos.above();
        BlockPos rightUpPipePos = rightUpPumpPos.above();
        BlockPos rightUpCogPos = rightUpPumpPos.south();
        BlockPos rightUpMotorPos = rightUpCogPos.above();
        BlockPos rightDownNozzlePos = rightDownPos.west();
        BlockPos rightDownPumpPos = rightDownNozzlePos.west();
        BlockPos rightDownPipePos = rightDownPumpPos.west();
        BlockPos rightDownCogPos = rightDownPumpPos.south();
        BlockPos rightDownMotorPos = rightDownPipePos.south();

        Selection turbineSelection = util.select().fromTo(leftUpPos, rightDownPos);
        Selection meterSelection = util.select().fromTo(ironBlockPos, shaftPos);
        Selection leftUpPumpSelection = util.select().fromTo(leftUpNozzlePos, leftUpPipePos);
        Selection leftUpSourceSelection = util.select().fromTo(leftUpCogPos, leftUpMotorPos);
        Selection rightUpPumpSelection = util.select().fromTo(rightUpNozzlePos, rightUpPipePos);
        Selection rightUpSourceSelection = util.select().fromTo(rightUpCogPos, rightUpMotorPos);
        Selection rightDownPumpSelection = util.select().fromTo(rightDownNozzlePos, rightDownPipePos);
        Selection rightDownSourceSelection = util.select().fromTo(rightDownCogPos, rightDownMotorPos);
        Selection shaftSelection = util.select().fromTo(speedometerPos, centerPos);

        Vec3 centerVec = util.vector().centerOf(centerPos);
        Vec3 rightDownNozzleVec = util.vector().centerOf(rightDownNozzlePos);
        Vec3 rightDownPipeVec = util.vector().centerOf(rightDownPipePos);
        Vec3 rightUpPipeVec = util.vector().centerOf(rightUpPipePos);
        Vec3 leftUpPipeVec = util.vector().centerOf(leftUpPipePos);
        Vec3 rotorControlVec = util.vector().blockSurface(centerPos, Direction.EAST);

        AABB rightDownArea = new AABB(rightDownPipeVec, rightDownPipeVec);
        AABB rightUpArea = new AABB(rightUpPipeVec, rightUpPipeVec);
        AABB leftUpArea = new AABB(leftUpPipeVec, leftUpPipeVec);

        Object rightDownObject = new Object();
        Object rightUpObject = new Object();
        Object leftUpObject = new Object();

        ItemStack rotorItem = new ItemStack(CCBItems.TESLA_TURBINE_ROTOR.asItem());

        scene.idle(20);
        scene.world().showSection(turbineSelection, Direction.NORTH);

        scene.idle(3);
        scene.world().showSection(meterSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(rightDownPumpSelection, Direction.EAST);
        scene.world().showSection(rightUpPumpSelection, Direction.DOWN);
        scene.world().showSection(leftUpPumpSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Generating Stress requires fulfilling multiple conditions...").colored(PonderPalette.RED).pointAt(centerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(rightDownMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), false);
        scene.world().showSection(rightDownSourceSelection, Direction.EAST);

        scene.idle(15);
        scene.world().setKineticSpeed(rightDownPumpSelection, 64);
        scene.world().setKineticSpeed(rightDownSourceSelection, -64);
        scene.world().setKineticSpeed(shaftSelection, 16);
        scene.effects().rotationSpeedIndicator(rightDownPumpPos);
        scene.effects().rotationSpeedIndicator(rightDownMotorPos);
        scene.effects().rotationSpeedIndicator(speedometerPos);
        scene.effects().indicateSuccess(speedometerPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, rightDownObject, rightDownArea, 3);

        scene.idle(3);
        rightDownArea = rightDownArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, rightDownObject, rightDownArea, 3);

        scene.idle(3);
        rightDownArea = rightDownArea.expandTowards(2, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, rightDownObject, rightDownArea, 60);
        scene.overlay().showText(60).text("1. Continuous high-volume gas input to the Nozzles").colored(PonderPalette.INPUT).pointAt(rightDownNozzleVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(rightUpMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(rightUpSourceSelection, Direction.DOWN);

        scene.idle(15);
        scene.world().setKineticSpeed(rightUpPumpSelection, 64);
        scene.world().setKineticSpeed(rightUpSourceSelection, -64);
        scene.world().setKineticSpeed(shaftSelection, 32);
        scene.effects().rotationSpeedIndicator(rightUpPumpPos);
        scene.effects().rotationSpeedIndicator(rightUpMotorPos);
        scene.effects().rotationSpeedIndicator(speedometerPos);
        scene.effects().indicateSuccess(speedometerPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightUpObject, rightUpArea, 3);
        rightDownArea = new AABB(rightDownPipeVec, rightDownPipeVec);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightDownObject, rightDownArea, 3);

        scene.idle(3);
        rightUpArea = rightUpArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightUpObject, rightUpArea, 3);
        rightDownArea = rightDownArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightDownObject, rightDownArea, 3);

        scene.idle(3);
        rightUpArea = rightUpArea.expandTowards(0, -2, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightUpObject, rightUpArea, 60);
        rightDownArea = rightDownArea.expandTowards(2, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightDownObject, rightDownArea, 60);
        scene.overlay().showText(60).text("...with consistent flow direction").colored(PonderPalette.GREEN).pointAt(Vec3.atCenterOf(rightDownPumpPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(leftUpMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(leftUpSourceSelection, Direction.DOWN);

        scene.idle(15);
        scene.world().setKineticSpeed(leftUpPumpSelection, 64);
        scene.world().setKineticSpeed(leftUpSourceSelection, -64);
        scene.world().setKineticSpeed(shaftSelection, 16);
        scene.effects().rotationSpeedIndicator(leftUpPumpPos);
        scene.effects().rotationSpeedIndicator(leftUpMotorPos);
        scene.effects().rotationSpeedIndicator(speedometerPos);
        scene.effects().indicateSuccess(speedometerPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftUpObject, leftUpArea, 3);
        rightUpArea = new AABB(rightUpPipeVec, rightUpPipeVec);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightUpObject, rightUpArea, 3);
        rightDownArea = new AABB(rightDownPipeVec, rightDownPipeVec);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightDownObject, rightDownArea, 3);

        scene.idle(3);
        leftUpArea = leftUpArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftUpObject, leftUpArea, 3);
        rightUpArea = rightUpArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightUpObject, rightUpArea, 3);
        rightDownArea = rightDownArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightDownObject, rightDownArea, 3);

        scene.idle(3);
        leftUpArea = leftUpArea.expandTowards(0, -2, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftUpObject, leftUpArea, 60);
        rightUpArea = rightUpArea.expandTowards(0, -2, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightUpObject, rightUpArea, 60);
        rightDownArea = rightDownArea.expandTowards(2, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightDownObject, rightDownArea, 60);
        scene.overlay().showText(60).text("Mismatched flow directions reduce power level").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(speedometerPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().hideSection(leftUpPumpSelection, Direction.UP);
        scene.world().hideSection(leftUpSourceSelection, Direction.UP);
        scene.world().setKineticSpeed(shaftSelection, 32);
        scene.effects().rotationSpeedIndicator(speedometerPos);

        scene.idle(20);
        scene.world().setKineticSpeed(rightUpPumpSelection, 256);
        scene.world().setKineticSpeed(rightUpSourceSelection, -256);
        scene.world().setKineticSpeed(rightDownPumpSelection, 256);
        scene.world().setKineticSpeed(rightDownSourceSelection, -256);
        scene.effects().rotationSpeedIndicator(rightUpPumpPos);
        scene.effects().rotationSpeedIndicator(rightDownPumpPos);
        scene.effects().rotationSpeedIndicator(rightUpMotorPos);
        scene.effects().rotationSpeedIndicator(rightDownMotorPos);
        scene.overlay().showText(60).text("2. Sufficient Tesla Turbine Rotors installed").colored(PonderPalette.GREEN).pointAt(centerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(rotorControlVec, Pointing.RIGHT, 37).rightClick().withItem(rotorItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(centerPos, s -> s.setValue(TeslaTurbineBlock.ROTOR, 2), false);
        scene.world().setKineticSpeed(shaftSelection, 64);
        scene.effects().rotationSpeedIndicator(speedometerPos);

        scene.idle(50);
        scene.overlay().showControls(rotorControlVec, Pointing.RIGHT, 37).rightClick().withItem(rotorItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(centerPos, s -> s.setValue(TeslaTurbineBlock.ROTOR, 3), false);
        scene.world().setKineticSpeed(shaftSelection, 96);
        scene.effects().rotationSpeedIndicator(speedometerPos);

        scene.idle(50);
        scene.overlay().showControls(rotorControlVec, Pointing.RIGHT, 37).rightClick().withItem(rotorItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(centerPos, s -> s.setValue(TeslaTurbineBlock.ROTOR, 4), false);
        scene.world().setKineticSpeed(shaftSelection, 128);
        scene.effects().rotationSpeedIndicator(speedometerPos);
        scene.overlay().showText(60).text("...to increase Stress production capacity").colored(PonderPalette.GREEN).pointAt(centerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Additionally, input gas type influences performance").colored(PonderPalette.MEDIUM).pointAt(rightDownNozzleVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
