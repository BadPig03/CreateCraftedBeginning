package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorControls.Selection;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorOutlineRenderer {
    private static final float LINE_WIDTH = 0.015625F;
    private static final float SELECTED_LINE_WIDTH = 0.0234375F;
    private static final double CONTROL_BOUNDS_PADDING = 0.25;
    private static final int HORIZONTAL_COLOR = 0xEA5C2B;
    private static final int HORIZONTAL_SELECTED_COLOR = 0xFF8060;
    private static final int VERTICAL_COLOR = 0x95CD41;
    private static final int VERTICAL_SELECTED_COLOR = 0xBFFF80;
    private static final Map<BlockPos, List<AmethystMirrorControlOutline>> OUTLINES = new HashMap<>();
    private static @Nullable ClientLevel outlineLevel;

    private AmethystMirrorOutlineRenderer() {
    }

    static void render(ClientLevel level, LocalPlayer player, Frustum frustum, Iterable<AmethystMirrorBlockEntity> mirrors, @Nullable Selection hovered, @Nullable Selection active, Map<AmethystMirrorBlockEntity, @Nullable AmethystMirrorControlPose> poses) {
        if (outlineLevel != level) {
            clear();
            outlineLevel = level;
        }
        AmethystMirrorBlockEntity hoveredMirror = hovered == null ? null : hovered.mirror();
        int hoveredHandle = hovered == null ? -1 : hovered.handle();
        AmethystMirrorBlockEntity activeMirror = active == null ? null : active.mirror();
        int activeHandle = active == null ? -1 : active.handle();
        Outliner outliner = Outliner.getInstance();
        Set<BlockPos> retained = new HashSet<>();
        float partialTick = AnimationTickHolder.getPartialTicks(level);
        for (AmethystMirrorBlockEntity mirror : mirrors) {
            if (mirror.isRemoved() || mirror.getLevel() != level) {
                continue;
            }

            BlockPos pos = mirror.getBlockPos();
            AmethystMirrorControlPose pose = poses.get(mirror);
            if (pose == null) {
                if (poses.containsKey(mirror) || !AmethystMirrorControlPose.canInteract(player, pos)) {
                    continue;
                }

                pose = AmethystMirrorControlPose.create(level, pos, mirror.getRotation(), partialTick);
            }
            retained.add(pos);
            if (!frustum.isVisible(pose.bounds(new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5).inflate(CONTROL_BOUNDS_PADDING)))) {
                List<AmethystMirrorControlOutline> hidden = OUTLINES.get(pos);
                if (hidden == null) {
                    continue;
                }

                for (AmethystMirrorControlOutline outline : hidden) {
                    outliner.remove(outline);
                }
                continue;
            }

            List<AmethystMirrorControlOutline> outlines = OUTLINES.computeIfAbsent(pos, ignored -> {
                Vec3 center = Vec3.atCenterOf(pos);
                List<AmethystMirrorControlOutline> controls = new ArrayList<>();
                for (int handle = 0; handle < AmethystMirrorControlGeometry.HANDLE_COUNT; handle++) {
                    controls.add(new AmethystMirrorHandleOutline(center, AmethystMirrorControlGeometry.handleBounds(handle)));
                }
                double radius = AmethystMirrorControlGeometry.ringCosine();
                controls.add(new AmethystMirrorRingOutline(center, radius, true));
                controls.add(new AmethystMirrorRingOutline(center, radius, false));
                return List.copyOf(controls);
            });
            boolean selectedMirror = activeMirror == mirror;
            for (int control = 0; control < outlines.size(); control++) {
                boolean horizontalAxis = control >= AmethystMirrorControlGeometry.HANDLES_PER_AXIS && control <= AmethystMirrorControlGeometry.HORIZONTAL_RING;
                boolean selected = control < AmethystMirrorControlGeometry.HANDLE_COUNT && (hoveredMirror == mirror && hoveredHandle == control || selectedMirror && activeHandle == control);
                int color = VERTICAL_COLOR;
                if (horizontalAxis) {
                    color = selected ? HORIZONTAL_SELECTED_COLOR : HORIZONTAL_COLOR;
                }
                else if (selected) {
                    color = VERTICAL_SELECTED_COLOR;
                }
                AmethystMirrorControlOutline outline = outlines.get(control);
                outline.setPose(pose);
                outliner.showOutline(outline, outline).colored(color).lineWidth(selected ? SELECTED_LINE_WIDTH : LINE_WIDTH);
            }
        }
        Iterator<Entry<BlockPos, List<AmethystMirrorControlOutline>>> iterator = OUTLINES.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<BlockPos, List<AmethystMirrorControlOutline>> entry = iterator.next();
            if (retained.contains(entry.getKey())) {
                continue;
            }

            for (AmethystMirrorControlOutline outline : entry.getValue()) {
                outliner.remove(outline);
            }
            iterator.remove();
        }
    }

    static void clear() {
        Outliner outliner = Outliner.getInstance();
        for (List<AmethystMirrorControlOutline> outlines : OUTLINES.values()) {
            for (AmethystMirrorControlOutline outline : outlines) {
                outliner.remove(outline);
            }
        }
        OUTLINES.clear();
        outlineLevel = null;
    }
}
