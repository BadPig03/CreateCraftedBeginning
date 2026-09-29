package net.ty.createcraftedbeginning.compat.functionalstorage.access;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasControllerAccess {
    @Nullable GasHandler ccb$getGasHandler();
}
