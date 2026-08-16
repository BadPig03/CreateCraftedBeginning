package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasCapabilities {
    private GasCapabilities() {
    }

    public static boolean hasGasCapability(BlockGetter level, BlockPos pos, Direction side) {
        return level instanceof Level l && l.getCapability(GasHandler.BLOCK, pos, side) != null;
    }

    public static final class GasHandler {
        public static final BlockCapability<IGasHandler, @Nullable Direction> BLOCK = BlockCapability.createSided(CCBAPI.asResource("gas_handler"), IGasHandler.class);
        @SuppressWarnings("unused")
        public static final EntityCapability<IGasHandler, @Nullable Direction> ENTITY = EntityCapability.createSided(CCBAPI.asResource("gas_handler"), IGasHandler.class);
        public static final ItemCapability<IGasCanisterContainer, @Nullable Void> ITEM = ItemCapability.createVoid(CCBAPI.asResource("gas_canister_container"), IGasCanisterContainer.class);

        private GasHandler() {
        }
    }
}
