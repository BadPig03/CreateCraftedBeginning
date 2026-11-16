package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserStaticDependencies.Sample;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserRenderGeometry {
    private final WeakReference<Level> world;
    private final @Nullable LaserTraceDependencies dependencies;
    private final @Nullable LaserStaticDependencies synchronizedDependencies;
    private final List<Sample> guarded;
    private @Nullable LaserWorldSections sections;

    LaserRenderGeometry(WeakReference<Level> world, LaserTraceDependencies dependencies) {
        this.world = world;
        this.dependencies = dependencies;
        synchronizedDependencies = null;
        guarded = List.of();
    }

    public LaserRenderGeometry(Level level, LaserStaticDependencies dependencies) {
        world = new WeakReference<>(level);
        this.dependencies = null;
        synchronizedDependencies = dependencies;
        guarded = dependencies.blocks().stream().filter(sample -> sample.optical() != null || !LaserWorldSections.stable(sample.state())).toList();
    }

    public boolean matches(Level level) {
        return matches(level, 1);
    }

    public boolean matches(Level level, float partialTick) {
        if (world.get() != level) {
            return false;
        }

        if (synchronizedDependencies == null) {
            return Objects.requireNonNull(dependencies, "Static laser render geometry requires world dependencies when no synchronized dependencies are available.").matchesStaticWorld(level);
        }

        if (!synchronizedDependencies.matchesWorld(level, partialTick)) {
            sections = null;
            return false;
        }

        if (sections != null && sections.matches(level)) {
            return synchronizedDependencies.matchesSamples(level, partialTick, guarded);
        }

        LaserWorldSections candidate = new LaserWorldSections(level);
        for (BlockPos pos : synchronizedDependencies.loaded()) {
            candidate.include(level, pos);
        }
        for (Sample sample : synchronizedDependencies.blocks()) {
            candidate.include(level, sample.pos());
        }
        sections = null;
        if (!synchronizedDependencies.matchesBlocks(level, partialTick)) {
            return false;
        }

        if (!candidate.isComplete()) {
            return true;
        }

        if (!candidate.matches(level)) {
            return false;
        }

        sections = candidate;
        return true;
    }
}
