package net.ty.createcraftedbeginning.ponder.scenes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

public class AirtightEncasedPipeScenes {
    public static void scene(SceneBuilder builder, @NotNull SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_encased_pipe", "Moving Compressed Air using Airtight Encased Pipes");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos encasedPos = util.grid().at(2, 2, 2);
        BlockPos lowerTankPos = util.grid().at(4, 1, 2);
        BlockPos upperTankPos = util.grid().at(2, 2, 4);
        BlockPos pumpPos = util.grid().at(3, 2, 2);
        BlockPos pipePos = util.grid().at(2, 2, 3);
        BlockPos cogPos = util.grid().at(3, 1, 2);
        BlockPos motorPos = util.grid().at(2, 1, 2);
        BlockPos newBlockPos = util.grid().at(2, 3, 2);

        Selection creativeTankSelection = util.select().fromTo(lowerTankPos, lowerTankPos.above());
        Selection tankSelection = util.select().fromTo(upperTankPos, upperTankPos.below());
        Selection pipeSelection = util.select().fromTo(pumpPos, pipePos);
        Selection pumpSelection = util.select().fromTo(pumpPos, pumpPos);
        Selection sourceSelection = util.select().fromTo(cogPos, motorPos);
        Selection newBlockSelection = util.select().fromTo(newBlockPos, newBlockPos);

        Vec3 opening = util.vector().blockSurface(encasedPos, Direction.NORTH);

        ItemStack sheetItem = new ItemStack(CCBItems.AIRTIGHT_SHEET.asItem());
        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        AABB bb = new AABB(util.vector().centerOf(encasedPos), util.vector().centerOf(encasedPos)).inflate(1 / 4f);
		AABB bb1 = bb.move(0, 1 / 2f, 0);
        AABB bb2 = bb.move(0, 0, - 1 / 2f);
        AABB bb3 = bb.move(-1 / 2f, 0, 0);

        scene.world().setBlock(newBlockPos, AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState(), false);
        scene.world().setBlock(cogPos, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST), false);

        scene.world().showSection(creativeTankSelection, Direction.DOWN);
        scene.world().showSection(tankSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().showSection(sourceSelection, Direction.DOWN);

        scene.idle(5);
        scene.world().showSection(pipeSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().setKineticSpeed(sourceSelection, 64);
        scene.world().setKineticSpeed(pumpSelection, -64);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.overlay().showText(60).text("Unlike regular Airtight Pipes, Airtight Encased Pipes can connect in any direction").colored(PonderPalette.INPUT).pointAt(Vec3.atCenterOf(encasedPos)).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, bb1, bb, 1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, bb2, bb, 1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, bb3, bb, 1);

        scene.idle(1);
		scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, bb1, bb1, 59);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, bb2, bb2, 59);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, bb3, bb3, 59);

        scene.idle(79);
        scene.overlay().showText(60).text("Using an Airtight Sheet on an opening prevents gas from leaking through it").pointAt(Vec3.atCenterOf(encasedPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.overlay().showControls(opening, Pointing.RIGHT, 40).rightClick().withItem(sheetItem);

        scene.idle(7);
        scene.world().modifyBlock(encasedPos, s -> s.setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.NORTH), false), false);

        scene.idle(53);
        ElementLink<WorldSectionElement> block = scene.world().showIndependentSection(newBlockSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showOutline(PonderPalette.OUTPUT, new Object(), newBlockSelection, 60);
        scene.overlay().showText(60).text("Placing a block against the opening also prevents gas leakage").colored(PonderPalette.OUTPUT).pointAt(util.vector().blockSurface(encasedPos, Direction.UP)).placeNearTarget();

        scene.idle(80);
        scene.overlay().showText(60).text("The Wrench can then be used to remove the Airtight Sheet used for sealing").colored(PonderPalette.INPUT).pointAt(Vec3.atCenterOf(encasedPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.overlay().showControls(opening, Pointing.RIGHT, 40).rightClick().withItem(wrenchItem);

        scene.idle(7);
        scene.world().modifyBlock(encasedPos, s -> s.setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.NORTH), true), false);

        scene.idle(53);
        scene.world().hideIndependentSection(block, Direction.UP);

        scene.idle(20);
        scene.world().multiplyKineticSpeed(sourceSelection, 4);
        scene.world().multiplyKineticSpeed(pumpSelection, 4);
        scene.effects().rotationSpeedIndicator(motorPos);
        scene.effects().rotationSpeedIndicator(pumpPos);
        scene.overlay().showText(60).text("Warning!\nExcessive openings combined with high rotational speeds trigger violent explosions").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(encasedPos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
