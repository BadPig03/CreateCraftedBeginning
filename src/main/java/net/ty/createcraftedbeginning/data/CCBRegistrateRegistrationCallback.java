package net.ty.createcraftedbeginning.data;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public class CCBRegistrateRegistrationCallback {
    public static <T> void register(ResourceKey<? extends Registry<T>> registry, ResourceLocation id, Consumer<T> callback) {
        CCBRegistrateRegistrationCallbackImpl.register(registry, id, callback);
    }
}
