package net.ty.createcraftedbeginning.ponder.scenes.gaspipes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public class AirtightPipeScenes {
    @SuppressWarnings("ConstantExpression")
    public static void moving(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_pipe_moving", "Moving Gases using Airtight Pipes");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos middlePipePos = util.grid().at(3, 1, 3);
        BlockPos cogPos = middlePipePos.south();
        BlockPos motorPos = cogPos.east();
        BlockPos leftPipePos = middlePipePos.east();
        BlockPos rightPipePos = middlePipePos.west();
        BlockPos upPipePos = middlePipePos.above();
        BlockPos frontPos = middlePipePos.north();
        BlockPos leftTankBottomPos = middlePipePos.east(2);
        BlockPos rightTankBottomPos = middlePipePos.west(2);

        Selection leftTankSelection = util.select().fromTo(leftTankBottomPos, leftTankBottomPos.above());
        Selection rightTankSelection = util.select().fromTo(rightTankBottomPos, rightTankBottomPos.above());
        Selection pipeSelection = util.select().fromTo(leftPipePos, rightPipePos);
        Selection upPipeSelection = util.select().position(upPipePos);
        Selection frontPipeSelection = util.select().position(frontPos);
        Selection cogSelection = util.select().fromTo(cogPos, motorPos);
        Selection pumpSelection = util.select().position(middlePipePos);

        AABB pipeArea = new AABB(util.vector().centerOf(leftPipePos), util.vector().centerOf(rightPipePos));
        AABB pumpArea = new AABB(util.vector().centerOf(leftPipePos), util.vector().centerOf(leftPipePos));
        AABB connectionArea = new AABB(util.vector().centerOf(middlePipePos), util.vector().centerOf(middlePipePos)).inflate(1 / 6.0f);

        Object pipeObject = new Object();
        Object upConnectionObject = new Object();
        Object frontConnectionObject = new Object();
        Object backConnectionObject = new Object();
        Object pumpObject = new Object();

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(leftTankSelection, Direction.WEST);

        scene.idle(3);
        scene.world().setBlock(middlePipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(AirtightPipeBlock.AXIS, Axis.X), false);
        scene.world().showSection(pipeSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(rightTankSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, pipeObject, pipeArea, 3);

        scene.idle(3);
        pipeArea = pipeArea.inflate(0.5, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, pipeObject, pipeArea, 60);
        scene.overlay().showText(60).text("Airtight Pipes can connect two or more gas sources and targets").pointAt(Vec3.atCenterOf(middlePipePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().showSection(upPipeSelection, Direction.DOWN);
        scene.world().showSection(frontPipeSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, upConnectionObject, connectionArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, frontConnectionObject, connectionArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, backConnectionObject, connectionArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, upConnectionObject, connectionArea.move(0, 0.5, 0), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, frontConnectionObject, connectionArea.move(0, 0, 0.5), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, backConnectionObject, connectionArea.move(0, 0, -0.5), 60);
        scene.overlay().showText(60).text("However, the Pipes will not connect to any other adjacent pipe segments").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(upPipePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().restoreBlocks(pumpSelection);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST), false);
        scene.world().setBlock(middlePipePos, CCBBlocks.AIRTIGHT_PUMP_BLOCK.getDefaultState().setValue(AirtightPumpBlock.FACING, Direction.WEST), true);
        scene.world().hideSection(upPipeSelection, Direction.UP);
        scene.world().hideSection(frontPipeSelection, Direction.NORTH);
        scene.world().showSection(cogSelection, Direction.NORTH);
        scene.world().setKineticSpeed(cogSelection, mediumSpeed);
        scene.world().setKineticSpeed(pipeSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(middlePipePos);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, pumpObject, pumpArea, 3);

        scene.idle(3);
        pumpArea = pumpArea.inflate(0.5, 0.375, 0.375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, pumpObject, pumpArea, 3);

        scene.idle(3);
        pumpArea = pumpArea.expandTowards(-2, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, pumpObject, pumpArea, 60);
        scene.overlay().showText(60).text("Powered by Airtight Pumps, the Pipes can moving Gases").colored(PonderPalette.INPUT).pointAt(Vec3.atCenterOf(middlePipePos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    public static void interaction(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_pipe_interaction", "Draining and Filling Gas Containers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos pumpPos = util.grid().at(3, 1, 3);
        BlockPos rightPipePos = pumpPos.west();
        BlockPos leftPipePos = pumpPos.east();
        BlockPos tankBottomPos = leftPipePos.east();
        BlockPos tankTopPos = tankBottomPos.above();
        BlockPos cogPos = pumpPos.south();
        BlockPos motorPos = cogPos.east();
        BlockPos airPos = rightPipePos.west();

        Selection pumpSelection = util.select().position(pumpPos);
        Selection pipeSelection = util.select().fromTo(leftPipePos, rightPipePos);
        Selection tankSelection = util.select().fromTo(tankBottomPos, tankTopPos);
        Selection cogSelection = util.select().fromTo(cogPos, motorPos);

        AABB tankArea = new AABB(util.vector().centerOf(airPos), util.vector().centerOf(airPos));
        AABB airArea = new AABB(util.vector().centerOf(airPos), util.vector().centerOf(airPos));

        Object tankObject = new Object();
        Object airObject = new Object();

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();

        scene.idle(20);
        scene.world().showSection(pipeSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(tankSelection, Direction.WEST);
        ElementLink<WorldSectionElement> tankSection = scene.world().showIndependentSection(tankSelection, Direction.EAST);
        scene.world().moveSection(tankSection, util.vector().of(-4, 0, 0), 0);

        scene.idle(20);
        scene.overlay().showText(60).text("Endpoints of an Airtight Pipe network can interact with a variety of gas containers").pointAt(Vec3.atCenterOf(rightPipePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 3);

        scene.idle(3);
        tankArea = tankArea.inflate(0.5, 0.5, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 3);

        scene.idle(3);
        tankArea = tankArea.expandTowards(0, 1, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, tankObject, tankArea, 60);
        scene.overlay().showText(60).text("Any block with gas storage capabilities can be filled or drained").pointAt(Vec3.atCenterOf(airPos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST), false);
        scene.world().hideIndependentSection(tankSection, Direction.UP);
        scene.world().showSection(cogSelection, Direction.NORTH);
        scene.world().setKineticSpeed(cogSelection, mediumSpeed);
        scene.world().setKineticSpeed(pumpSelection, -mediumSpeed);
        scene.effects().rotationSpeedIndicator(pumpPos);

        scene.idle(20);
        airArea = airArea.inflate(0.5, 0.375, 0.375);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, airObject, airArea, 3);

        scene.idle(3);
        airArea = airArea.expandTowards(3, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, airObject, airArea, 60);
        scene.overlay().showText(60).text("Powered by Airtight Pumps, the Pipes can extract gases from the air").colored(PonderPalette.INPUT).pointAt(Vec3.atCenterOf(airPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
