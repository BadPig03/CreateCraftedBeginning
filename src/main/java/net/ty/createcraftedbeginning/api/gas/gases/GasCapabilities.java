package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public final class GasCapabilities {
    private GasCapabilities() {
    }

    public static final class GasHandler {
        public static final BlockCapability<IGasHandler, @Nullable Direction> BLOCK = BlockCapability.createSided(create(), IGasHandler.class);
        public static final EntityCapability<IGasHandler, @Nullable Direction> ENTITY = EntityCapability.createSided(create(), IGasHandler.class);

        private GasHandler() {
        }

        @Contract(" -> new")
        private static @NotNull ResourceLocation create() {
            return ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, "gas_handler");
        }
    }
}
