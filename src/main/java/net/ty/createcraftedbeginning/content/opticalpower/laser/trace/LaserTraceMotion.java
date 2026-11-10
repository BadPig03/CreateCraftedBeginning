package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserQuerySpace.Pose;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RenderGeometry;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record LaserTraceMotion(List<AABB> bounds, List<AABB> paths, Map<UUID, RenderGeometry> subLevels, Map<UUID, Pose> contraptions) {
    @Nullable LaserTraceMotion observe(Level level, float partialTick) {
        Map<UUID, RenderGeometry> currentSubLevels = SubLevelBridge.captureRenderGeometry(level, bounds, paths, partialTick);
        if (currentSubLevels == null) {
            return null;
        }

        Map<UUID, Pose> currentContraptions = Map.of();
        if (!contraptions.isEmpty() || !LaserContraptionBounds.isClear(level, bounds, partialTick)) {
            currentContraptions = LaserTraceDependencies.poses(LaserQuerySpace.mounted(level, partialTick), bounds);
        }
        return new LaserTraceMotion(bounds, paths, currentSubLevels, currentContraptions);
    }
}
