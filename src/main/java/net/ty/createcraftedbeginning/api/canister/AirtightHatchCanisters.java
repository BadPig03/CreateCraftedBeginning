package net.ty.createcraftedbeginning.api.canister;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightHatchCanisters {
    private AirtightHatchCanisters() {
    }

    @Nullable
    public static AirtightHatchCanister of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        GasCanisterContainer container = stack.getCapability(CanisterCapabilities.ITEM);
        if (!(container instanceof AirtightHatchCanister hatchCanister)) {
            return null;
        }

        return hatchCanister;
    }

    public static boolean isCompatible(ItemStack stack) {
        return of(stack) != null;
    }
}
