package net.ty.createcraftedbeginning.api.gascanisters;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightHatchCanisters {
    private AirtightHatchCanisters() {
    }

    @Nullable
    public static IAirtightHatchCanister get(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        IGasCanisterContainer container = stack.getCapability(GasHandler.ITEM);
        if (!(container instanceof IAirtightHatchCanister hatchCanister)) {
            return null;
        }
        return hatchCanister;
    }

    public static boolean isCompatible(ItemStack stack) {
        return get(stack) != null;
    }
}
