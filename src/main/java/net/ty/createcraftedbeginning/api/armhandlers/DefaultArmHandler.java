package net.ty.createcraftedbeginning.api.armhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DefaultArmHandler {
    public static final AirtightArmStats INSTANCE = new AirtightArmStats(1, 2, 2, 0.5f);

    private DefaultArmHandler() {
    }
}
