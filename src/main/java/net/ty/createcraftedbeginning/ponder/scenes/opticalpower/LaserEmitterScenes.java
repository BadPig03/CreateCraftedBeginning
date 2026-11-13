package net.ty.createcraftedbeginning.ponder.scenes.opticalpower;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterRangeBehaviour;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalchamber.PhotothermalChamberBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalchamber.PhotothermalChamberPort;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalchamber.PhotothermalChamberSerialization;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserEmitterScenes {
    private LaserEmitterScenes() {
    }

    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("laser_emitter", "Emitting and Controlling Lasers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos emitterPos = util.grid().at(1, 1, 1);
        BlockPos inputPos = emitterPos.west();
        BlockPos lineStartPos = emitterPos.west().south();
        BlockPos lineEndPos = emitterPos.west().south(3);
        BlockPos collectorStartPos = emitterPos.south(2);
        BlockPos collectorEndPos = emitterPos.east(2).south(4);
        BlockPos receiverPos = emitterPos.east(5);
        BlockPos blockerPos = emitterPos.east(3);

        Selection emitterSelection = util.select().position(emitterPos);
        Selection inputSelection = util.select().position(inputPos);
        Selection lineSelection = util.select().fromTo(lineStartPos, lineEndPos);
        Selection fiberSelection = lineSelection.copy().add(inputSelection);
        Selection collectorSelection = util.select().fromTo(collectorStartPos, collectorEndPos);
        Selection machineSelection = emitterSelection.copy().add(util.select().position(receiverPos));
        Selection blockerSelection = util.select().position(blockerPos);

        Vec3 emitterVec = util.vector().topOf(emitterPos);
        Vec3 inputVec = util.vector().blockSurface(emitterPos, Direction.WEST);
        Vec3 outputVec = util.vector().blockSurface(emitterPos, Direction.EAST);
        Vec3 panelVec = util.vector().blockSurface(emitterPos, Direction.NORTH).add(-0.125, 0, 0);
        Vec3 receiverVec = util.vector().blockSurface(receiverPos, Direction.WEST);
        Vec3 blockerVec = util.vector().topOf(blockerPos);

        AABB inputArea = new AABB(inputVec, inputVec).inflate(0.0625, 0.25, 0.25);
        AABB outputArea = new AABB(outputVec, outputVec).inflate(0.0625, 0.25, 0.25);
        AABB panelArea = new AABB(panelVec, panelVec).inflate(0.1875, 0.1875, 0.03125);
        AABB blockerArea = new AABB(blockerPos);
        AABB emitterArea = new AABB(emitterPos);

        Object inputObject = new Object();
        Object outputObject = new Object();
        Object panelObject = new Object();
        Object blockerObject = new Object();
        Object emitterObject = new Object();

        scene.world().setBlocks(blockerSelection, Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlocks(fiberSelection, CCBBlocks.OPTICAL_FIBER_BLOCK.getDefaultState(), false);

        scene.idle(20);
        scene.world().showSection(collectorSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(lineSelection, Direction.EAST);
        scene.world().showSection(inputSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(machineSelection, Direction.DOWN);

        scene.idle(20);
        scene.world().restoreBlocks(fiberSelection);
        scene.overlay().showText(60).text("Connect Optical Fibers to the back of a Laser Emitter to emit a laser from the front").colored(PonderPalette.GREEN).pointAt(inputVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, inputObject, inputArea, 60);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, outputObject, outputArea, 60);
        scene.world().modifyBlockEntity(emitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(10));
        setReceiverPower(scene, receiverPos, 10);

        scene.idle(80);
        scene.overlay().showText(60).text("Use the side panel to adjust the laser's range").colored(PonderPalette.BLUE).pointAt(panelVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, panelObject, panelArea, 60);
        scene.overlay().showControls(panelVec, Pointing.DOWN, 60).scroll();

        scene.idle(20);
        scene.world().modifyBlockEntityNBT(emitterSelection, LaserEmitterBlockEntity.class, tag -> tag.putInt(LaserEmitterRangeBehaviour.COMPOUND_KEY_LASER_RANGE, 2));
        setReceiverPower(scene, receiverPos, 0);

        scene.idle(60);
        scene.overlay().showText(60).text("Increase the range to reach more distant devices without changing the beam's strength").colored(PonderPalette.GREEN).placeNearTarget().pointAt(receiverVec).attachKeyFrame();
        scene.overlay().showControls(panelVec, Pointing.DOWN, 20).scroll();

        scene.idle(20);
        scene.world().modifyBlockEntityNBT(emitterSelection, LaserEmitterBlockEntity.class, tag -> tag.putInt(LaserEmitterRangeBehaviour.COMPOUND_KEY_LASER_RANGE, 7));
        setReceiverPower(scene, receiverPos, 10);

        scene.idle(60);
        scene.overlay().showText(60).text("If any part of the beam hits an obstacle, the whole beam stops there").colored(PonderPalette.RED).pointAt(blockerVec).placeNearTarget().attachKeyFrame();

        scene.idle(7);
        scene.world().restoreBlocks(blockerSelection);
        setReceiverPower(scene, receiverPos, 0);
        scene.world().showSection(blockerSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, blockerObject, blockerArea, 30);

        scene.idle(30);
        scene.world().hideSection(blockerSelection, Direction.UP);

        scene.idle(20);
        scene.world().setBlocks(blockerSelection, Blocks.AIR.defaultBlockState(), false);
        setReceiverPower(scene, receiverPos, 10);

        scene.idle(3);
        scene.overlay().showText(60).text("Once the emitter reaches its input limit, adding more light sources will not strengthen the beam").colored(PonderPalette.BLUE).pointAt(emitterVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, emitterObject, emitterArea, 60);

        scene.idle(60);
        scene.markAsFinished();
    }

    private static void setReceiverPower(CreateSceneBuilder scene, BlockPos receiverPos, double fluxLm) {
        scene.world().modifyBlockEntity(receiverPos, PhotothermalChamberBlockEntity.class, receiver -> {
            int ports = 0;
            if (fluxLm > 0) {
                ports = 1 << PhotothermalChamberPort.WEST.ordinal();
            }

            CompoundTag compoundTag = new CompoundTag();
            PhotothermalChamberSerialization.writeIllumination(compoundTag, ports, fluxLm, fluxLm, fluxLm);
            receiver.handleUpdateTag(compoundTag, scene.world().getHolderLookupProvider());
        });
    }
}
