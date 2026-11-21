package net.ty.createcraftedbeginning.gas.network.solver.scheduling;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasRouteSearch.Path;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute.ParallelSpan;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasParallelRoutes {
    private List<Path> paths;
    private @Nullable ParallelSpan span;

    GasParallelRoutes(Path first) {
        paths = List.of(first);
    }

    List<Path> paths() {
        return paths;
    }

    int learn(Path path, GasZoneRoute route) {
        for (int index = 0; index < paths.size(); index++) {
            if (route.samePath(Objects.requireNonNull(paths.get(index).zoneRoute()))) {
                return index;
            }
        }

        ParallelSpan alternative = route.parallelSpan(Objects.requireNonNull(paths.getFirst().zoneRoute()));
        if (alternative == null || span != null && !span.equals(alternative)) {
            return -1;
        }

        span = alternative;
        List<Path> next = new ArrayList<>(paths);
        next.add(path);
        paths = List.copyOf(next);
        return paths.size() - 1;
    }
}
