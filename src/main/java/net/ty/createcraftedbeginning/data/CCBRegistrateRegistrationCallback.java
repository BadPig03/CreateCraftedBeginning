package net.ty.createcraftedbeginning.data;

import com.tterrag.registrate.util.nullness.NonNullConsumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class CCBRegistrateRegistrationCallback {
    private CCBRegistrateRegistrationCallback() {
    }

    public static <R, T extends R> void register(ResourceKey<? extends Registry<R>> registry, ResourceLocation id, NonNullConsumer<? super T> callback) {
        CCBRegistrateRegistrationCallbackImpl.<R, T>register(registry, id, callback);
    }

    public static void provideRegistrate(CCBRegistrate registrate) {
        CCBRegistrateRegistrationCallbackImpl.provideRegistrate(registrate);
    }
}
