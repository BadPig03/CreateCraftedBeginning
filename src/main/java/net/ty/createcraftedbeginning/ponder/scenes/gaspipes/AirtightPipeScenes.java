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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPipeScenes {
    public static void connecting(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("airtight_pipe_connecting", "Connecting Airtight Pipes");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos middlePipePos = util.grid().at(3, 1, 3);
        BlockPos leftPipePos = middlePipePos.east();
        BlockPos rightPipePos = middlePipePos.west();
        BlockPos upPipePos = middlePipePos.above();
        BlockPos frontPipePos = middlePipePos.north();
        BlockPos leftTankBottomPos = middlePipePos.east(2);
        BlockPos rightTankBottomPos = middlePipePos.west(2);

        Selection leftTankSelection = util.select().fromTo(leftTankBottomPos, leftTankBottomPos.above());
        Selection rightTankSelection = util.select().fromTo(rightTankBottomPos, rightTankBottomPos.above());
        Selection pipeSelection = util.select().fromTo(leftPipePos, rightPipePos);
        Selection upPipeSelection = util.select().position(upPipePos);
        Selection frontPipeSelection = util.select().position(frontPipePos);

        Vec3 middlePipeVec = util.vector().centerOf(middlePipePos);
        Vec3 leftPipeVec = util.vector().centerOf(leftPipePos);
        Vec3 rightPipeVec = util.vector().centerOf(rightPipePos);

        AABB pipeArea = new AABB(leftPipeVec, rightPipeVec);
        AABB connectionArea = new AABB(middlePipeVec, middlePipeVec).inflate(0.16666667);

        Object pipeObject = new Object();
        Object upConnectionObject = new Object();
        Object frontConnectionObject = new Object();

        ItemStack airtightSheetItem = new ItemStack(CCBItems.AIRTIGHT_SHEET.asItem());
        ItemStack waterBucketItem = new ItemStack(Items.WATER_BUCKET);
        ItemStack wrenchItem = new ItemStack(AllItems.WRENCH.asItem());

        scene.idle(20);
        scene.world().showSection(leftTankSelection, Direction.WEST);

        scene.idle(3);
        scene.world().showSection(pipeSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(rightTankSelection, Direction.EAST);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, pipeObject, pipeArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, pipeObject, pipeArea.inflate(0.5, 0.3125, 0.3125), 60);
        scene.overlay().showText(60).text("Airtight Pipes connect along their axis").colored(PonderPalette.GREEN).pointAt(middlePipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(65);
        scene.world().showSection(upPipeSelection, Direction.DOWN);
        scene.world().showSection(frontPipeSelection, Direction.SOUTH);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, upConnectionObject, connectionArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, frontConnectionObject, connectionArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, upConnectionObject, connectionArea.move(0, 0.5, 0), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, frontConnectionObject, connectionArea.move(0, 0, -0.5), 60);
        scene.overlay().showText(60).text("Pipes placed against their sides will not connect").colored(PonderPalette.RED).pointAt(Vec3.atCenterOf(upPipePos)).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().topOf(middlePipePos), Pointing.DOWN, 60).rightClick().withItem(airtightSheetItem.copy());
        scene.overlay().showText(60).text("Airtight Sheets can be used to add casing without being consumed").colored(PonderPalette.BLUE).pointAt(middlePipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(25);
        scene.world().modifyBlock(middlePipePos, state -> state.setValue(AirtightPipeBlock.CASED, true), false);

        scene.idle(55);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, upConnectionObject, connectionArea.move(0, 0.5, 0), 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, frontConnectionObject, connectionArea.move(0, 0, -0.5), 60);
        scene.overlay().showText(60).text("Casing does not add side connections; the pipe remains axial").colored(PonderPalette.RED).pointAt(middlePipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().topOf(middlePipePos), Pointing.DOWN, 60).showing(AllIcons.I_MTD_CLOSE).withItem(waterBucketItem.copy());
        scene.overlay().showText(60).text("Once cased, Airtight Pipes cannot be waterlogged").colored(PonderPalette.RED).pointAt(middlePipeVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showControls(util.vector().topOf(middlePipePos), Pointing.DOWN, 60).rightClick().withItem(wrenchItem.copy());
        scene.overlay().showText(60).text("Use a Wrench to remove the casing").colored(PonderPalette.BLUE).pointAt(middlePipeVec).placeNearTarget().attachKeyFrame();

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
        BlockPos rightTankBottomPos = leftPipePos.east();
        BlockPos rightTankTopPos = rightTankBottomPos.above();
        BlockPos leftTankBottomPos = rightPipePos.west();
        BlockPos leftTankTopPos = leftTankBottomPos.above();
        BlockPos airPos = rightPipePos.west();

        Selection pipeSelection = util.select().fromTo(leftPipePos, rightPipePos);
        Selection rightTankSelection = util.select().fromTo(rightTankBottomPos, rightTankTopPos);
        Selection leftTankSelection = util.select().fromTo(leftTankBottomPos, leftTankTopPos);

        Vec3 middlePipeVec = util.vector().centerOf(middlePipePos);
        Vec3 airVec = util.vector().centerOf(airPos);
        Vec3 openEndVec = util.vector().blockSurface(rightPipePos, Direction.WEST);

        AABB leftTankArea = new AABB(util.vector().centerOf(leftTankBottomPos), util.vector().centerOf(leftTankTopPos));
        AABB rightTankArea = new AABB(util.vector().centerOf(rightTankBottomPos), util.vector().centerOf(rightTankTopPos));
        AABB pipeArea = new AABB(util.vector().centerOf(leftPipePos), util.vector().centerOf(rightPipePos));
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
