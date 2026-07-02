package net.ty.createcraftedbeginning.compat;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum CCBCompatMods {
    JEI("jei");

    private final String id;

    CCBCompatMods(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Item getItem(String id) {
        return BuiltInRegistries.ITEM.get(asResource(id));
    }

    @Contract("_ -> new")
    public ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(id, path);
    }

    public Item getItem(ResourceLocation id) {
        return BuiltInRegistries.ITEM.get(id);
    }

    public <T> Optional<T> runIfInstalled(Supplier<Supplier<T>> toRun) {
        if (!isLoaded()) {
            return Optional.empty();
        }
        return Optional.of(toRun.get().get());
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
