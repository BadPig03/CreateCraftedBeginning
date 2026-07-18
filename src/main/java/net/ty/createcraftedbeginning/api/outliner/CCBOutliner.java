package net.ty.createcraftedbeginning.api.outliner;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.outliner.CCBOutline.CCBOutlineParams;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public enum CCBOutliner {
    INSTANCE;

    private final Map<Object, CCBOutlineEntry> outlines = Collections.synchronizedMap(new HashMap<>());
    private final Map<Object, CCBOutlineEntry> outlinesView = Collections.unmodifiableMap(outlines);

    private static float getFadeAlpha(CCBOutlineEntry entry, float partialTicks) {
        int previousTicks = entry.ticksTillRemoval + 1;
        float previousAlpha = previousTicks >= 0 ? 1 : 1 + previousTicks / (float) CCBOutlineEntry.FADE_TICKS;
        float currentAlpha = 1 + entry.ticksTillRemoval / (float) CCBOutlineEntry.FADE_TICKS;
        float alpha = Mth.lerp(partialTicks, previousAlpha, currentAlpha);
        return (float) Math.pow(alpha, 3);
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

    /**
     * Displays the cluster.
     *
     * @param slot      the zero-based slot index
     * @param selection the selection to inspect or process
     * @return the resulting CCB outline params
     */
    public CCBOutlineParams showCluster(Object slot, Iterable<BlockPos> selection) {
        XRayBlockClusterOutline outline = new XRayBlockClusterOutline(selection);
        addOutline(slot, outline);
        return outline.getParams();
    }

    /**
     * Keeps the specified outline alive for the current tick.
     *
     * @param slot the zero-based slot index
     */
    public void keep(Object slot) {
        CCBOutlineEntry entry = outlines.get(slot);
        if (entry == null) {
            return;
        }

        entry.ticksTillRemoval = 1;
    }

    /**
     * Updates state by performing remove.
     *
     * @param slot the zero-based slot index
     */
    public void remove(Object slot) {
        outlines.remove(slot);
    }

    /**
     * Returns the parameters for an existing outline so they can be modified.
     *
     * @param slot the zero-based slot index
     * @return an optional containing the result, or an empty optional when unavailable
     */
    public Optional<CCBOutlineParams> edit(Object slot) {
        CCBOutlineEntry entry = outlines.get(slot);
        if (entry == null) {
            return Optional.empty();
        }

        entry.ticksTillRemoval = 1;
        return Optional.of(entry.getOutline().getParams());
    }

    /**
     * Returns the outlines.
     *
     * @return the outlines
     */
    public Map<Object, CCBOutlineEntry> getOutlines() {
        return outlinesView;
    }

    private void addOutline(Object slot, CCBOutline outline) {
        outlines.put(slot, new CCBOutlineEntry(outline));
    }

    /**
     * Updates all active outlines for one game tick.
     */
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

    /**
     * Renders the outlines.
     *
     * @param poseStack    the pose stack to inspect or process
     * @param buffer       the buffer to read from or write to
     * @param camera       the camera to use
     * @param partialTicks the partial-tick interpolation value
     */
    public void renderOutlines(PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 camera, float partialTicks) {
        outlines.forEach((ignoredSlot, entry) -> renderOutline(entry, poseStack, buffer, camera, partialTicks));
    }

    public static class CCBOutlineEntry {
        public static final int FADE_TICKS = 8;

        private final CCBOutline outline;
        private int ticksTillRemoval = 1;

        /**
         * Creates a new {@code CCBOutlineEntry} instance.
         *
         * @param outline the outline to use
         */
        public CCBOutlineEntry(CCBOutline outline) {
            this.outline = outline;
        }

        /**
         * Returns the outline.
         *
         * @return the outline
         */
        public CCBOutline getOutline() {
            return outline;
        }

        /**
         * Returns the ticks till removal.
         *
         * @return the ticks till removal
         */
        public int getTicksTillRemoval() {
            return ticksTillRemoval;
        }

        /**
         * Checks whether this value is alive.
         *
         * @return {@code true} if this value is alive; otherwise {@code false}
         */
        public boolean isAlive() {
            return ticksTillRemoval >= -FADE_TICKS;
        }

        /**
         * Checks whether this value is fading.
         *
         * @return {@code true} if this value is fading; otherwise {@code false}
         */
        public boolean isFading() {
            return ticksTillRemoval < 0;
        }

        /**
         * Updates this object for one game tick.
         */
        public void tick() {
            ticksTillRemoval--;
            outline.tick();
        }
    }
}
