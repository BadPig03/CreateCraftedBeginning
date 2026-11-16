package net.ty.createcraftedbeginning.mixin.common.accessor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.ty.createcraftedbeginning.platform.access.PlayerFlightCheckAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerImplAccessor extends PlayerFlightCheckAccess {
    @Override
    @Accessor("aboveGroundTickCount")
    void ccb$setAboveGroundTickCount(int ticks);
}
