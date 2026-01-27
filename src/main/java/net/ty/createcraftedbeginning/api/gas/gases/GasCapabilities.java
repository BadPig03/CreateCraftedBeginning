package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.cansiters.IGasCanisterContainer;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public final class GasCapabilities {
    private GasCapabilities() {
    }

    public static final class GasHandler {
        public static final BlockCapability<IGasHandler, @Nullable Direction> BLOCK = BlockCapability.createSided(CreateCraftedBeginning.asResource("gas_handler"), IGasHandler.class);
        public static final EntityCapability<IGasHandler, @Nullable Direction> ENTITY = EntityCapability.createSided(CreateCraftedBeginning.asResource("gas_handler"), IGasHandler.class);
        public static final ItemCapability<IGasCanisterContainer, @Nullable Void> ITEM = ItemCapability.createVoid(CreateCraftedBeginning.asResource("gas_canister_container"), IGasCanisterContainer.class);

        private GasHandler() {
        }
    }
}
