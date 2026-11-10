package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.Objects;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserRenderGeometry {
    private final WeakReference<Level> world;
    private final @Nullable LaserTraceDependencies dependencies;
    private final @Nullable LaserStaticDependencies synchronizedDependencies;

    LaserRenderGeometry(WeakReference<Level> world, LaserTraceDependencies dependencies) {
        this.world = world;
        this.dependencies = dependencies;
        synchronizedDependencies = null;
    }

    public LaserRenderGeometry(Level level, LaserStaticDependencies dependencies) {
        world = new WeakReference<>(level);
        this.dependencies = null;
        synchronizedDependencies = dependencies;
    }

    public boolean matches(Level level) {
        if (synchronizedDependencies != null) {
            return world.get() == level && synchronizedDependencies.matches(level, 1);
        }

        return world.get() == level && Objects.requireNonNull(dependencies, "Static laser render geometry requires world dependencies when no synchronized dependencies are available.").matchesStaticWorld(level);
    }
}
