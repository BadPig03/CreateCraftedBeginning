package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

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
import net.minecraft.core.particles.ParticleTypes;
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

        scene.title("tesla_turbine_setting_up", "Setting Up a Tesla Turbine");
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
        Selection rightWholeSelection = util.select().fromTo(rightLU, rightRD);
        Selection rightNozzlesSelection = util.select().fromTo(rightLeftNozzle, rightRightNozzle);

        Vec3 rightCenterVec = util.vector().centerOf(rightCenter);
        Vec3 leftRDVec = util.vector().centerOf(leftRD);
        Vec3 leftVec = util.vector().centerOf(leftCenter);
        Vec3 leftRDNozzleVec = util.vector().centerOf(leftRDNozzle);
        Vec3 leftVirtualNozzleVec = util.vector().centerOf(leftVirtualNozzle);
        Vec3 rightNozzleVec = util.vector().centerOf(rightRightNozzle);
        Vec3 rightLeftNozzleVec = util.vector().centerOf(rightLeftNozzle);
        Vec3 rightOutputVec = util.vector().blockSurface(rightCenter, Direction.NORTH);
        Vec3 rightOtherOutputVec = util.vector().blockSurface(rightCenter, Direction.SOUTH);

        AABB leftArea = new AABB(leftVec, leftVec);
        AABB rightArea = new AABB(rightCenterVec, rightCenterVec);
        AABB leftLDNozzleArea = new AABB(util.vector().centerOf(leftLDNozzle), util.vector().centerOf(leftLDNozzle));
        AABB leftLUNozzleArea = new AABB(util.vector().centerOf(leftLUNozzle), util.vector().centerOf(leftLUNozzle));
        AABB leftRUNozzleArea = new AABB(util.vector().centerOf(leftRUNozzle), util.vector().centerOf(leftRUNozzle));
        AABB leftRDNozzleArea = new AABB(leftRDNozzleVec, leftRDNozzleVec);
        AABB leftVirtualNozzleArea = new AABB(leftVirtualNozzleVec, leftVirtualNozzleVec);
        AABB rightLeftNozzleArea = new AABB(rightLeftNozzleVec, rightLeftNozzleVec);
        AABB rightRightNozzleArea = new AABB(rightNozzleVec, rightNozzleVec);
        AABB rightOutputArea = new AABB(rightOutputVec, rightOutputVec).inflate(0.35, 0.35, 0.02);
        AABB rightOtherOutputArea = new AABB(rightOtherOutputVec, rightOtherOutputVec).inflate(0.35, 0.35, 0.02);

        Object leftObject = new Object();
        Object rightObject = new Object();
        Object leftLDNozzleObject = new Object();
        Object leftLUNozzleObject = new Object();
        Object leftRUNozzleObject = new Object();
        Object leftRDNozzleObject = new Object();
        Object leftVirtualObject = new Object();
        Object rightLeftNozzleObject = new Object();
        Object rightRightNozzleObject = new Object();
        Object rightOutputObject = new Object();
        Object rightOtherOutputObject = new Object();

        ItemStack rotorItem = new ItemStack(CCBItems.TESLA_TURBINE_ROTOR.asItem());
        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        scene.idle(20);
        scene.world().showSection(rightWholeSelection, Direction.NORTH);

        scene.idle(20);
        scene.overlay().showText(60).text("Placing a Tesla Turbine forms a 3x3 structure automatically").pointAt(rightCenterVec).placeNearTarget().attachKeyFrame();
        rightArea = rightArea.inflate(1.5, 1.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightObject, rightArea, 60);

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, rightObject, rightArea, 60);
        scene.overlay().showText(60).text("Clear the entire 3x3 area before placing the Turbine").colored(PonderPalette.RED).pointAt(rightCenterVec).placeNearTarget().attachKeyFrame();

        scene.idle(54);
        scene.world().showSection(leftSelection, Direction.DOWN);

        scene.idle(20);
        rightArea = new AABB(rightCenterVec, rightCenterVec);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightObject, rightArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftObject, leftArea, 3);

        scene.idle(3);
        leftArea = leftArea.inflate(0.5);
        rightArea = rightArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftObject, leftArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightObject, rightArea, 3);

        scene.idle(3);
        leftArea = leftArea.inflate(1, 0, 1);
        rightArea = rightArea.inflate(1, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftObject, leftArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightObject, rightArea, 60);
        scene.overlay().showText(60).text("The 3x3 plane is perpendicular to the horizontal or vertical rotation axis").pointAt(leftRDVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("The Turbine needs 1 to 8 installed Rotors").colored(PonderPalette.RED).pointAt(leftVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().topOf(leftCenter), Pointing.DOWN, 60).rightClick().withItem(rotorItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(leftCenter, state -> state.setValue(TeslaTurbineBlock.ROTOR, 1), false);
        for (int count = 2; count <= 8; count++) {
            scene.idle(7);

            int rotorCount = count;
            scene.world().modifyBlock(leftCenter, state -> state.setValue(TeslaTurbineBlock.ROTOR, rotorCount), false);
        }

        scene.idle(24);
        scene.overlay().showText(60).text("A Wrench removes one Rotor per use").colored(PonderPalette.BLUE).pointAt(leftVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().topOf(leftCenter), Pointing.DOWN, 60).rightClick().withItem(wrenchItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(leftCenter, state -> state.setValue(TeslaTurbineBlock.ROTOR, 7), false);

        scene.idle(73);
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
        leftLDNozzleArea = leftLDNozzleArea.inflate(0.5);
        leftLUNozzleArea = leftLUNozzleArea.inflate(0.5);
        leftRUNozzleArea = leftRUNozzleArea.inflate(0.5);
        leftRDNozzleArea = leftRDNozzleArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftLDNozzleObject, leftLDNozzleArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftLUNozzleObject, leftLUNozzleArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftRUNozzleObject, leftRUNozzleArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftRDNozzleObject, leftRDNozzleArea, 60);
        scene.overlay().showText(60).text("Each of the four corner blocks can host one Tesla Turbine Nozzle").colored(PonderPalette.GREEN).pointAt(leftRDNozzleVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftLDNozzleObject, leftLDNozzleArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftLUNozzleObject, leftLUNozzleArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftRUNozzleObject, leftRUNozzleArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftRDNozzleObject, leftRDNozzleArea, 60);
        scene.overlay().showText(60).text("The Turbine needs 1 to 4 Nozzles").colored(PonderPalette.RED).pointAt(leftRDNozzleVec).placeNearTarget().attachKeyFrame();

        scene.idle(77);
        leftRDNozzleArea = new AABB(leftRDNozzleVec, leftRDNozzleVec);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftRDNozzleObject, leftRDNozzleArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftVirtualObject, leftVirtualNozzleArea, 3);

        scene.idle(3);
        leftRDNozzleArea = leftRDNozzleArea.inflate(0.5);
        leftVirtualNozzleArea = leftVirtualNozzleArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftRDNozzleObject, leftRDNozzleArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftVirtualObject, leftVirtualNozzleArea, 60);
        scene.overlay().showText(60).text("Only one of the two positions at each corner can hold a Nozzle").colored(PonderPalette.RED).pointAt(leftVirtualNozzleVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(rightCenter, Direction.NORTH), Pointing.RIGHT, 20).rightClick().withItem(rotorItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(rightCenter, state -> state.setValue(TeslaTurbineBlock.ROTOR, 1), false);

        scene.idle(13);
        scene.world().showSection(rightNozzlesSelection, Direction.DOWN);

        scene.idle(20);
        rightLeftNozzleArea = rightLeftNozzleArea.inflate(0.5);
        rightRightNozzleArea = rightRightNozzleArea.inflate(0.5);
        scene.overlay().showText(60).text("A Nozzle's position determines which direction it drives the Turbine").pointAt(rightNozzleVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightLeftNozzleObject, rightLeftNozzleArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightRightNozzleObject, rightRightNozzleArea, 60);

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, rightOutputObject, rightOutputArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, rightOtherOutputObject, rightOtherOutputArea, 60);
        scene.overlay().showText(60).text("The Turbine outputs rotation at both ends of its axis").pointAt(rightOutputVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void generating(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("tesla_turbine_generating_rotational_force", "Generating Rotational Force with a Tesla Turbine");
        scene.scaleSceneView(0.8F);
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos ironBlockPos = util.grid().at(4, 1, 2);
        BlockPos speedometerPos = ironBlockPos.above();
        BlockPos shaftPos = speedometerPos.south();
        BlockPos centerPos = shaftPos.south();
        BlockPos leftUpPos = centerPos.east().above();
        BlockPos rightDownPos = centerPos.west().below();
        BlockPos leftUpNozzlePos = leftUpPos.above();
        BlockPos leftUpPipeInnerPos = leftUpNozzlePos.above();
        BlockPos leftUpPipeOuterPos = leftUpPipeInnerPos.above();
        BlockPos leftUpMotorPos = leftUpPipeOuterPos.east();
        BlockPos rightUpNozzlePos = leftUpNozzlePos.west(2);
        BlockPos rightUpPipeInnerPos = rightUpNozzlePos.above();
        BlockPos rightUpPipeOuterPos = rightUpPipeInnerPos.above();
        BlockPos rightUpMotorPos = rightUpPipeOuterPos.west();
        BlockPos rightDownNozzlePos = rightDownPos.west();
        BlockPos rightDownPipeInnerPos = rightDownNozzlePos.west();
        BlockPos rightDownPipeOuterPos = rightDownPipeInnerPos.west();
        BlockPos rightDownMotorPos = rightDownPipeOuterPos.south();

        Selection turbineSelection = util.select().fromTo(leftUpPos, rightDownPos);
        Selection meterSelection = util.select().fromTo(ironBlockPos, shaftPos);
        Selection leftUpGasSelection = util.select().fromTo(leftUpNozzlePos, leftUpPipeOuterPos);
        Selection leftUpMotorSelection = util.select().position(leftUpMotorPos);
        Selection leftUpRegulatorSelection = util.select().position(leftUpPipeOuterPos);
        Selection rightUpGasSelection = util.select().fromTo(rightUpNozzlePos, rightUpPipeOuterPos);
        Selection rightUpMotorSelection = util.select().position(rightUpMotorPos);
        Selection rightUpRegulatorSelection = util.select().position(rightUpPipeOuterPos);
        Selection rightDownGasSelection = util.select().fromTo(rightDownNozzlePos, rightDownPipeOuterPos);
        Selection rightDownMotorSelection = util.select().position(rightDownMotorPos);
        Selection rightDownRegulatorSelection = util.select().position(rightDownPipeOuterPos);
        Selection shaftSelection = util.select().fromTo(speedometerPos, centerPos);

        Vec3 centerVec = util.vector().centerOf(centerPos);
        Vec3 leftUpNozzleVec = util.vector().centerOf(leftUpNozzlePos);
        Vec3 rightDownNozzleVec = util.vector().centerOf(rightDownNozzlePos);
        Vec3 rightDownPipeVec = util.vector().centerOf(rightDownPipeOuterPos);
        Vec3 rightUpPipeVec = util.vector().centerOf(rightUpPipeOuterPos);
        Vec3 leftUpPipeVec = util.vector().centerOf(leftUpPipeOuterPos);
        Vec3 rotorControlVec = util.vector().blockSurface(centerPos, Direction.EAST);
        Vec3 speedometerVec = util.vector().centerOf(speedometerPos);

        AABB rightDownArea = new AABB(rightDownPipeVec, rightDownPipeVec);
        AABB rightUpArea = new AABB(rightUpPipeVec, rightUpPipeVec);
        AABB leftUpArea = new AABB(leftUpPipeVec, leftUpPipeVec);
        AABB turbineArea = new AABB(centerVec, centerVec).inflate(0.5);

        Object rightDownObject = new Object();
        Object rightUpObject = new Object();
        Object leftUpObject = new Object();
        Object turbineObject = new Object();
        Object mixedGasObject = new Object();

        ItemStack rotorItem = new ItemStack(CCBItems.TESLA_TURBINE_ROTOR.asItem());

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(turbineSelection, Direction.NORTH);

        scene.idle(3);
        scene.world().showSection(meterSelection, Direction.SOUTH);

        scene.idle(20);
        scene.world().setBlock(rightDownMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.NORTH), false);
        scene.world().showSection(rightDownGasSelection, Direction.EAST);
        scene.world().showSection(rightDownMotorSelection, Direction.NORTH);

        scene.idle(15);
        scene.world().setKineticSpeed(rightDownMotorSelection, mediumSpeed);
        scene.world().setKineticSpeed(rightDownRegulatorSelection, mediumSpeed);
        scene.world().setKineticSpeed(shaftSelection, mediumSpeed / 2);
        scene.effects().rotationSpeedIndicator(rightDownPipeOuterPos);
        scene.effects().rotationSpeedIndicator(speedometerPos);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, rightDownObject, rightDownArea, 3);

        scene.idle(3);
        rightDownArea = rightDownArea.inflate(0.5).expandTowards(2, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, rightDownObject, rightDownArea, 60);
        scene.overlay().showText(60).text("Pressurized gas enters through a Nozzle's outward-facing side").colored(PonderPalette.INPUT).pointAt(rightDownNozzleVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, rightDownObject, rightDownArea, 60);
        scene.overlay().showText(60).text("Gas flows only when source pressure exceeds local atmospheric pressure").colored(PonderPalette.RED).pointAt(rightDownNozzleVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Same-direction Nozzles add their flow and increase rotational output").colored(PonderPalette.GREEN).pointAt(centerVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightDownObject, rightDownArea, 60);

        scene.idle(7);
        scene.world().setBlock(rightUpMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), false);
        scene.world().showSection(rightUpGasSelection, Direction.DOWN);
        scene.world().showSection(rightUpMotorSelection, Direction.EAST);

        scene.idle(15);
        scene.world().setKineticSpeed(rightUpMotorSelection, mediumSpeed);
        scene.world().setKineticSpeed(rightUpRegulatorSelection, mediumSpeed);
        scene.world().setKineticSpeed(shaftSelection, mediumSpeed);
        scene.effects().rotationSpeedIndicator(speedometerPos);
        scene.effects().rotationSpeedIndicator(rightUpPipeOuterPos);
        rightUpArea = rightUpArea.inflate(0.5).expandTowards(0, -2, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightUpObject, rightUpArea, 40);

        scene.idle(60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightUpObject, rightUpArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightDownObject, rightDownArea, 60);
        scene.overlay().showText(60).text("Opposing Nozzles cancel some net flow, reducing rotational output").colored(PonderPalette.RED).pointAt(speedometerVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().setBlock(leftUpMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST), false);
        scene.world().showSection(leftUpGasSelection, Direction.DOWN);
        scene.world().showSection(leftUpMotorSelection, Direction.WEST);

        scene.idle(15);
        scene.world().setKineticSpeed(leftUpMotorSelection, mediumSpeed);
        scene.world().setKineticSpeed(leftUpRegulatorSelection, mediumSpeed);
        scene.world().setKineticSpeed(shaftSelection, mediumSpeed / 2);
        scene.effects().rotationSpeedIndicator(speedometerPos);
        scene.effects().rotationSpeedIndicator(leftUpPipeOuterPos);
        leftUpArea = leftUpArea.inflate(0.5).expandTowards(0, -2, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftUpObject, leftUpArea, 40);

        scene.idle(60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, turbineObject, turbineArea, 60);
        scene.overlay().showText(60).text("Too few Rotors limit both speed and Stress capacity").colored(PonderPalette.RED).pointAt(centerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Adding a Rotor lets the Turbine use more of the available flow").colored(PonderPalette.GREEN).pointAt(centerVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(rotorControlVec, Pointing.RIGHT, 60).rightClick().withItem(rotorItem.copy());
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, turbineObject, turbineArea, 60);

        scene.idle(7);
        scene.world().modifyBlock(centerPos, state -> state.setValue(TeslaTurbineBlock.ROTOR, 2), false);
        scene.world().setKineticSpeed(shaftSelection, mediumSpeed * 0.75F);
        scene.effects().rotationSpeedIndicator(speedometerPos);

        scene.idle(73);
        scene.overlay().showText(60).text("Only compatible gases work; gas type and pressure also limit output").colored(PonderPalette.RED).pointAt(rightDownNozzleVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, rightDownObject, rightDownArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, turbineObject, turbineArea, 60);
        scene.overlay().showText(60).text("The lowest level of supply, Rotors and gas conditions limits output").colored(PonderPalette.RED).pointAt(centerVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Boosting a limiting gas supply raises speed and Stress capacity").colored(PonderPalette.GREEN).pointAt(speedometerVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightDownObject, rightDownArea, 60);

        scene.idle(8);
        scene.world().setKineticSpeed(rightDownMotorSelection, mediumSpeed * 2);
        scene.world().setKineticSpeed(rightDownRegulatorSelection, mediumSpeed * 2);
        scene.effects().rotationSpeedIndicator(rightDownPipeOuterPos);

        scene.idle(8);
        scene.world().setKineticSpeed(shaftSelection, mediumSpeed);
        scene.effects().rotationSpeedIndicator(speedometerPos);

        scene.idle(64);
        scene.overlay().showText(60).text("Mixing gas types makes a running Turbine explode and removes its Rotors").colored(PonderPalette.RED).pointAt(centerVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, mixedGasObject, rightDownArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, leftUpObject, leftUpArea, 60);

        scene.idle(8);
        scene.effects().emitParticles(rightDownNozzleVec, scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0.1, 0, 0)), 1, 8);
        scene.effects().emitParticles(leftUpNozzleVec, scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0, -0.1, 0)), 1, 8);

        scene.idle(12);
        scene.effects().emitParticles(centerVec, scene.effects().simpleParticleEmitter(ParticleTypes.EXPLOSION_EMITTER, Vec3.ZERO), 1, 1);
        scene.effects().emitParticles(centerVec, scene.effects().simpleParticleEmitter(ParticleTypes.LARGE_SMOKE, Vec3.ZERO), 1, 22);
        scene.world().setKineticSpeed(shaftSelection, 0);
        scene.world().modifyBlock(centerPos, state -> state.setValue(TeslaTurbineBlock.ROTOR, 0), false);
        scene.effects().rotationSpeedIndicator(speedometerPos);

        scene.idle(40);
        scene.markAsFinished();
    }
}
