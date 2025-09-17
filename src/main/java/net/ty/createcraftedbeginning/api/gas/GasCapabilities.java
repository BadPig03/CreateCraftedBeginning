package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public final class GasCapabilities {
    private GasCapabilities() {
    }

    @SuppressWarnings("SameParameterValue")
    private static ResourceLocation create(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, path);
    }

    public static final class GasHandler {
        public static final BlockCapability<IGasHandler, @Nullable Direction> BLOCK = BlockCapability.createSided(create("gas_handler"), IGasHandler.class);
        public static final EntityCapability<IGasHandler, @Nullable Direction> ENTITY = EntityCapability.createSided(create("gas_handler"), IGasHandler.class);

        private GasHandler() {
        }
    }
}
