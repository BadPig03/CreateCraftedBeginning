package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasCapabilities {
    public static final BlockCapability<GasHandler, @Nullable Direction> BLOCK = BlockCapability.createSided(CCBAPI.asResource("gas_handler"), GasHandler.class);
    @SuppressWarnings("unused")
    public static final EntityCapability<GasHandler, @Nullable Direction> ENTITY = EntityCapability.createSided(CCBAPI.asResource("gas_handler"), GasHandler.class);

    private GasCapabilities() {
    }

    public static boolean hasBlockHandler(BlockGetter level, BlockPos pos, Direction side) {
        return level instanceof Level serverLevel && serverLevel.getCapability(BLOCK, pos, side) != null;
    }
}
