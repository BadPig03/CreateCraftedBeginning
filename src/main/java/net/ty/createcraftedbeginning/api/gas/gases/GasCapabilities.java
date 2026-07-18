package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public final class GasCapabilities {
    private GasCapabilities() {
    }

    /**
     * Checks whether the target exposes a gas capability on the requested side.
     *
     * @param level the level in which the operation is performed
     * @param pos   the target block position
     * @param side  the side from which the target is accessed
     * @return {@code true} if the target exposes a gas capability on the requested side; otherwise {@code
     * false}
     */
    public static boolean hasGasCapability(BlockGetter level, BlockPos pos, Direction side) {
        return level instanceof Level l && l.getCapability(GasHandler.BLOCK, pos, side) != null;
    }

    public static final class GasHandler {
        public static final BlockCapability<IGasHandler, @Nullable Direction> BLOCK = BlockCapability.createSided(CreateCraftedBeginning.asResource("gas_handler"), IGasHandler.class);
        public static final EntityCapability<IGasHandler, @Nullable Direction> ENTITY = EntityCapability.createSided(CreateCraftedBeginning.asResource("gas_handler"), IGasHandler.class);
        public static final ItemCapability<IGasCanisterContainer, @Nullable Void> ITEM = ItemCapability.createVoid(CreateCraftedBeginning.asResource("gas_canister_container"), IGasCanisterContainer.class);

        private GasHandler() {
        }
    }
}
