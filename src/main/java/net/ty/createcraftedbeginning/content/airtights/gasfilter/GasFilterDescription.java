package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.recipe.gas.ingredient.GasStackLinkedSet;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasFilterDescription(boolean blacklist, boolean respectData, List<GasStack> gases) {
    public GasFilterDescription {
        Set<GasStack> seenGases = respectData ? GasStackLinkedSet.createTypeAndComponentsSet() : GasStackLinkedSet.createTypeSet();
        List<GasStack> samples = new ArrayList<>();
        for (GasStack gas : gases) {
            if (gas.isEmpty()) {
                continue;
            }

            GasStack sample = gas.copyWithAmount(1);
            if (!seenGases.add(sample)) {
                continue;
            }

            samples.add(sample);
        }
        gases = List.copyOf(samples);
    }

    public static GasFilterDescription ofContainer(GasCanisterContainer container) {
        List<GasStack> gases = new ArrayList<>();
        for (int tank = 0; tank < container.getTanks(); tank++) {
            gases.add(container.getGasInTank(tank));
        }
        return new GasFilterDescription(false, true, gases);
    }

    @Override
    public @Unmodifiable List<GasStack> gases() {
        return gases.stream().map(GasStack::copy).toList();
    }
}
