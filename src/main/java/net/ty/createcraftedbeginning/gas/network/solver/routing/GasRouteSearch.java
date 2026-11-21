package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkState;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasRouteSearch {
    static GasRouteSearch ordinary(GasNetworkState state, Map<BlockPos, Long> limits, BlockPos from, BlockPos to) {
        return new GasOrdinaryPathSearch(state, limits, from, to);
    }

    static GasRouteSearch zoned(Level level, GasZoneLayout layout, BlockFace from, BlockFace to, GasStack gas, GasNetworkPressureEndpoint supply, GasNetworkPressureEndpoint demand) {
        return new GasZoneRouteSearch(level, layout, from, to, gas, supply, demand);
    }

    boolean pending();

    @Nullable
    Path expand();

    record Path(BlockPos pos, long capacity, @Nullable Path previous, @Nullable GasZoneRoute zoneRoute) {
        Path(BlockPos pos, long capacity, @Nullable Path previous) {
            this(pos, capacity, previous, null);
        }
    }
}
