package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface OutletSideRelief extends GasPressureBoundary {
    static boolean crossesInlet(GasNetworkPressureEndpoint target, Direction entry, Direction outlet) {
        return target.access().fillHandler() instanceof OutletSideRelief && entry == outlet.getOpposite();
    }
}
