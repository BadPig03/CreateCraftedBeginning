package net.ty.createcraftedbeginning.compat.functionalstorage.access;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface GasConnectedDrawersAccess {
    List<GasHandler> ccb$getGasHandlers();
}
