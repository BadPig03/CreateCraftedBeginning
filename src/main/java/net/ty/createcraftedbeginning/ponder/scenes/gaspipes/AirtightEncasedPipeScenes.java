package net.ty.createcraftedbeginning.ponder.scenes.gaspipes;

import com.simibubi.create.AllItems;
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
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightEncasedPipeScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_encased_pipe", "Configuring Airtight Encased Pipes");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos centerPos = util.grid().at(3, 1, 3);
        BlockPos leftTankBottomPos = centerPos.east(2).north(2);
        BlockPos leftPipeStartPos = leftTankBottomPos.south();
        BlockPos replacePos = leftPipeStartPos.south();
        BlockPos leftPipeEndPos = leftPipeStartPos.south(2);
        BlockPos encasedPos = leftPipeEndPos.south();
        BlockPos rightPipeStartPos = encasedPos.west();
        BlockPos pumpPos = rightPipeStartPos.west();
        BlockPos rightPipeEndPos = pumpPos.west();
        BlockPos rightTankBottomPos = rightPipeEndPos.west();

        Selection leftTankSelection = util.select().fromTo(leftTankBottomPos, leftTankBottomPos.above());
        Selection leftPipeSelection = util.select().fromTo(leftPipeStartPos, leftPipeEndPos);
        Selection encasedSelection = util.select().position(encasedPos);
        Selection rightPipeSelection = util.select().fromTo(rightPipeStartPos, rightPipeEndPos);
        Selection rightTankSelection = util.select().fromTo(rightTankBottomPos, rightTankBottomPos.above());

        Vec3 encasedVec = util.vector().centerOf(encasedPos);
        Vec3 encasedTopVec = util.vector().topOf(encasedPos);
        Vec3 replaceVec = util.vector().centerOf(replacePos);

        AABB northPipeArea = new AABB(encasedVec, encasedVec);
        AABB westPipeArea = new AABB(encasedVec, encasedVec);
        AABB connectionArea = new AABB(encasedVec, encasedVec);
        AABB replaceArea = new AABB(replaceVec, replaceVec);

        Object northPipeObject = new Object();
        Object westPipeObject = new Object();
        Object branchObject = new Object();
        Object disconnectedSideObject = new Object();
        Object remainingSideObject = new Object();
        Object replaceObject = new Object();

        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());
        ItemStack encasedPipeItem = new ItemStack(CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.asItem());

        scene.idle(20);
        scene.world().showSection(leftTankSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(leftPipeSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(encasedSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(rightPipeSelection, Direction.NORTH);

        scene.idle(3);
        scene.world().showSection(rightTankSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, northPipeObject, northPipeArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, westPipeObject, westPipeArea, 3);

        scene.idle(3);
        northPipeArea = northPipeArea.inflate(0.3125, 0.3125, 0.5);
        westPipeArea = westPipeArea.inflate(0.5, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, northPipeObject, northPipeArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, westPipeObject, westPipeArea, 3);

        scene.idle(3);
        northPipeArea = northPipeArea.expandTowards(0, 0, -3);
        westPipeArea = westPipeArea.expandTowards(-3, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, northPipeObject, northPipeArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, westPipeObject, westPipeArea, 60);
        scene.overlay().showText(60).text("Airtight Encased Pipes can connect through multiple faces at once").pointAt(encasedVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("This allows gas networks to turn corners and branch").colored(PonderPalette.GREEN).pointAt(encasedVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Use a Wrench to toggle individual connection faces").colored(PonderPalette.BLUE).pointAt(encasedTopVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().blockSurface(encasedPos, Direction.UP), Pointing.RIGHT, 60).rightClick().withItem(wrenchItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(encasedPos, state -> state.setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.UP), true), false);

        scene.idle(73);
        connectionArea = connectionArea.inflate(0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, branchObject, connectionArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, branchObject, connectionArea.expandTowards(0, 1, 0), 60);
        scene.overlay().showText(60).text("Opening another face allows a connection in that direction").colored(PonderPalette.GREEN).pointAt(encasedTopVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Closing a face disconnects only that side of the network").colored(PonderPalette.RED).pointAt(util.vector().blockSurface(encasedPos, Direction.NORTH)).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().blockSurface(encasedPos, Direction.NORTH), Pointing.RIGHT, 60).rightClick().withItem(wrenchItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(encasedPos, state -> state.setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.NORTH), false), false);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, disconnectedSideObject, northPipeArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, remainingSideObject, westPipeArea, 60);

        scene.idle(73);
        scene.overlay().showText(60).text("Right-click an Airtight Pipe with an Airtight Encased Pipe...").colored(PonderPalette.BLUE).pointAt(replaceVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().blockSurface(replacePos, Direction.UP), Pointing.DOWN, 60).rightClick().withItem(encasedPipeItem.copy());

        scene.idle(30);
        scene.world().setBlock(replacePos, CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.getDefaultState().setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.NORTH), true).setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.SOUTH), true), false);

        scene.idle(30);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, replaceObject, replaceArea, 3);

        scene.idle(3);
        replaceArea = replaceArea.inflate(0.3125, 0.3125, 0.5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, replaceObject, replaceArea, 3);

        scene.idle(3);
        replaceArea = replaceArea.inflate(0, 0, 1);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, replaceObject, replaceArea, 60);
        scene.overlay().showText(60).text("...its axial connections are preserved").colored(PonderPalette.GREEN).pointAt(replaceVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
