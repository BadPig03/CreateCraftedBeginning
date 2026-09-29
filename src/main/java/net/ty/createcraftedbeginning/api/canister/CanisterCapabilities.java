package net.ty.createcraftedbeginning.api.canister;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.ty.createcraftedbeginning.api.CCBAPI;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CanisterCapabilities {
    public static final ItemCapability<GasCanisterContainer, @Nullable Void> ITEM = ItemCapability.createVoid(CCBAPI.asResource("gas_canister_container"), GasCanisterContainer.class);

    private CanisterCapabilities() {
    }
}
