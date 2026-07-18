package net.ty.createcraftedbeginning.data;

import com.mojang.datafixers.util.Either;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBRegistrateRegistrationCallbackImpl {
    private static final Map<String, Either<List<CallbackImpl<?, ?>>, CCBRegistrate>> CALLBACKS = new HashMap<>();

    public static void provideRegistrate(CCBRegistrate registrate) {
        synchronized (CALLBACKS) {
            String modId = registrate.getModid();
            Either<List<CallbackImpl<?, ?>>, CCBRegistrate> registration = CALLBACKS.remove(modId);
            if (registration == null) {
                CALLBACKS.put(modId, Either.right(registrate));
                return;
            }

            List<CallbackImpl<?, ?>> callbacks = registration.left().orElseThrow(() -> new IllegalArgumentException("Tried to register a duplicate CCBRegistrate instance for mod Id: " + modId));
            callbacks.forEach(callback -> callback.addToRegistrate(registrate));
            CALLBACKS.put(modId, Either.right(registrate));
        }
    }

    public static <R, T extends R> void register(ResourceKey<? extends Registry<R>> registry, ResourceLocation id, NonNullConsumer<? super T> callback) {
        CallbackImpl<R, T> callbackImpl = new CallbackImpl<>(registry, id, callback);
        Either<List<CallbackImpl<?, ?>>, CCBRegistrate> registration;
        synchronized (CALLBACKS) {
            registration = CALLBACKS.computeIfAbsent(id.getNamespace(), namespace -> Either.left(new ArrayList<>()));
            registration.ifLeft(callbacks -> callbacks.add(callbackImpl));
        }
        registration.ifRight(callbackImpl::addToRegistrate);
    }

    private record CallbackImpl<R, T extends R>(ResourceKey<? extends Registry<R>> registry, ResourceLocation id, NonNullConsumer<? super T> callback) {
        public void addToRegistrate(CCBRegistrate registrate) {
            registrate.<R, T>addRegisterCallback(id.getPath(), registry, callback);
        }
    }
}
