package net.ty.createcraftedbeginning.ponder.scenes.gaspipes;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeConnections;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPipeScenes {
    public static void connecting(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_pipe_connecting", "Connecting and Casing Airtight Pipes");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos middlePipePos = util.grid().at(3, 1, 3);
        BlockPos leftPipePos = middlePipePos.east();
        BlockPos rightPipePos = middlePipePos.west();
        BlockPos upPipePos = middlePipePos.above();
        BlockPos frontPipePos = middlePipePos.north();
        BlockPos leftTankPos = middlePipePos.east(2);
        BlockPos rightTankPos = middlePipePos.west(2);

        Selection leftTankSelection = util.select().fromTo(leftTankPos, leftTankPos.above());
        Selection rightTankSelection = util.select().fromTo(rightTankPos, rightTankPos.above());
        Selection upPipeSelection = util.select().position(upPipePos);
        Selection elbowSelection = util.select().fromTo(leftPipePos, middlePipePos).add(upPipeSelection);
        Selection rightPipeSelection = util.select().position(rightPipePos);
        Selection frontPipeSelection = util.select().position(frontPipePos);

        Vec3 middlePipeVec = util.vector().centerOf(middlePipePos);
        Vec3 middlePipeTopVec = util.vector().topOf(middlePipePos);
        Vec3 northFaceVec = util.vector().blockSurface(middlePipePos, Direction.NORTH);
        Vec3 frontPipeVec = util.vector().centerOf(frontPipePos);
        AABB connectionArea = new AABB(northFaceVec, northFaceVec).inflate(0.2);
        Object connectionObject = new Object();

        ItemStack airtightSheetItem = new ItemStack(CCBItems.AIRTIGHT_SHEET.asItem());
        ItemStack waterBucketItem = new ItemStack(Items.WATER_BUCKET);
        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        scene.world().setBlock(frontPipePos, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(rightPipePos, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(middlePipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(AirtightPipeBlock.DOWN, false).setValue(AirtightPipeBlock.EAST, true), false);

        scene.idle(20);
        scene.world().showSection(leftTankSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(elbowSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Airtight Pipes automatically connect and turn toward nearby gas components").colored(PonderPalette.GREEN).pointAt(middlePipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.world().setBlock(rightPipePos, AirtightPipeConnections.straight(CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState(), Axis.X), false);
        scene.world().showSection(rightPipeSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(rightTankSelection, Direction.EAST);

        scene.idle(20);
        scene.world().modifyBlock(middlePipePos, state -> state.setValue(AirtightPipeBlock.WEST, true), false);
        scene.overlay().showText(60).text("Adding more connections forms branches with up to six ports").colored(PonderPalette.GREEN).pointAt(middlePipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(middlePipeTopVec, Pointing.DOWN, 60).rightClick().withItem(airtightSheetItem.copy());
        scene.overlay().showText(60).text("Apply an Airtight Sheet to freeze the current ports without consuming the sheet").colored(PonderPalette.BLUE).pointAt(middlePipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().modifyBlock(middlePipePos, state -> state.setValue(AirtightPipeBlock.CASED, true), false);

        scene.idle(53);
        scene.world().setBlock(frontPipePos, AirtightPipeConnections.straight(CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState(), Axis.Z).setValue(AirtightPipeBlock.CASED, true), false);
        scene.world().showSection(frontPipeSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, connectionObject, connectionArea, 60);
        scene.overlay().showText(60).text("New neighbors do not open cased ports; closed faces keep networks separate").colored(PonderPalette.RED).pointAt(frontPipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.world().hideSection(frontPipeSelection, Direction.NORTH);

        scene.idle(20);
        scene.overlay().showControls(northFaceVec, Pointing.RIGHT, 60).rightClick().withItem(wrenchItem.copy());
        scene.overlay().showText(60).text("Right-click a face with a Wrench to open that port and connect the branch").colored(PonderPalette.BLUE).pointAt(northFaceVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().modifyBlock(middlePipePos, state -> state.setValue(AirtightPipeBlock.NORTH, true), false);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, connectionObject, connectionArea, 53);

        scene.idle(73);
        scene.overlay().showControls(northFaceVec, Pointing.RIGHT, 60).rightClick().withItem(wrenchItem.copy());
        scene.overlay().showText(60).text("Click again to close only that face; the other connections stay open").colored(PonderPalette.BLUE).pointAt(northFaceVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().modifyBlock(middlePipePos, state -> state.setValue(AirtightPipeBlock.NORTH, false), false);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, connectionObject, connectionArea, 53);

        scene.idle(73);
        scene.overlay().showControls(middlePipeTopVec, Pointing.DOWN, 60).showing(AllIcons.I_MTD_CLOSE).withItem(waterBucketItem.copy());
        scene.overlay().showText(60).text("Cased Airtight Pipes cannot be waterlogged").colored(PonderPalette.RED).pointAt(middlePipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(middlePipeTopVec, Pointing.DOWN, 60).rightClick().whileSneaking().withItem(wrenchItem.copy());
        scene.overlay().showText(60).text("Sneak with a Wrench to remove the casing and automatically reconnect nearby components").colored(PonderPalette.BLUE).pointAt(middlePipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().modifyBlock(middlePipePos, state -> state.setValue(AirtightPipeBlock.CASED, false), false);

        scene.idle(53);
        scene.markAsFinished();
    }

    public static void exchange(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_pipe_exchange", "Transporting Gas with Airtight Pipes");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos middlePipePos = util.grid().at(3, 1, 3);
        BlockPos rightPipePos = middlePipePos.west();
        BlockPos leftPipePos = middlePipePos.east();
        BlockPos rightTankBottomPos = middlePipePos.east(2);
        BlockPos rightTankTopPos = middlePipePos.east(2).above();
        BlockPos leftTankBottomPos = middlePipePos.west(2);
        BlockPos leftTankTopPos = middlePipePos.west(2).above();
        BlockPos airPos = middlePipePos.west(2);

        Selection pipeSelection = util.select().fromTo(leftPipePos, rightPipePos);
        Selection rightTankSelection = util.select().fromTo(rightTankBottomPos, rightTankTopPos);
        Selection leftTankSelection = util.select().fromTo(leftTankBottomPos, leftTankTopPos);

        Vec3 middlePipeVec = util.vector().centerOf(middlePipePos);
        Vec3 airVec = util.vector().centerOf(airPos);
        Vec3 openEndVec = util.vector().blockSurface(rightPipePos, Direction.WEST);
        Vec3 leftTankBottomVec = util.vector().centerOf(leftTankBottomPos);
        Vec3 leftTankTopVec = util.vector().centerOf(leftTankTopPos);
        Vec3 rightTankBottomVec = util.vector().centerOf(rightTankBottomPos);
        Vec3 rightTankTopVec = util.vector().centerOf(rightTankTopPos);
        Vec3 leftPipeVec = util.vector().centerOf(leftPipePos);
        Vec3 rightPipeVec = util.vector().centerOf(rightPipePos);

        AABB leftTankArea = new AABB(leftTankBottomVec, leftTankTopVec);
        AABB rightTankArea = new AABB(rightTankBottomVec, rightTankTopVec);
        AABB pipeArea = new AABB(leftPipeVec, rightPipeVec);
        AABB atmosphereArea = new AABB(airVec, airVec);

        Object highPressureObject = new Object();
        Object lowPressureObject = new Object();
        Object pipeObject = new Object();
        Object atmosphereObject = new Object();

        scene.idle(20);
        scene.world().showSection(rightTankSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(pipeSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(leftTankSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().showText(60).text("Airtight Pipes can connect gas containers into the same network").pointAt(middlePipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(77);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, highPressureObject, leftTankArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, lowPressureObject, rightTankArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, pipeObject, pipeArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, highPressureObject, leftTankArea.inflate(0.5), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, lowPressureObject, rightTankArea.inflate(0.5), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, pipeObject, pipeArea.inflate(0.5, 0.3125, 0.3125), 60);
        scene.overlay().showText(60).text("When pressures differ, gas flows from higher pressure toward lower pressure").pointAt(middlePipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Gas can flow through the network without an Airtight Pump").colored(PonderPalette.GREEN).pointAt(middlePipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.world().hideSection(leftTankSelection, Direction.UP);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, atmosphereObject, atmosphereArea, 3);

        scene.idle(3);
        atmosphereArea = atmosphereArea.inflate(0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, atmosphereObject, atmosphereArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, atmosphereObject, atmosphereArea.inflate(1, 0, 0), 60);
        scene.overlay().showText(60).text("Open pipe ends connect the network to the surrounding atmosphere").pointAt(openEndVec).placeNearTarget().attachKeyFrame();

        scene.idle(77);
        atmosphereArea = atmosphereArea.move(1, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, atmosphereObject, atmosphereArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, atmosphereObject, atmosphereArea.expandTowards(-2, 0, 0), 60);
        scene.overlay().showText(60).text("When network pressure exceeds atmospheric pressure, gas escapes through the open end").colored(PonderPalette.OUTPUT).pointAt(openEndVec).placeNearTarget().attachKeyFrame();

        scene.idle(77);
        atmosphereArea = atmosphereArea.move(-2, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, atmosphereObject, atmosphereArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, atmosphereObject, atmosphereArea.expandTowards(2, 0, 0), 60);
        scene.overlay().showText(60).text("Below atmospheric pressure, the open end draws in ambient gas").colored(PonderPalette.INPUT).pointAt(openEndVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }
}
