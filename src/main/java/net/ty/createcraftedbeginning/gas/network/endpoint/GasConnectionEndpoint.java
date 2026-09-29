package net.ty.createcraftedbeginning.gas.network.endpoint;

import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class GasConnectionEndpoint {
    protected final BlockFace location;

    protected GasConnectionEndpoint(BlockFace location) {
        this.location = location;
    }

    public abstract void bind(Level level, BlockEntity networkBlockEntity);

    @Nullable
    public abstract ICapabilityProvider<GasHandler> getGasHandlerProvider();
}
