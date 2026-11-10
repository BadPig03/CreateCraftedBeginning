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

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpticalFiberScenes {
    private OpticalFiberScenes() {
    }

    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("optical_fiber", "Supplying Light with Optical Fibers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos collectorPos = util.grid().at(1, 1, 2);
        BlockPos firstArrayMinPos = collectorPos.west().north(2);
        BlockPos firstArrayMaxPos = collectorPos.east();
        BlockPos secondArrayMinPos = collectorPos.east(3).north(2);
        BlockPos secondArrayMaxPos = collectorPos.east(5);
        BlockPos secondCollectorPos = collectorPos.east(4);
        BlockPos firstFiberPos = collectorPos.south();
        BlockPos middleFiberPos = collectorPos.east().south();
        BlockPos junctionPos = collectorPos.east(2).south();
        BlockPos secondLinkPos = collectorPos.east(3).south();
        BlockPos secondJunctionPos = collectorPos.east(4).south();
        BlockPos inputPos = collectorPos.east(2).south(2);
        BlockPos branchPos = collectorPos.east(2).south(3);
        BlockPos secondInputPos = collectorPos.east(2).south(4);
        BlockPos emitterPos = collectorPos.east().south(2);
        BlockPos secondEmitterPos = collectorPos.east().south(4);
        BlockPos targetPos = collectorPos.west().south(2);
        BlockPos secondTargetPos = collectorPos.west().south(4);

        Selection firstArraySelection = util.select().fromTo(firstArrayMinPos, firstArrayMaxPos);
        Selection secondArraySelection = util.select().fromTo(secondArrayMinPos, secondArrayMaxPos);
        Selection initialLineSelection = util.select().fromTo(firstFiberPos, junctionPos);
        Selection secondLinkSelection = util.select().fromTo(secondLinkPos, secondJunctionPos);
        Selection inputSelection = util.select().position(inputPos);
        Selection secondInputSelection = util.select().fromTo(branchPos, secondInputPos);
        Selection machineSelection = util.select().position(emitterPos).add(util.select().position(targetPos));
        Selection secondMachineSelection = util.select().position(secondEmitterPos).add(util.select().position(secondTargetPos));
        Selection collectorSelection = util.select().position(collectorPos);
        Selection secondGlowstoneSelection = util.select().position(firstArrayMaxPos);
        Selection laterSelection = secondArraySelection.copy().add(secondLinkSelection).add(inputSelection).add(secondInputSelection).add(machineSelection).add(secondMachineSelection);
        Selection removedSelection = firstArraySelection.copy().add(secondArraySelection).add(secondLinkSelection).add(secondInputSelection).add(secondMachineSelection);

        Vec3 collectorVec = util.vector().blockSurface(collectorPos, Direction.SOUTH);
        Vec3 inputVec = util.vector().blockSurface(emitterPos, Direction.EAST);
        Vec3 secondArrayVec = util.vector().topOf(secondCollectorPos);
        Vec3 junctionVec = util.vector().centerOf(junctionPos);
        Vec3 glowstoneVec = util.vector().topOf(collectorPos);
        Vec3 secondGlowstoneVec = util.vector().topOf(firstArrayMaxPos);

        AABB collectorPortArea = new AABB(collectorVec, collectorVec).inflate(0.375, 0.25, 0.0625);
        AABB inputPortArea = new AABB(inputVec, inputVec).inflate(0.0625, 0.375, 0.375);
        AABB firstArrayStartArea = new AABB(collectorPos);
        AABB firstArrayArea = new AABB(firstArrayMinPos).minmax(new AABB(firstArrayMaxPos));
        AABB secondArrayStartArea = new AABB(secondCollectorPos);
        AABB secondArrayArea = new AABB(secondArrayMinPos).minmax(new AABB(secondArrayMaxPos));
        AABB emitterArea = new AABB(emitterPos);
        AABB secondEmitterArea = new AABB(secondEmitterPos);
        AABB glowstoneArea = new AABB(collectorPos);
        AABB combinedGlowstoneArea = glowstoneArea.expandTowards(1, 0, 0);

        Object collectorPortObject = new Object();
        Object inputPortObject = new Object();
        Object firstArrayObject = new Object();
        Object secondArrayObject = new Object();
        Object emitterObject = new Object();
        Object secondEmitterObject = new Object();
        Object glowstoneObject = new Object();

        scene.world().setBlocks(laterSelection, Blocks.AIR.defaultBlockState(), false);
        scene.world().modifyBlock(junctionPos, state -> state.setValue(OpticalFiberBlock.EAST, false).setValue(OpticalFiberBlock.SOUTH, false), false);

        scene.idle(20);
        scene.world().showSection(firstArraySelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(initialLineSelection, Direction.NORTH);

        scene.idle(20);
        scene.overlay().showText(60).text("Connect Optical Fibers to a horizontal side of an Amethyst Collector Panel to carry away the light it collects").colored(PonderPalette.INPUT).pointAt(collectorVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, collectorPortObject, collectorPortArea, 60);

        scene.idle(60);
        scene.world().restoreBlocks(machineSelection);
        scene.world().showSection(machineSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Run the fiber to the back of a Laser Emitter to supply it with light").colored(PonderPalette.GREEN).pointAt(inputVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputPortObject, inputPortArea, 60);

        scene.idle(60);
        scene.world().restoreBlocks(inputSelection);
        scene.world().modifyBlock(inputPos, state -> state.setValue(OpticalFiberBlock.SOUTH, false), false);
        scene.world().showSection(inputSelection, Direction.WEST);

        scene.idle(20);
        scene.world().modifyBlock(junctionPos, state -> state.setValue(OpticalFiberBlock.SOUTH, true), false);
        scene.world().modifyBlockEntity(emitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(10));

        scene.idle(33);
        scene.world().restoreBlocks(secondArraySelection);
        scene.world().showSection(secondArraySelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Connect another independent collector array to the same fiber network to combine the light from both arrays").colored(PonderPalette.GREEN).pointAt(secondArrayVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().restoreBlocks(secondLinkSelection);
        scene.world().showSection(secondLinkSelection, Direction.WEST);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, firstArrayObject, firstArrayStartArea, 3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, secondArrayObject, secondArrayStartArea, 3);

        scene.idle(3);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, firstArrayObject, firstArrayArea, 50);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, secondArrayObject, secondArrayArea, 50);

        scene.idle(17);
        scene.world().modifyBlock(junctionPos, state -> state.setValue(OpticalFiberBlock.EAST, true), false);
        scene.world().modifyBlockEntity(emitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(20));

        scene.idle(53);
        scene.world().restoreBlocks(secondMachineSelection);
        scene.world().showSection(secondMachineSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Add a second emitter and the two will share the network's light equally").pointAt(junctionVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, emitterObject, emitterArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, secondEmitterObject, secondEmitterArea, 60);

        scene.idle(7);
        scene.world().restoreBlocks(secondInputSelection);
        scene.world().showSection(secondInputSelection, Direction.WEST);

        scene.idle(20);
        scene.world().modifyBlock(inputPos, state -> state.setValue(OpticalFiberBlock.SOUTH, true), false);
        scene.world().modifyBlockEntity(emitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(10));
        scene.world().modifyBlockEntity(secondEmitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(10));

        scene.idle(53);
        scene.world().modifyBlockEntity(emitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(0));
        scene.world().modifyBlockEntity(secondEmitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(0));
        scene.world().modifyBlock(junctionPos, state -> state.setValue(OpticalFiberBlock.EAST, false), false);
        scene.world().modifyBlock(middleFiberPos, state -> state.setValue(OpticalFiberBlock.NORTH, false), false);
        scene.world().modifyBlock(inputPos, state -> state.setValue(OpticalFiberBlock.SOUTH, false), false);
        scene.world().hideSection(removedSelection, Direction.UP);

        scene.idle(20);
        scene.world().setBlocks(removedSelection, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlock(collectorPos, Blocks.GLOWSTONE.defaultBlockState(), false);
        scene.world().showSection(collectorSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().modifyBlockEntity(emitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(1));
        scene.overlay().showText(60).text("Full blocks as bright as Glowstone can also supply a little light").colored(PonderPalette.BLUE).pointAt(glowstoneVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, glowstoneObject, glowstoneArea, 60);

        scene.idle(80);
        scene.overlay().showText(60).text("Adding more of these sources to the same network does not increase the light supply").colored(PonderPalette.BLUE).pointAt(secondGlowstoneVec).placeNearTarget().attachKeyFrame();
        scene.world().setBlock(firstArrayMaxPos, Blocks.GLOWSTONE.defaultBlockState(), false);
        scene.world().modifyBlock(middleFiberPos, state -> state.setValue(OpticalFiberBlock.NORTH, true), false);
        scene.world().showSection(secondGlowstoneSelection, Direction.DOWN);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, glowstoneObject, glowstoneArea, 20);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, glowstoneObject, combinedGlowstoneArea, 40);

        scene.idle(40);
        scene.markAsFinished();
    }
}
