package net.ty.createcraftedbeginning.data;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CCBDeferredHolder<R, T extends R> extends DeferredHolder<R, T> {
    public CCBDeferredHolder(ResourceKey<? extends Registry<R>> registryKey, ResourceLocation valueName) {
        this(ResourceKey.create(registryKey, valueName));
    }

    public CCBDeferredHolder(ResourceKey<R> key) {
        super(key);
    }

    public String getName() {
        return getId().getPath();
    }

    public @NotNull ResourceLocation getId() {
        return this.getKey().location();
    }

    public boolean is(R other) {
        return get() == other;
    }

    public boolean keyMatches(@NotNull Holder<R> holder) {
        return holder.is(getKey());
    }
}
