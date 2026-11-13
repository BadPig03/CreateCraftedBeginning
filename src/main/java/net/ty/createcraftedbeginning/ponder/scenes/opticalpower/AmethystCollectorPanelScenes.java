package net.ty.createcraftedbeginning.ponder.scenes.opticalpower;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.opticalfiber.OpticalFiberBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystCollectorPanelScenes {
    private AmethystCollectorPanelScenes() {
    }

    public static void assembly(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("amethyst_collector_panel_assembly", "Building Amethyst Collector Arrays");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos collectorPos = util.grid().at(0, 1, 1);
        BlockPos firstRowStartPos = collectorPos.east();
        BlockPos firstRowEndPos = collectorPos.east(2);
        BlockPos secondRowStartPos = collectorPos.south();
        BlockPos secondRowEndPos = collectorPos.east(2).south();
        BlockPos loosePanelPos = collectorPos.south(2);
        BlockPos thirdRowStartPos = collectorPos.east().south(2);
        BlockPos thirdRowEndPos = collectorPos.east(2).south(2);
        BlockPos northExpansionStartPos = collectorPos.south(3);
        BlockPos northExpansionEndPos = collectorPos.east(2).south(4);
        BlockPos eastExpansionStartPos = collectorPos.east(3);
        BlockPos arrayEndPos = collectorPos.east(4).south(4);
        BlockPos excessPanelPos = collectorPos.east(5);
        BlockPos fiberPos = collectorPos.north();
        BlockPos secondFiberPos = collectorPos.east().north();
        BlockPos thirdFiberPos = collectorPos.east(2).north();
        BlockPos emitterPos = collectorPos.east(3).north();
        BlockPos targetPos = collectorPos.east(6).north();

        Selection singlePanelSelection = util.select().position(collectorPos);
        Selection expandedSelection = util.select().fromTo(firstRowStartPos, firstRowEndPos).add(util.select().fromTo(secondRowStartPos, secondRowEndPos));
        Selection loosePanelSelection = util.select().position(loosePanelPos);
        Selection thirdRowSelection = util.select().fromTo(thirdRowStartPos, thirdRowEndPos);
        Selection northExpansionSelection = util.select().fromTo(northExpansionStartPos, northExpansionEndPos);
        Selection eastExpansionSelection = util.select().fromTo(eastExpansionStartPos, arrayEndPos);
        Selection excessPanelSelection = util.select().position(excessPanelPos);
        Selection fiberSelection = util.select().fromTo(fiberPos, thirdFiberPos);
        Selection supplySelection = util.select().fromTo(fiberPos, emitterPos).add(util.select().position(targetPos));
        Selection laterSelection = expandedSelection.copy().add(loosePanelSelection).add(thirdRowSelection).add(northExpansionSelection).add(eastExpansionSelection).add(excessPanelSelection);

        Vec3 collectorVec = util.vector().topOf(collectorPos);
        Vec3 rectangleVec = util.vector().topOf(secondRowEndPos);
        Vec3 gapVec = util.vector().topOf(thirdRowStartPos);
        Vec3 arrayVec = util.vector().topOf(thirdRowEndPos);
        Vec3 excessPanelVec = util.vector().topOf(excessPanelPos);

        AABB arrayStartArea = new AABB(collectorPos);
        AABB twoRowsArea = new AABB(collectorPos).minmax(new AABB(secondRowEndPos));
        AABB threeRowsArea = new AABB(collectorPos).minmax(new AABB(thirdRowEndPos));
        AABB fullArrayArea = new AABB(collectorPos).minmax(new AABB(arrayEndPos));
        AABB excessPanelArea = new AABB(excessPanelPos);

        Object arrayObject = new Object();
        Object excessObject = new Object();

        scene.world().setBlocks(laterSelection, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlocks(fiberSelection, CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState(), false);

        scene.idle(20);
        scene.world().showSection(singlePanelSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(supplySelection, Direction.DOWN);

        scene.idle(20);
        scene.world().restoreBlocks(fiberSelection);
        scene.world().modifyBlock(secondFiberPos, state -> state.setValue(OpticalFiberBlock.SOUTH, false), false);
        scene.world().modifyBlock(thirdFiberPos, state -> state.setValue(OpticalFiberBlock.SOUTH, false), false);
        setPower(scene, emitterPos, 1);
        scene.overlay().showText(60).text("Amethyst Collector Panels gather sunlight to supply devices through Optical Fibers").colored(PonderPalette.GREEN).pointAt(collectorVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Join panels side by side at the same height to build an array").colored(PonderPalette.GREEN).pointAt(rectangleVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().restoreBlocks(expandedSelection);
        scene.world().showSection(expandedSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().modifyBlock(secondFiberPos, state -> state.setValue(OpticalFiberBlock.SOUTH, true), false);
        scene.world().modifyBlock(thirdFiberPos, state -> state.setValue(OpticalFiberBlock.SOUTH, true), false);
        setPower(scene, emitterPos, 6);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, arrayObject, arrayStartArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, arrayObject, twoRowsArea, 30);

        scene.idle(50);
        scene.overlay().showText(60).text("Only the array's largest complete rectangle gathers sunlight").colored(PonderPalette.BLUE).pointAt(rectangleVec).placeNearTarget().attachKeyFrame();
        scene.world().restoreBlocks(loosePanelSelection);
        scene.world().showSection(loosePanelSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, arrayObject, arrayStartArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, arrayObject, twoRowsArea, 84);

        scene.idle(57);
        scene.overlay().showText(60).text("Fill the gaps to enlarge the collecting area").colored(PonderPalette.GREEN).pointAt(gapVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().restoreBlocks(thirdRowSelection);
        scene.world().showSection(thirdRowSelection, Direction.DOWN);

        scene.idle(20);
        setPower(scene, emitterPos, 10);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, arrayObject, threeRowsArea, 33);

        scene.idle(53);
        scene.overlay().showText(60).text("Expand the panels where sunlight is plentiful and unobstructed to supply more light to devices").colored(PonderPalette.GREEN).pointAt(arrayVec).placeNearTarget().attachKeyFrame();
        scene.world().restoreBlocks(northExpansionSelection);
        scene.world().showSection(northExpansionSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().restoreBlocks(eastExpansionSelection);
        scene.world().showSection(eastExpansionSelection, Direction.DOWN);

        scene.idle(20);
        setPower(scene, emitterPos, 32);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, arrayObject, arrayStartArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, arrayObject, fullArrayArea, 54);

        scene.idle(54);
        scene.overlay().showText(60).text("If the connected array grows beyond 5 blocks in length or width, all of it stops supplying light").colored(PonderPalette.RED).pointAt(excessPanelVec).placeNearTarget().attachKeyFrame();
        scene.world().restoreBlocks(excessPanelSelection);
        scene.world().showSection(excessPanelSelection, Direction.DOWN);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, excessObject, excessPanelArea, 70);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, arrayObject, fullArrayArea, 70);

        scene.idle(20);
        setPower(scene, emitterPos, 0);

        scene.idle(50);
        scene.world().hideSection(excessPanelSelection, Direction.UP);

        scene.idle(20);
        scene.world().setBlocks(excessPanelSelection, Blocks.AIR.defaultBlockState(), false);
        setPower(scene, emitterPos, 32);
        scene.markAsFinished();
    }

    public static void lighting(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("amethyst_collector_panel_lighting", "Sunlight and Collector Output");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9F);
        scene.showBasePlate();

        BlockPos collectorPos = util.grid().at(0, 1, 1);
        BlockPos partialArrayEndPos = collectorPos.east().south(4);
        BlockPos arrayEndPos = collectorPos.east(4).south(4);
        BlockPos excessPanelPos = collectorPos.east(5);
        BlockPos fiberPos = collectorPos.north();
        BlockPos emitterPos = collectorPos.east(3).north();
        BlockPos lastFiberPos = emitterPos.west();
        BlockPos targetPos = collectorPos.east(6).north();
        BlockPos roofStartPos = collectorPos.above(2);
        BlockPos partialRoofEndPos = collectorPos.east().south(4).above(2);
        BlockPos remainingRoofStartPos = collectorPos.east(2).above(2);
        BlockPos roofEndPos = collectorPos.east(4).south(4).above(2);
        BlockPos arrayCenterPos = collectorPos.east(2).south(2);

        Selection arraySelection = util.select().fromTo(collectorPos, arrayEndPos);
        Selection excessPanelSelection = util.select().position(excessPanelPos);
        Selection fiberSelection = util.select().fromTo(fiberPos, lastFiberPos);
        Selection supplySelection = util.select().fromTo(fiberPos, emitterPos).add(util.select().position(targetPos));
        Selection partialRoofSelection = util.select().fromTo(roofStartPos, partialRoofEndPos);
        Selection remainingRoofSelection = util.select().fromTo(remainingRoofStartPos, roofEndPos);
        Selection roofSelection = util.select().fromTo(roofStartPos, roofEndPos);

        Vec3 arrayVec = util.vector().topOf(arrayCenterPos);
        Vec3 roofVec = util.vector().topOf(remainingRoofStartPos);

        AABB arrayStartArea = new AABB(collectorPos);
        AABB partialArrayArea = new AABB(collectorPos).minmax(new AABB(partialArrayEndPos));
        AABB arrayArea = new AABB(collectorPos).minmax(new AABB(arrayEndPos));
        AABB roofStartArea = new AABB(roofStartPos);
        AABB roofArea = new AABB(roofStartPos).minmax(new AABB(roofEndPos));

        Object arrayObject = new Object();
        Object roofObject = new Object();

        scene.world().setBlocks(excessPanelSelection, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlocks(roofSelection, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlocks(fiberSelection, CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState(), false);

        scene.idle(20);
        scene.world().showSection(arraySelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(supplySelection, Direction.DOWN);

        scene.idle(20);
        scene.world().restoreBlocks(fiberSelection);
        setPower(scene, emitterPos, 32);
        scene.overlay().showText(60).text("Covering the panels from above reduces the light they collect; covering them completely stops the supply").colored(PonderPalette.RED).pointAt(arrayVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().restoreBlocks(partialRoofSelection);
        scene.world().showSection(partialRoofSelection, Direction.DOWN);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, arrayObject, arrayStartArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, arrayObject, partialArrayArea, 27);

        scene.idle(17);
        setPower(scene, emitterPos, 19.2);

        scene.idle(10);
        scene.world().restoreBlocks(remainingRoofSelection);
        scene.world().showSection(remainingRoofSelection, Direction.DOWN);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, arrayObject, arrayArea, 40);

        scene.idle(20);
        setPower(scene, emitterPos, 0);

        scene.idle(20);
        scene.world().hideSection(roofSelection, Direction.UP);

        scene.idle(20);
        scene.world().setBlocks(roofSelection, Blocks.AIR.defaultBlockState(), false);
        setPower(scene, emitterPos, 32);

        scene.idle(3);
        scene.overlay().showText(60).text("Stacking panels above an array blocks sunlight from reaching the panels below").colored(PonderPalette.RED).pointAt(roofVec).placeNearTarget().attachKeyFrame();
        scene.world().setBlocks(roofSelection, CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK.getDefaultState(), false);
        scene.world().showSection(roofSelection, Direction.DOWN);

        scene.idle(23);
        setPower(scene, emitterPos, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, roofObject, roofStartArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, arrayObject, arrayStartArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, roofObject, roofArea, 34);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, arrayObject, arrayArea, 34);

        scene.idle(54);
        scene.world().hideSection(roofSelection, Direction.UP);

        scene.idle(20);
        scene.world().setBlocks(roofSelection, Blocks.AIR.defaultBlockState(), false);
        setPower(scene, emitterPos, 32);

        scene.idle(20);
        scene.overlay().showText(60).text("Panels cannot work in weak sunlight or in dimensions without skylight").colored(PonderPalette.RED).pointAt(arrayVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("With default settings, rain reduces the light supplied by the panels").colored(PonderPalette.BLUE).pointAt(arrayVec).placeNearTarget().attachKeyFrame();

        scene.idle(60);
        scene.markAsFinished();
    }

    private static void setPower(CreateSceneBuilder scene, BlockPos emitterPos, double fluxLm) {
        scene.world().modifyBlockEntity(emitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(fluxLm));
    }
}
