package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.common.Tags.Items;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
public final class AmethystMirrorControls {
    private static final double NORMAL_DEGREES_PER_PIXEL = 1;
    private static final double FINE_DEGREES_PER_PIXEL = 0.1;
    private static final double ROTATION_ALIGNMENT_THRESHOLD = 0.9999999;
    private static final int ROTATION_CONFIRMATION_TIMEOUT_TICKS = 20;
    private static final int MIRROR_SCAN_INTERVAL_TICKS = 4;
    private static final AmethystMirrorControlTargets TARGETS = new AmethystMirrorControlTargets();
    private static final List<AmethystMirrorBlockEntity> MIRRORS = TARGETS.getMirrors();
    private static final List<PendingRotation> PENDING_ROTATIONS = new ArrayList<>();
    private static @Nullable Drag drag;
    private static @Nullable PickCache pickCache;
    private static int scanTicks;

    private AmethystMirrorControls() {
    }

    public static boolean dragMouse(double deltaX, double deltaY) {
        Drag current = drag;
        if (current == null) {
            return false;
        }

        pickCache = null;
        Minecraft minecraft = Minecraft.getInstance();
        boolean fineAdjustment = minecraft.options.keyShift.isDown();
        if (fineAdjustment != current.fineAdjustment) {
            current.fineAdjustment = fineAdjustment;
            current.snapOriginSteps = current.steps;
            current.angle = current.steps / (double) AmethystMirrorRotatePacket.STEPS_PER_DEGREE;
        }
        double degreesPerPixel = NORMAL_DEGREES_PER_PIXEL;
        int snapSteps = AmethystMirrorRotatePacket.STEPS_PER_DEGREE;
        if (fineAdjustment) {
            degreesPerPixel = FINE_DEGREES_PER_PIXEL;
            snapSteps = 1;
        }
        AmethystMirrorBlockEntity mirror = current.selection.mirror();
        boolean horizontalAxis = current.selection.horizontalAxis();
        Vec3 direction = current.screenDirection;
        double movement = deltaX * direction.x;
        if (horizontalAxis) {
            movement = deltaY * direction.y;
        }
        double sensitivity = minecraft.options.sensitivity().get() * 0.6 + 0.2;
        double sensitivityScale = sensitivity * sensitivity * sensitivity * 8;
        current.angle += movement * sensitivityScale * degreesPerPixel;
        current.steps = current.snapOriginSteps + (int) Math.round((current.angle * AmethystMirrorRotatePacket.STEPS_PER_DEGREE - current.snapOriginSteps) / snapSteps) * snapSteps;
        Quaternionf preview = new Quaternionf(current.initial);
        rotatePreview(preview, horizontalAxis, current.steps);
        current.preview = preview;
        mirror.setPreviewRotation(current.preview);
        return true;
    }

    public static boolean hasDisplayPreview(AmethystMirrorBlockEntity mirror) {
        if (drag != null && drag.selection.mirror() == mirror || AmethystMirrorRotationTracker.hasPending(mirror)) {
            return true;
        }

        for (PendingRotation pending : PENDING_ROTATIONS) {
            if (pending.mirror == mirror) {
                return true;
            }
        }
        return false;
    }

    public static @Nullable HitResult resolveGoggleTarget(@Nullable HitResult original) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (level == null || player == null || minecraft.screen != null || !minecraft.isWindowActive() || player.isSpectator() || !player.mayBuild() || !hasWrench(player)) {
            return original;
        }

        Drag current = drag;
        Selection selection = current == null ? findSelection(minecraft, true) : current.selection;
        if (selection == null) {
            return original;
        }

        AmethystMirrorBlockEntity mirror = selection.mirror();
        BlockPos pos = mirror.getBlockPos();
        if (mirror.isRemoved() || mirror.getLevel() != level || !AmethystMirrorControlPose.canInteract(player, pos)) {
            return original;
        }

        return new BlockHitResult(SubLevelBridge.resolve(level, pos).worldPosition(), Direction.UP, pos, false);
    }

    static void beginAim(AmethystMirrorBlockEntity mirror) {
        pickCache = null;
        AmethystMirrorRotationTracker.forget(mirror);
        removePending(mirror);
        mirror.setPreviewRotation(null);
    }

    static void previewAim(AmethystMirrorBlockEntity mirror, Quaternionf rotation) {
        pickCache = null;
        removePending(mirror);
        mirror.setPreviewRotation(rotation);
        PENDING_ROTATIONS.add(new PendingRotation(mirror, rotation));
    }

    static void rotatePreview(Quaternionf rotation, boolean horizontalAxis, int steps) {
        float radians = (float) Math.toRadians(steps % AmethystMirrorRotatePacket.FULL_TURN_STEPS / (double) AmethystMirrorRotatePacket.STEPS_PER_DEGREE);
        if (horizontalAxis) {
            rotation.rotateX(radians);
        }
        else {
            rotation.rotateY(radians);
        }
        rotation.normalize();
    }

    static void reconcileRotation(AmethystMirrorBlockEntity mirror, Quaternionf predicted) {
        pickCache = null;
        Drag current = drag;
        if (current == null || current.selection.mirror() != mirror) {
            previewAim(mirror, predicted);
            return;
        }

        removePending(mirror);
        boolean horizontalAxis = current.selection.horizontalAxis();
        current.initial.set(predicted);
        rotatePreview(current.initial, horizontalAxis, -current.sentSteps);
        current.preview = new Quaternionf(predicted);
        rotatePreview(current.preview, horizontalAxis, current.steps - current.sentSteps);
        mirror.setPreviewRotation(current.preview);
    }

    static void cancelRotation(AmethystMirrorBlockEntity mirror) {
        pickCache = null;
        if (drag != null && drag.selection.mirror() == mirror) {
            drag = null;
        }
        removePending(mirror);
        mirror.setPreviewRotation(null);
    }

    @SubscribeEvent
    private static void onFrame(RenderFrameEvent.Pre event) {
        pickCache = null;
    }

    @SubscribeEvent
    private static void onTick(Post event) {
        pickCache = null;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || minecraft.screen != null || !minecraft.isWindowActive() || player.isSpectator() || !player.mayBuild() || !hasWrench(player)) {
            clear();
            return;
        }

        AmethystMirrorRotationTracker.tick(level, player);
        tickPendingRotations(level, player);
        AmethystMirrorAimControls.tick(level, player);
        Drag current = drag;
        if (current != null) {
            AmethystMirrorBlockEntity mirror = current.selection.mirror();
            if (mirror.isRemoved() || mirror.getLevel() != level || !AmethystMirrorControlPose.canInteract(player, mirror.getBlockPos())) {
                clear();
                return;
            }

            flush(current);
            return;
        }

        if (scanTicks-- > 0) {
            return;
        }

        scanTicks = MIRROR_SCAN_INTERVAL_TICKS;
        TARGETS.collect(level, player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private static void onMouse(Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        Drag current = drag;
        if (current != null) {
            event.setCanceled(true);
            if (event.getAction() != GLFW.GLFW_RELEASE) {
                return;
            }

            flush(current);
            PENDING_ROTATIONS.add(new PendingRotation(current.selection.mirror(), current.preview));
            drag = null;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (event.getAction() != GLFW.GLFW_PRESS || minecraft.screen != null || player == null || !hasWrench(player) || player.isSpectator() || !player.mayBuild()) {
            return;
        }

        if (AmethystMirrorAimControls.isSelecting()) {
            AmethystMirrorAimControls.click(minecraft, player);
            event.setCanceled(true);
            return;
        }

        Selection selection = findSelection(minecraft, false);
        if (selection == null) {
            if (AmethystMirrorAimControls.click(minecraft, player)) {
                event.setCanceled(true);
            }

            return;
        }

        beginDrag(selection);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private static void onInteraction(InteractionKeyMappingTriggered event) {
        if (drag == null && !AmethystMirrorAimControls.isSelecting()) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent
    private static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (level == null || player == null || minecraft.screen != null || !minecraft.isWindowActive() || !hasWrench(player) || player.isSpectator() || !player.mayBuild()) {
            AmethystMirrorOutlineRenderer.clear();
            AmethystMirrorAimOutlineRenderer.clear();
            return;
        }

        Map<AmethystMirrorBlockEntity, @Nullable AmethystMirrorControlPose> poses = new IdentityHashMap<>();
        Selection hovered = findSelection(minecraft, true, poses);
        Drag current = drag;
        Selection active = current == null ? hovered : current.selection;
        AmethystMirrorOutlineRenderer.render(level, player, event.getFrustum(), MIRRORS, hovered, active, poses);
        AmethystMirrorAimControls.render();
    }

    private static boolean hasWrench(LocalPlayer player) {
        return player.getMainHandItem().is(Items.TOOLS_WRENCH) || player.getOffhandItem().is(Items.TOOLS_WRENCH);
    }

    private static void beginDrag(Selection selection) {
        pickCache = null;
        AmethystMirrorBlockEntity mirror = selection.mirror();
        Quaternionf initial = mirror.getRotation();
        removePending(mirror);
        Vec3 localHandle = AmethystMirrorControlGeometry.handleBounds(selection.handle()).getCenter();
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Level level = mirror.getLevel();
        BlockPos pos = mirror.getBlockPos();
        AmethystMirrorControlPose pose = new AmethystMirrorControlPose(Vec3.atCenterOf(pos), new Matrix3d().rotation(initial));
        if (level != null) {
            pose = AmethystMirrorControlPose.create(level, pos, initial, AnimationTickHolder.getPartialTicks(level));
        }
        Vec3 cameraToCenter = pose.center().subtract(camera.getPosition());
        Vec3 direction = AmethystMirrorDragGeometry.projectDirection(pose.matrix(), localHandle, selection.horizontalAxis(), cameraToCenter, camera.rotation());
        drag = new Drag(selection, initial, direction);
    }

    private static void removePending(AmethystMirrorBlockEntity mirror) {
        Iterator<PendingRotation> iterator = PENDING_ROTATIONS.iterator();
        while (iterator.hasNext()) {
            PendingRotation pending = iterator.next();
            if (pending.mirror != mirror) {
                continue;
            }

            iterator.remove();
        }
    }

    private static void clear() {
        pickCache = null;
        AmethystMirrorRotationTracker.clear();
        AmethystMirrorOutlineRenderer.clear();
        AmethystMirrorAimControls.clear();
        if (drag != null) {
            drag.selection.mirror().setPreviewRotation(null);
        }
        drag = null;
        for (PendingRotation pending : PENDING_ROTATIONS) {
            pending.mirror.setPreviewRotation(null);
        }
        PENDING_ROTATIONS.clear();
        TARGETS.clear();
        scanTicks = 0;
    }

    private static void flush(Drag current) {
        int difference = Math.floorMod(current.steps - current.sentSteps, AmethystMirrorRotatePacket.FULL_TURN_STEPS);
        if (difference >= AmethystMirrorRotatePacket.FULL_TURN_STEPS / 2) {
            difference -= AmethystMirrorRotatePacket.FULL_TURN_STEPS;
        }
        if (difference == 0) {
            return;
        }

        AmethystMirrorBlockEntity mirror = current.selection.mirror();
        Level level = mirror.getLevel();
        if (level == null) {
            return;
        }

        CatnipServices.NETWORK.sendToServer(AmethystMirrorRotationTracker.submit(mirror, level.dimension().location(), current.selection.horizontalAxis(), difference));
        current.sentSteps = current.steps;
    }

    private static void tickPendingRotations(ClientLevel level, LocalPlayer player) {
        Iterator<PendingRotation> iterator = PENDING_ROTATIONS.iterator();
        while (iterator.hasNext()) {
            PendingRotation pending = iterator.next();
            AmethystMirrorBlockEntity mirror = pending.mirror;
            boolean invalid = mirror.isRemoved() || mirror.getLevel() != level || !AmethystMirrorControlPose.canInteract(player, mirror.getBlockPos());
            if (!invalid && AmethystMirrorRotationTracker.hasPending(mirror)) {
                continue;
            }

            Quaternionf stored = mirror.getStoredRotation();
            Quaternionf preview = pending.preview;
            double dot = (double) stored.x * preview.x + (double) stored.y * preview.y + (double) stored.z * preview.z + (double) stored.w * preview.w;
            double storedLengthSquared = (double) stored.x * stored.x + (double) stored.y * stored.y + (double) stored.z * stored.z + (double) stored.w * stored.w;
            double previewLengthSquared = (double) preview.x * preview.x + (double) preview.y * preview.y + (double) preview.z * preview.z + (double) preview.w * preview.w;
            boolean confirmed = dot * dot > ROTATION_ALIGNMENT_THRESHOLD * ROTATION_ALIGNMENT_THRESHOLD * storedLengthSquared * previewLengthSquared;
            if (!invalid && !confirmed && ++pending.waitTicks <= ROTATION_CONFIRMATION_TIMEOUT_TICKS) {
                continue;
            }

            mirror.setPreviewRotation(null);
            iterator.remove();
        }
    }

    private static @Nullable Selection findSelection(Minecraft minecraft, boolean includeRings) {
        return findSelection(minecraft, includeRings, null);
    }

    private static @Nullable Selection findSelection(Minecraft minecraft, boolean includeRings, @Nullable Map<AmethystMirrorBlockEntity, @Nullable AmethystMirrorControlPose> poses) {
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || MIRRORS.isEmpty()) {
            return null;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 start = camera.getPosition();
        Vec3 end = start.add(new Vec3(camera.getLookVector()).scale(player.blockInteractionRange() + 1));
        HitResult crosshair = minecraft.hitResult;
        Vec3 eye = player.getEyePosition();
        PickCache cached = pickCache;
        if (includeRings && cached != null && cached.level() == level && cached.player() == player && cached.start().equals(start) && cached.end().equals(end) && cached.eye().equals(eye) && cached.crosshair() == crosshair) {
            Selection selection = cached.selection();
            if (selection == null || !selection.mirror().isRemoved()) {
                return selection;
            }
        }
        float partialTick = AnimationTickHolder.getPartialTicks(level);
        BlockHitResult obstruction = SubLevelBridge.clipAtRenderPose(level, new ClipContext(start, end, Block.OUTLINE, Fluid.NONE, player), partialTick);
        double nearest = start.distanceToSqr(end);
        if (obstruction.getType() != Type.MISS) {
            Vec3 anchor = Vec3.atCenterOf(obstruction.getBlockPos());
            Vec3 hit = SubLevelBridge.createRenderTransform(level, anchor, partialTick).transformPosition(obstruction.getLocation());
            nearest = start.distanceToSqr(hit);
        }
        if (crosshair != null && crosshair.getType() == Type.ENTITY) {
            nearest = Math.min(nearest, start.distanceToSqr(crosshair.getLocation()));
        }

        Selection result = null;
        for (AmethystMirrorBlockEntity mirror : MIRRORS) {
            if (mirror.isRemoved() || mirror.getLevel() != level) {
                continue;
            }

            BlockPos pos = mirror.getBlockPos();
            if (!AmethystMirrorControlPose.canInteract(player, pos)) {
                if (poses != null) {
                    poses.put(mirror, null);
                }
                continue;
            }

            AmethystMirrorControlPose pose = AmethystMirrorControlPose.create(level, pos, mirror.getRotation(), partialTick);
            if (poses != null) {
                poses.put(mirror, pose);
            }
            Vec3 localStart = pose.toLocal(start);
            Vec3 localEnd = pose.toLocal(end);
            AABB pickBounds = AmethystMirrorControlGeometry.PICK_BOUNDS;
            if (!pickBounds.contains(localStart) && pickBounds.clip(localStart, localEnd).isEmpty()) {
                continue;
            }

            for (int handle = 0; handle < AmethystMirrorControlGeometry.HANDLE_COUNT; handle++) {
                Optional<Vec3> hit = AmethystMirrorControlGeometry.handleBounds(handle).clip(localStart, localEnd);
                if (hit.isEmpty()) {
                    continue;
                }

                double distance = start.distanceToSqr(pose.toWorld(hit.get()));
                if (distance > nearest) {
                    continue;
                }

                nearest = distance;
                result = new Selection(mirror, handle);
            }
            if (!includeRings) {
                continue;
            }

            for (int ring = AmethystMirrorControlGeometry.HORIZONTAL_RING; ring <= AmethystMirrorControlGeometry.VERTICAL_RING; ring++) {
                List<AABB> bounds = AmethystMirrorControlGeometry.ringBounds(ring);
                for (AABB segment : bounds) {
                    Optional<Vec3> hit = segment.clip(localStart, localEnd);
                    if (hit.isEmpty()) {
                        continue;
                    }

                    double distance = start.distanceToSqr(pose.toWorld(hit.get()));
                    if (distance >= nearest) {
                        continue;
                    }

                    nearest = distance;
                    result = new Selection(mirror, ring);
                }
            }
        }
        if (includeRings) {
            pickCache = new PickCache(level, player, start, end, eye, crosshair, result);
        }
        return result;
    }

    private record PickCache(ClientLevel level, LocalPlayer player, Vec3 start, Vec3 end, Vec3 eye, @Nullable HitResult crosshair, @Nullable Selection selection) {
    }

    record Selection(AmethystMirrorBlockEntity mirror, int handle) {
        boolean horizontalAxis() {
            return handle >= AmethystMirrorControlGeometry.HANDLES_PER_AXIS && handle <= AmethystMirrorControlGeometry.HORIZONTAL_RING;
        }
    }

    private static final class Drag {
        private final Selection selection;
        private final Quaternionf initial;
        private final Vec3 screenDirection;
        private Quaternionf preview;
        private double angle;
        private int steps;
        private int sentSteps;
        private int snapOriginSteps;
        private boolean fineAdjustment;

        private Drag(Selection selection, Quaternionf initial, Vec3 screenDirection) {
            this.selection = selection;
            this.initial = initial;
            preview = initial;
            this.screenDirection = screenDirection;
        }
    }

    private static final class PendingRotation {
        private final AmethystMirrorBlockEntity mirror;
        private final Quaternionf preview;
        private int waitTicks;

        private PendingRotation(AmethystMirrorBlockEntity mirror, Quaternionf preview) {
            this.mirror = mirror;
            this.preview = new Quaternionf(preview);
        }
    }
}
