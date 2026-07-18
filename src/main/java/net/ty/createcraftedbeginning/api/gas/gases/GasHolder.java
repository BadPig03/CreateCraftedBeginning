package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasHolder<R, T extends R> extends DeferredHolder<R, T> {
    /**
     * Creates a new {@code GasHolder} instance.
     *
     * @param key the key identifying the target value
     */
    public GasHolder(ResourceKey<R> key) {
        super(key);
    }

    /**
     * Returns the name.
     *
     * @return the name
     */
    public String getName() {
        return getId().getPath();
    }
}
