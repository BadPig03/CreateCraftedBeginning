package net.ty.createcraftedbeginning.registry;

import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class CCBMaterials {
    public static final ResourceLocation VORTICES_GLINT = register("textures/gas/vortices_glint.png");

    @SuppressWarnings("SameParameterValue")
    @Contract("_ -> new")
    private static @NotNull ResourceLocation register(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateCraftedBeginning.MOD_ID, path);
    }
}
