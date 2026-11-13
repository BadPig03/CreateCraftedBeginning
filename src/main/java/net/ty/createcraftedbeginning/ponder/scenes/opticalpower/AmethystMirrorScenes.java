package net.ty.createcraftedbeginning.ponder.scenes.opticalpower;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorReflectionGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorSerialization;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterRangeBehaviour;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystMirrorScenes {
    private AmethystMirrorScenes() {
    }

    public static void reflecting(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("amethyst_mirror_reflecting", "Adjusting Reflected Lasers");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos mirrorPos = util.grid().at(3, 1, 2);
        BlockPos emitterPos = mirrorPos.west(3);
        BlockPos reverseEmitterPos = mirrorPos.east(3);
        BlockPos receiverPos = mirrorPos.south(4);
        BlockPos secondMirrorPos = mirrorPos.south(2);
        BlockPos finalReceiverPos = mirrorPos.east(3).south(2);

        Selection mirrorSelection = util.select().position(mirrorPos);
        Selection emitterSelection = util.select().position(emitterPos);
        Selection firstSelection = util.select().fromTo(mirrorPos.west(3).north(2), mirrorPos.east(3)).add(util.select().position(receiverPos));
        Selection secondSelection = util.select().position(secondMirrorPos).add(util.select().position(finalReceiverPos));
        Selection receiverSelection = util.select().position(receiverPos);

        Vec3 mirrorVec = util.vector().centerOf(mirrorPos);
        Vec3 secondMirrorVec = util.vector().topOf(secondMirrorPos);
        Vec3 panelVec = util.vector().blockSurface(emitterPos, Direction.NORTH);
        Vec3 greenHandleVec = mirrorVec.add(AmethystMirrorGeometry.create(Direction.UP, 135, 0).horizontal().scale(0.5625));

        AABB mirrorArea = new AABB(mirrorPos);
        AABB secondMirrorArea = new AABB(secondMirrorPos);

        Object mirrorObject = new Object();
        Object secondMirrorObject = new Object();
        Object[] handles = {new Object(), new Object(), new Object(), new Object()};

        ItemStack wrench = new ItemStack(AllItems.WRENCH.get());

        setPose(scene, mirrorSelection, 135, 0);
        scene.world().setBlocks(secondSelection, Blocks.AIR.defaultBlockState(), false);

        scene.idle(20);
        scene.world().showSection(firstSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Both sides of an Amethyst Mirror can redirect a laser beam").colored(PonderPalette.GREEN).pointAt(mirrorVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, mirrorObject, mirrorArea, 60);
        scene.world().modifyBlockEntity(emitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(16));
        scene.world().modifyBlockEntity(reverseEmitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(16));

        scene.idle(60);
        scene.world().modifyBlockEntity(reverseEmitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(0));

        scene.idle(20);
        scene.overlay().showText(60).text("While holding a Wrench, right-click and drag one of the outer square handles to rotate the mirror").colored(PonderPalette.BLUE).pointAt(mirrorVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(mirrorVec, Pointing.RIGHT, 40).rightClick().withItem(wrench.copy());
        turnMirror(scene, mirrorSelection, mirrorVec, handles, 135, 145, 0, 0);
        turnMirror(scene, mirrorSelection, mirrorVec, handles, 145, 135, 0, 0);
        turnMirror(scene, mirrorSelection, mirrorVec, handles, 135, 135, 0, 10);
        turnMirror(scene, mirrorSelection, mirrorVec, handles, 135, 135, 10, 0);

        scene.idle(40);
        scene.overlay().showText(60).text("Drag while sneaking to make finer adjustments to the mirror's orientation").colored(PonderPalette.BLUE).pointAt(mirrorVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(greenHandleVec, Pointing.RIGHT, 60).rightClick().whileSneaking().withItem(wrench.copy());
        setPose(scene, mirrorSelection, 136, 0);
        turnMirror(scene, mirrorSelection, mirrorVec, handles, 136, 135, 0, 0);
        showHandles(scene, mirrorVec, handles, 135, 0, 50);

        scene.idle(70);
        scene.overlay().showText(60).text("Place another Amethyst Mirror in the beam's path to redirect it again").colored(PonderPalette.GREEN).pointAt(secondMirrorVec).placeNearTarget().attachKeyFrame();
        scene.world().hideSection(receiverSelection, Direction.UP);
        scene.world().restoreBlocks(secondSelection);
        scene.world().showSection(secondSelection, Direction.DOWN);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, secondMirrorObject, secondMirrorArea, 60);

        scene.idle(20);
        scene.world().setBlocks(receiverSelection, Blocks.AIR.defaultBlockState(), false);

        scene.idle(60);
        scene.overlay().showText(60).text("The total length of the reflected path cannot exceed the Laser Emitter's range").colored(PonderPalette.RED).pointAt(panelVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(panelVec, Pointing.DOWN, 40).scroll();

        scene.idle(20);
        scene.world().modifyBlockEntityNBT(emitterSelection, LaserEmitterBlockEntity.class, tag -> tag.putInt(LaserEmitterRangeBehaviour.COMPOUND_KEY_LASER_RANGE, 5));

        scene.idle(40);
        scene.markAsFinished();
    }

    public static void targeting(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("amethyst_mirror_targeting", "Aiming at a Target");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos mirrorPos = util.grid().at(3, 1, 1);
        BlockPos emitterPos = mirrorPos.west(3);
        BlockPos targetPos = mirrorPos.east(2).south(3);
        BlockPos movedTargetPos = mirrorPos.west().south(5);
        BlockPos blockerPos = mirrorPos.east().south(2);

        Selection mirrorSelection = util.select().position(mirrorPos);
        Selection machineSelection = util.select().fromTo(mirrorPos.west(3), mirrorPos).add(util.select().position(mirrorPos.south(4))).add(util.select().position(targetPos));
        Selection targetSelection = util.select().position(targetPos);
        Selection movedTargetSelection = util.select().position(movedTargetPos);
        Selection blockerSelection = util.select().position(blockerPos);

        Vec3 mirrorVec = util.vector().centerOf(mirrorPos);
        Vec3 inputVec = util.vector().blockSurface(emitterPos, Direction.EAST);
        Vec3 targetVec = util.vector().blockSurface(targetPos, Direction.WEST);
        Vec3 movedTargetVec = util.vector().blockSurface(movedTargetPos, Direction.NORTH, 0.015625F);
        Vec3 blockerVec = util.vector().topOf(blockerPos);
        Vec3 inputDirection = Vec3.atLowerCornerOf(Direction.EAST.getNormal());
        BeamFrame incoming = BeamFrame.fromDirection(inputVec, inputDirection);
        Aim firstAim = solveAim(mirrorVec, incoming, targetVec);
        Aim movedAim = solveAim(mirrorVec, incoming, movedTargetVec);

        AABB mirrorArea = new AABB(mirrorPos);
        AABB targetArea = new AABB(targetVec, targetVec).inflate(0.1);
        AABB movedTargetArea = new AABB(movedTargetVec, movedTargetVec).inflate(0.1);
        AABB blockerArea = new AABB(blockerPos);
        Object mirrorObject = new Object();
        Object targetObject = new Object();
        Object blockerObject = new Object();
        ItemStack wrench = new ItemStack(AllItems.WRENCH.get());
        BlockState airState = Blocks.AIR.defaultBlockState();

        setPose(scene, mirrorSelection, 135, 0);
        scene.world().setBlocks(blockerSelection, airState, false);
        scene.world().setBlocks(movedTargetSelection, airState, false);

        scene.idle(20);
        scene.world().showSection(machineSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().showText(60).text("Make sure the laser reaches the mirror from only one direction before selecting a target").colored(PonderPalette.BLUE).pointAt(mirrorVec).placeNearTarget().attachKeyFrame();
        scene.world().modifyBlockEntity(emitterPos, LaserEmitterBlockEntity.class, emitter -> emitter.setPonderFlux(16));
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, mirrorObject, mirrorArea, 60);

        scene.idle(80);
        scene.overlay().showText(60).text("Right-click the mirror's body with a Wrench to start selecting a target").colored(PonderPalette.BLUE).pointAt(mirrorVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(mirrorVec, Pointing.UP, 60).rightClick().withItem(wrench.copy());
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, mirrorObject, mirrorArea, 60);

        scene.idle(80);
        scene.overlay().showText(60).text("Select a point on a block surface within 32 blocks of the mirror").colored(PonderPalette.BLUE).pointAt(targetVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, targetObject, targetArea, 60);

        scene.idle(80);
        scene.overlay().showText(60).text("A green preview means the reflected laser can reach the target").colored(PonderPalette.BLUE).pointAt(targetVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, targetObject, targetArea, 80);
        previewPath(scene, inputVec, firstAim.contact(), targetVec, 80);

        scene.idle(80);
        scene.overlay().showText(60).text("Right-click to confirm, or right-click while sneaking to cancel").colored(PonderPalette.BLUE).pointAt(targetVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, targetObject, targetArea, 40);
        previewPath(scene, inputVec, firstAim.contact(), targetVec, 40);
        scene.overlay().showControls(targetVec, Pointing.UP, 40).rightClick().withItem(wrench.copy());

        scene.idle(40);
        setPose(scene, mirrorSelection, firstAim.angle(), 0);

        scene.idle(40);
        scene.overlay().showText(60).text("The mirror's frame or blocks along the path can stop the entire laser beam").colored(PonderPalette.RED).pointAt(blockerVec).placeNearTarget().attachKeyFrame();
        scene.world().restoreBlocks(blockerSelection);
        scene.world().showSection(blockerSelection, Direction.DOWN);

        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, blockerObject, blockerArea, 60);

        scene.idle(60);
        scene.overlay().showText(60).text("Targets that the reflected laser cannot reach are shown in red").colored(PonderPalette.RED).pointAt(targetVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, targetObject, targetArea, 60);

        scene.idle(40);
        scene.world().hideSection(blockerSelection, Direction.UP);

        scene.idle(10);
        scene.world().setBlocks(blockerSelection, airState, false);

        scene.idle(10);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, targetObject, targetArea, 40);
        previewPath(scene, inputVec, firstAim.contact(), targetVec, 40);

        scene.idle(60);
        scene.overlay().showText(60).text("The mirror does not automatically follow a target when it moves").colored(PonderPalette.BLUE).pointAt(movedTargetVec).placeNearTarget().attachKeyFrame();
        scene.world().hideSection(targetSelection, Direction.UP);
        scene.world().restoreBlocks(movedTargetSelection);
        scene.world().showSection(movedTargetSelection, Direction.NORTH);

        scene.idle(10);
        scene.world().setBlocks(targetSelection, airState, false);

        scene.idle(10);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.BLUE, targetObject, movedTargetArea, 60);

        scene.idle(60);
        scene.overlay().showText(60).text("Select a new point on the target to aim the reflected laser at it again").colored(PonderPalette.BLUE).pointAt(movedTargetVec).placeNearTarget().attachKeyFrame();
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, targetObject, movedTargetArea, 40);
        scene.overlay().showControls(movedTargetVec, Pointing.UP, 40).rightClick().withItem(wrench.copy());
        previewPath(scene, inputVec, movedAim.contact(), movedTargetVec, 40);

        scene.idle(40);
        setPose(scene, mirrorSelection, movedAim.angle(), 0);

        scene.idle(20);
        scene.markAsFinished();
    }

    private static void setPose(CreateSceneBuilder scene, Selection selection, float angle, float tilt) {
        scene.world().modifyBlockEntityNBT(selection, AmethystMirrorBlockEntity.class, compoundTag -> AmethystMirrorSerialization.writeRotation(Direction.UP, angle, tilt, compoundTag));
    }

    private static void turnMirror(CreateSceneBuilder scene, Selection selection, Vec3 center, Object[] handles, float fromAngle, float toAngle, float fromTilt, float toTilt) {
        for (int tick = 1; tick <= 10; tick++) {
            float progress = (float) tick / 10;
            float angle = fromAngle + (toAngle - fromAngle) * progress;
            float tilt = fromTilt + (toTilt - fromTilt) * progress;
            setPose(scene, selection, angle, tilt);
            showHandles(scene, center, handles, angle, tilt, 2);

            scene.idle(1);
        }
    }

    private static void showHandles(CreateSceneBuilder scene, Vec3 center, Object[] handles, float angle, float tilt, int duration) {
        AmethystMirrorGeometry geometry = AmethystMirrorGeometry.create(Direction.UP, angle, tilt);
        for (int index = 0; index < handles.length; index++) {
            Vec3 axis = geometry.horizontal();
            PonderPalette color = PonderPalette.GREEN;
            if (index >= 2) {
                axis = geometry.vertical();
                color = PonderPalette.OUTPUT;
            }
            double distance = 0.5625;
            if (index % 2 == 0) {
                distance = -distance;
            }
            Vec3 handle = center.add(axis.scale(distance));
            AABB area = new AABB(handle, handle).inflate(0.0625);
            scene.overlay().chaseBoundingBoxOutline(color, handles[index], area, duration);
        }
    }

    private static Aim solveAim(Vec3 center, BeamFrame incoming, Vec3 target) {
        Quaternionf rotation = AmethystMirrorReflectionGeometry.solve(AmethystMirrorGeometry.createRotation(Direction.UP, 135, 0), center, incoming, target);
        if (rotation == null) {
            throw new IllegalStateException("No reflected Ponder path from " + center + " to " + target + '.');
        }

        Vec3 normal = AmethystMirrorGeometry.create(rotation).normal();
        double distance = (AmethystMirrorGeometry.MIRROR_HALF_THICKNESS - normal.dot(incoming.origin().subtract(center))) / normal.dot(incoming.direction());
        Vec3 contact = incoming.origin().add(incoming.direction().scale(distance));
        return new Aim((float) Math.toDegrees(Math.atan2(-normal.x, -normal.z)), contact);
    }

    private static void previewPath(CreateSceneBuilder scene, Vec3 start, Vec3 contact, Vec3 target, int duration) {
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.62F, 0.87F, 0.45F), 1);
        scene.addInstruction(new TickingInstruction(false, duration) {
            @Override
            public void tick(PonderScene ponderScene) {
                super.tick(ponderScene);
                PonderLevel world = ponderScene.getWorld();
                double offset = (totalTicks - remainingTicks) / 3.0 % 2.5;
                for (int index = 0; index < 4; index++) {
                    double progress = (offset + index * 2.5) / 10;
                    Vec3 incoming = start.lerp(contact, progress);
                    Vec3 outgoing = contact.lerp(target, progress);
                    world.addParticle(dust, incoming.x, incoming.y, incoming.z, 0, 0, 0);
                    world.addParticle(dust, outgoing.x, outgoing.y, outgoing.z, 0, 0, 0);
                }
            }
        });
    }

    private record Aim(float angle, Vec3 contact) {
    }
}
