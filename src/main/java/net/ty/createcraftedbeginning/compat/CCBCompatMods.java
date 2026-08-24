package net.ty.createcraftedbeginning.compat;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum CCBCompatMods {
    CREATE_FLUID_LOGISTICS("fluidlogistics"),
    JEI("jei"),
    SABLE("sable"),
    FUNCTIONAL_STORAGE("functionalstorage");

    private final String id;

    CCBCompatMods(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Item getItem(String path) {
        return BuiltInRegistries.ITEM.get(asResource(path));
    }

    @Contract("_ -> new")
    public ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(id, path);
    }

    public Item getItem(ResourceLocation itemId) {
        return BuiltInRegistries.ITEM.get(itemId);
    }

    public boolean isLoaded() {
        return ModList.get().isLoaded(id);
    }

    public void executeIfInstalled(Supplier<Runnable> actionSupplier) {
        if (!isLoaded()) {
            return;
        }

        actionSupplier.get().run();
    }
}
