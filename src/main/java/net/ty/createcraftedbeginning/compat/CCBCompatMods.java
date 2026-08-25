package net.ty.createcraftedbeginning.compat;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum CCBCompatMods {
    COMPUTERCRAFT("computercraft"),
    CREATE_DRAGONS_PLUS("create_dragons_plus"),
    CREATE_FLUID_LOGISTICS("fluidlogistics"),
    DNDESIRES("dndesires"),
    FUNCTIONAL_STORAGE("functionalstorage"),
    JADE("jade"),
    JEI("jei"),
    KUBEJS("kubejs"),
    SABLE("sable");

    private final String id;

    CCBCompatMods(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Contract("_ -> new")
    public ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(id, path);
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
