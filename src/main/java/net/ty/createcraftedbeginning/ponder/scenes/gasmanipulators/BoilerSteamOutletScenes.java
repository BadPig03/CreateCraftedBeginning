package net.ty.createcraftedbeginning.ponder.scenes.gasmanipulators;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BoilerSteamOutletScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("boiler_steam_outlet", "Extracting Steam from Boilers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos basePos = util.grid().at(1, 1, 3);
        BlockPos tankPos = basePos.above();
        BlockPos pipePos = tankPos.east();
        BlockPos pumpPos = pipePos.east();
        BlockPos outletPos = pumpPos.east();
        BlockPos boilerTankPos = outletPos.east();
        BlockPos tankTopPos = boilerTankPos.east().south().above();
        BlockPos burnerPos = boilerTankPos.below();
        BlockPos burnerRightPos = burnerPos.east().south();
        BlockPos enginePos = boilerTankPos.north().above();
        BlockPos cogPos = pumpPos.south();
        BlockPos motorPos = cogPos.west();
        BlockPos redstonePos = outletPos.below();
        BlockPos fluidPipePos = enginePos.above().south(2);
        BlockPos fluidPumpPos = fluidPipePos.above(2);
        BlockPos fluidCogPos = fluidPumpPos.west();
        BlockPos fluidMotorPos = fluidCogPos.below();

        Selection boilerSelection = util.select().fromTo(boilerTankPos, tankTopPos);
        Selection burnerSelection = util.select().fromTo(burnerPos, burnerRightPos);
        Selection outletSelection = util.select().position(outletPos);
        Selection engineSelection = util.select().position(enginePos);
        Selection tankSelection = util.select().fromTo(basePos, tankPos);
        Selection pipeSelection = util.select().fromTo(pipePos, pumpPos);
        Selection pumpSelection = util.select().position(pumpPos);
        Selection sourceSelection = util.select().fromTo(cogPos, motorPos);
        Selection redstoneSelection = util.select().position(redstonePos);
        Selection fluidPumpSelection = util.select().fromTo(fluidPipePos, fluidPumpPos);
        Selection fluidSourceSelection = util.select().fromTo(fluidCogPos, fluidMotorPos);

        Vec3 outletVec = util.vector().centerOf(outletPos);
        Vec3 engineVec = util.vector().centerOf(enginePos);
        Vec3 pumpVec = util.vector().centerOf(pumpPos);

        AABB outletArea = new AABB(outletVec, outletVec);
        AABB engineArea = new AABB(engineVec, engineVec);

        Object outletObject = new Object();
        Object engineObject = new Object();

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(burnerSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(boilerSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(outletSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(fluidPumpSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().setBlock(fluidMotorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.UP), false);
        scene.world().showSection(fluidSourceSelection, Direction.DOWN);

        scene.idle(15);
        scene.world().setKineticSpeed(fluidSourceSelection, mediumSpeed);
        scene.world().setKineticSpeed(fluidPumpSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(fluidPumpPos);
        scene.effects().rotationSpeedIndicator(fluidMotorPos);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, outletObject, outletArea, 3);

        scene.idle(3);
        outletArea = outletArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, outletObject, outletArea, 60);
        scene.overlay().showText(60).text("Boiler Steam Outlets must be attached to a Fluid Tank").pointAt(outletVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("The Outlet converts the boiler capacity assigned to it into Steam").colored(PonderPalette.GREEN).pointAt(outletVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(engineSelection, Direction.SOUTH);

        scene.idle(10);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, engineObject, engineArea, 3);

        scene.idle(3);
        engineArea = engineArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, engineObject, engineArea, 60);
        scene.overlay().showText(60).text("Steam Engines and Boiler Steam Outlets on the same boiler share its available capacity").colored(PonderPalette.INPUT).pointAt(engineVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(pipeSelection, Direction.EAST);

        scene.idle(3);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), false);
        scene.world().showSection(sourceSelection, Direction.NORTH);

        scene.idle(15);
        scene.world().setKineticSpeed(sourceSelection, mediumSpeed);
        scene.world().setKineticSpeed(pumpSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.overlay().showText(60).text("Use Airtight Pumps to draw the generated Steam into the gas network").colored(PonderPalette.GREEN).pointAt(pumpVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Surplus Steam is not stored inside the Outlet").colored(PonderPalette.RED).pointAt(outletVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(redstoneSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().modifyBlock(redstonePos, state -> state.setValue(LeverBlock.POWERED, true), false);
        scene.effects().indicateRedstone(redstonePos);
        scene.overlay().showText(60).text("Redstone power disables the Outlet and releases its share back to the boiler").colored(PonderPalette.RED).pointAt(outletVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
