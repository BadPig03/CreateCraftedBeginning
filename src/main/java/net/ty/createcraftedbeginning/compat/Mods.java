package net.ty.createcraftedbeginning.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public enum Mods {
    DRAGONS_PLUS("create_dragons_plus");

    private final String id;

    Mods(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public @NotNull Item getItem(String id) {
        return BuiltInRegistries.ITEM.get(asResource(id));
    }

    @Contract("_ -> new")
    public @NotNull ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(id, path);
    }

    public @NotNull Item getItem(ResourceLocation id) {
        return BuiltInRegistries.ITEM.get(id);
    }

    public <T> Optional<T> runIfInstalled(Supplier<Supplier<T>> toRun) {
        return isLoaded() ? Optional.of(toRun.get().get()) : Optional.empty();
    }

    public boolean isLoaded() {
        return ModList.get().isLoaded(id);
    }

    public void executeIfInstalled(Supplier<Runnable> toExecute) {
        if (!isLoaded()) {
            return;
        }

        toExecute.get().run();
    }
}
