package net.ty.createcraftedbeginning.gas.network.solver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.gas.network.solver.GasOrdinaryPathSearch.Path;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
interface GasRouteSearch {
    boolean pending();

    @Nullable
    Path expand();
}
