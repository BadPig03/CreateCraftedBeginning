package net.ty.createcraftedbeginning.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;

import java.util.Optional;
import java.util.function.Supplier;

public enum Mods {
    DRAGONS_PLUS("create_dragons_plus");

    private final String id;

    Mods(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(id, path);
    }

    public Item getItem(String id) {
        return BuiltInRegistries.ITEM.get(asResource(id));
    }

    public Item getItem(ResourceLocation id) {
        return BuiltInRegistries.ITEM.get(id);
    }

    public boolean isLoaded() {
        return ModList.get().isLoaded(id);
    }

    public <T> Optional<T> runIfInstalled(Supplier<Supplier<T>> toRun) {
        if (isLoaded()) {
            return Optional.of(toRun.get().get());
        }
        return Optional.empty();
    }

    public void executeIfInstalled(Supplier<Runnable> toExecute) {
        if (isLoaded()) {
            toExecute.get().run();
        }
    }
}
