package net.ty.createcraftedbeginning.api.outliner;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.outliner.CCBOutline.CCBOutlineParams;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
public enum CCBOutliner {
    INSTANCE;

    private final Map<Object, CCBOutlineEntry> outlines = Collections.synchronizedMap(new HashMap<>());
    private final Map<Object, CCBOutlineEntry> outlinesView = Collections.unmodifiableMap(outlines);

    public CCBOutlineParams showCluster(Object slot, Iterable<BlockPos> selection) {
        XRayBlockClusterOutline outline = new XRayBlockClusterOutline(selection);
        addOutline(slot, outline);
        return outline.getParams();
    }

    public void keep(Object slot) {
        if (!outlines.containsKey(slot)) {
            return;
        }

        outlines.get(slot).ticksTillRemoval = 1;
    }

    public void remove(Object slot) {
        outlines.remove(slot);
    }

    public Optional<CCBOutlineParams> edit(Object slot) {
        keep(slot);
        return outlines.containsKey(slot) ? Optional.of(outlines.get(slot).getOutline().getParams()) : Optional.empty();
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

    public void renderOutlines(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
        outlines.forEach((key, entry) -> {
            CCBOutline outline = entry.getOutline();
            CCBOutlineParams params = outline.getParams();
            params.alpha = 1;
            if (entry.isFading()) {
                int prevTicks = entry.ticksTillRemoval + 1;
                float fadeTicks = CCBOutlineEntry.FADE_TICKS;
                float lastAlpha = prevTicks >= 0 ? 1 : 1 + prevTicks / fadeTicks;
                float currentAlpha = 1 + entry.ticksTillRemoval / fadeTicks;
                float alpha = Mth.lerp(pt, lastAlpha, currentAlpha);
                params.alpha = (float) Math.pow(alpha, 3);
				if (params.alpha < 0.125f) {
					return;
				}
            }
            outline.render(ms, buffer, camera, pt);
        });
    }

    public static class CCBOutlineEntry {
        public static final int FADE_TICKS = 8;

        private final CCBOutline outline;
        private int ticksTillRemoval = 1;

        public CCBOutlineEntry(CCBOutline newOutline) {
            outline = newOutline;
        }

        public CCBOutline getOutline() {
            return outline;
        }

        public int getTicksTillRemoval() {
            return ticksTillRemoval;
        }

        public boolean isAlive() {
            return ticksTillRemoval >= -FADE_TICKS;
        }

        public boolean isFading() {
            return ticksTillRemoval < 0;
        }

        public void tick() {
            ticksTillRemoval--;
            outline.tick();
        }
    }
}
