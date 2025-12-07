package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;

public class GasHolder<R, T extends R> extends DeferredHolder<R, T> {
    public GasHolder(ResourceKey<R> key) {
        super(key);
    }

    public String getName() {
        return getId().getPath();
    }
}
