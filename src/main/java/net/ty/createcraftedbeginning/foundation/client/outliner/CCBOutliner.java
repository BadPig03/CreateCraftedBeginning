package net.ty.createcraftedbeginning.foundation.client.outliner;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.foundation.client.outliner.CCBOutline.CCBOutlineParams;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public enum CCBOutliner {
    INSTANCE;

    private final Map<Object, CCBOutlineEntry> outlines = Collections.synchronizedMap(new HashMap<>());
    private final Map<Object, CCBOutlineEntry> outlinesView = Collections.unmodifiableMap(outlines);

    private static float getFadeAlpha(CCBOutlineEntry entry, float partialTicks) {
        int previousTicks = entry.ticksTillRemoval + 1;
        float previousAlpha = previousTicks >= 0 ? 1 : 1 + previousTicks / (float) CCBOutlineEntry.FADE_TICKS;
        float currentAlpha = 1 + entry.ticksTillRemoval / (float) CCBOutlineEntry.FADE_TICKS;
        float alpha = Mth.lerp(partialTicks, previousAlpha, currentAlpha);
        return Mth.square(alpha) * alpha;
    }

    private static void renderOutline(CCBOutlineEntry entry, PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 camera, float partialTicks) {
        CCBOutline outline = entry.getOutline();
        CCBOutlineParams params = outline.getParams();
        params.alpha = 1;
        if (entry.isFading()) {
            params.alpha = getFadeAlpha(entry, partialTicks);
            if (params.alpha < 0.125f) {
                return;
            }
        }

        outline.render(poseStack, buffer, camera, partialTicks);
    }

    public CCBOutlineParams showCluster(Object slot, Iterable<BlockPos> selection) {
        XRayBlockClusterOutline outline = new XRayBlockClusterOutline(selection);
        addOutline(slot, outline);
        return outline.getParams();
    }

    public void keep(Object slot) {
        CCBOutlineEntry entry = outlines.get(slot);
        if (entry == null) {
            return;
        }

        entry.ticksTillRemoval = 1;
    }

    public void remove(Object slot) {
        outlines.remove(slot);
    }

    public Map<Object, CCBOutlineEntry> getOutlines() {
        return outlinesView;
    }

    private void addOutline(Object slot, CCBOutline outline) {
        outlines.put(slot, new CCBOutlineEntry(outline));
    }

    public void tickOutlines() {
        Iterator<CCBOutlineEntry> iterator = outlines.values().iterator();
        while (iterator.hasNext()) {
            CCBOutlineEntry entry = iterator.next();
            entry.tick();
            if (entry.isAlive()) {
                continue;
            }

            iterator.remove();
        }
    }

    public void renderOutlines(PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 camera, float partialTicks) {
        outlines.forEach((ignoredSlot, entry) -> renderOutline(entry, poseStack, buffer, camera, partialTicks));
    }

    public static class CCBOutlineEntry {
        private static final int FADE_TICKS = 8;

        private final CCBOutline outline;
        private int ticksTillRemoval = 1;

        protected CCBOutlineEntry(CCBOutline outline) {
            this.outline = outline;
        }

        private CCBOutline getOutline() {
            return outline;
        }

        public boolean isAlive() {
            return ticksTillRemoval >= -FADE_TICKS;
        }

        private boolean isFading() {
            return ticksTillRemoval < 0;
        }

        public void tick() {
            ticksTillRemoval--;
            outline.tick();
        }
    }
}
