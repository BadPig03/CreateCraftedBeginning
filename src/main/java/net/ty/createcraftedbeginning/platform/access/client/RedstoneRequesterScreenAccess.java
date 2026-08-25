package net.ty.createcraftedbeginning.platform.access.client;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

import java.util.List;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface RedstoneRequesterScreenAccess {
    List<Integer> ccb$getAmounts();
}
