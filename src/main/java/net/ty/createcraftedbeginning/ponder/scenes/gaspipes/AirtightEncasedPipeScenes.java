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

        scene.title("airtight_encased_pipe", "Moving Gases using Airtight Encased Pipes");
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
        Selection rightPipeSelection = util.select().fromTo(rightPipeStartPos, rightPipeEndPos);
        Selection encasedSelection = util.select().position(encasedPos);
        Selection rightTankSelection = util.select().fromTo(rightTankBottomPos, rightTankBottomPos.above());

        Vec3 encasedVec = util.vector().centerOf(encasedPos);

        Object leftPipeObject = new Object();
        Object rightPipeObject = new Object();

        AABB leftPipeArea = new AABB(encasedVec, encasedVec);
        AABB rightPipeArea = new AABB(encasedVec, encasedVec);

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
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftPipeObject, leftPipeArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightPipeObject, rightPipeArea, 3);

        scene.idle(3);
        leftPipeArea = leftPipeArea.inflate(0.3125, 0.3125, 0.5);
        rightPipeArea = rightPipeArea.inflate(0.5, 0.3125, 0.3125);

        scene.idle(3);
        leftPipeArea = leftPipeArea.expandTowards(0, 0, -3);
        rightPipeArea = rightPipeArea.expandTowards(-3, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, leftPipeObject, leftPipeArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, rightPipeObject, rightPipeArea, 60);
        scene.overlay().showText(60).text("The Airtight Encased Pipe can connect in multiple directions").pointAt(encasedVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(encasedPos, Direction.UP), Pointing.DOWN, 67).rightClick().withItem(wrenchItem.copy());

        scene.idle(7);
        scene.world().modifyBlock(encasedPos, state -> state.setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.UP), true), false);
        scene.overlay().showText(60).text("A Wrench can be used to toggle openings on specific sides").colored(PonderPalette.GREEN).pointAt(encasedVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().blockSurface(replacePos, Direction.UP), Pointing.DOWN, 67).rightClick().withItem(encasedPipeItem.copy());

        scene.idle(7);
        scene.world().setBlock(replacePos, CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.getDefaultState(), false);
        scene.overlay().showText(60).text("The Airtight Encased Pipe can directly replace an Airtight Pipe").colored(PonderPalette.GREEN).pointAt(util.vector().centerOf(replacePos)).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
