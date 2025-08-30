package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.ty.createcraftedbeginning.content.airtightengine.AirtightEngineBlock;
import net.ty.createcraftedbeginning.content.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

public class AirtightEngineScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_engine", "Setting up Airtight Engines");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos lowerTankPos = util.grid().at(3, 1, 3);
        BlockPos upperTankPos = util.grid().at(3, 3, 3);
        BlockPos enginePos = util.grid().at(3, 4, 3);
        BlockPos cogPos = util.grid().at(2, 4, 2);
        BlockPos pipePos = util.grid().at(3, 1, 2);
        BlockPos pumpPos = util.grid().at(3, 1, 1);
        BlockPos pipe2Pos = util.grid().at(3, 1, 0);
        BlockPos cog2Pos = util.grid().at(4, 1, 1);
        BlockPos motorPos = util.grid().at(4, 1, 0);

        Selection tankSelection = util.select().fromTo(lowerTankPos, upperTankPos);
        Selection engineSelection = util.select().fromTo(enginePos, enginePos);
        Selection cogSelection = util.select().fromTo(cogPos, cogPos);
        Selection pumpSelection = util.select().fromTo(pumpPos, pipe2Pos);
        Selection sourceSelection = util.select().fromTo(cog2Pos, motorPos);
        Selection pipeSelection = util.select().fromTo(pipePos, pipePos);
        Selection largeTankSelection = util.select().fromTo(util.grid().at(2, 1, 4), util.grid().at(0, 4, 6));
        Selection newLargeTankSelection = util.select().fromTo(util.grid().at(4, 1, 2), util.grid().at(2, 4, 4));

        scene.world().setBlock(enginePos, CCBBlocks.AIRTIGHT_ENGINE_BLOCK.getDefaultState().setValue(AirtightEngineBlock.AXIS, Direction.Axis.Y).setValue(AirtightEngineBlock.FACE, AttachFace.FLOOR).setValue(AirtightEngineBlock.FACING, Direction.NORTH), false);
        scene.world().setBlock(cogPos, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.Y), false);
        scene.world().setBlock(pumpPos, CCBBlocks.AIRTIGHT_PUMP_BLOCK.getDefaultState().setValue(AirtightPumpBlock.FACING, Direction.SOUTH), false);
        scene.world().setBlock(pipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(AirtightPipeBlock.AXIS, Direction.Axis.Z), false);
        scene.world().setBlock(pipe2Pos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(AirtightPipeBlock.AXIS, Direction.Axis.Z), false);
        scene.world().setBlock(cog2Pos, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.Z), false);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH), false);

        scene.idle(10);
        ElementLink<WorldSectionElement> tank = scene.world().showIndependentSection(tankSelection, Direction.DOWN);

        scene.idle(10);
        ElementLink<WorldSectionElement> engine = scene.world().showIndependentSection(engineSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Airtight Engines can be placed on an Airtight Tank").pointAt(util.vector().centerOf(enginePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        ElementLink<WorldSectionElement> cog = scene.world().showIndependentSection(cogSelection, Direction.DOWN);

        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.OUTPUT, new Object(), cogSelection, 60);
        scene.overlay().showText(60).text("The Rotational Force of the Airtight Engine can be output through Cogwheels").colored(PonderPalette.OUTPUT).pointAt(util.vector().centerOf(cogPos)).placeNearTarget();

        scene.idle(80);
        scene.world().hideIndependentSection(cog, Direction.UP);
        ElementLink<WorldSectionElement> pipe = scene.world().showIndependentSection(pipeSelection, Direction.DOWN);
        scene.world().showSection(pumpSelection, Direction.DOWN);
        scene.world().showSection(sourceSelection, Direction.DOWN);
        scene.world().setKineticSpeed(pumpSelection, 30);
        scene.world().setKineticSpeed(sourceSelection, -30);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.effects().rotationSpeedIndicator(motorPos);

        scene.idle(10);
        scene.overlay().showText(40).text("With sufficient Compressed Air and space...").colored(PonderPalette.INPUT).pointAt(util.vector().centerOf(pumpPos)).placeNearTarget().attachKeyFrame();

        scene.idle(45);
        scene.overlay().showText(40).text("...Airtight Engines will generate Rotational Force").colored(PonderPalette.INPUT).pointAt(util.vector().centerOf(enginePos)).placeNearTarget();
        scene.world().setKineticSpeed(engineSelection, 8);
        scene.effects().rotationSpeedIndicator(enginePos);

        scene.idle(70);
        scene.overlay().showOutline(PonderPalette.GREEN, new Object(), tankSelection, 60);
        scene.overlay().showText(60).text("The minimal setup requires 3 Airtight Tanks").colored(PonderPalette.GREEN).pointAt(util.vector().centerOf(upperTankPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().hideIndependentSection(tank, Direction.WEST);
        scene.world().hideIndependentSection(pipe, Direction.WEST);

        scene.idle(10);
        scene.world().moveSection(engine, util.vector().of(0, 1, 0), 10);

        scene.idle(5);
        ElementLink<WorldSectionElement> largeTank = scene.world().showIndependentSection(largeTankSelection, Direction.EAST);
        scene.world().moveSection(largeTank, util.vector().of(2, 0, -2), 0);

        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.FAST, new Object(), newLargeTankSelection, 60);
        scene.overlay().showText(60).text("While supporting a maximum structure of 3x3x4").colored(PonderPalette.FAST).pointAt(util.vector().centerOf(lowerTankPos)).placeNearTarget();

        scene.idle(80);
        scene.world().multiplyKineticSpeed(sourceSelection, 3);
        scene.world().multiplyKineticSpeed(pumpSelection, 3);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.overlay().showText(60).text("If supplied with Compressed Air, the Airtight Drive Assembly will remain passive state regardless of input quantity").colored(PonderPalette.RED).pointAt(util.vector().centerOf(pumpPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
