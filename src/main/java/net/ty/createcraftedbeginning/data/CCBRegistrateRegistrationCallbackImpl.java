package net.ty.createcraftedbeginning.data;

import com.mojang.datafixers.util.Either;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CCBRegistrateRegistrationCallbackImpl {
    private static final Map<String, Either<List<CallbackImpl<?, ?>>, CCBRegistrate>> CALLBACKS = new HashMap<>();

    public static void provideRegistrate(@NotNull CCBRegistrate registrate) {
        synchronized (CALLBACKS) {
            String modId = registrate.getModid();
            Either<List<CallbackImpl<?, ?>>, CCBRegistrate> either = CALLBACKS.remove(modId);
            if (either != null) {
                Optional<List<CallbackImpl<?, ?>>> optionalCallbacks = either.left();
                if (optionalCallbacks.isEmpty()) {
                    throw new IllegalArgumentException("Tried to register a duplicate CCBRegistrate instance for mod Id: " + modId);
                }

                optionalCallbacks.get().forEach(callback -> callback.addToRegistrate(registrate));
            }
            CALLBACKS.put(modId, Either.right(registrate));
        }
    }

    public static <R, T extends R> void register(ResourceKey<? extends Registry<R>> registry, ResourceLocation id, NonNullConsumer<? super T> callback) {
        CallbackImpl<R, T> callbackImpl = new CallbackImpl<>(registry, id, callback);
        Either<List<CallbackImpl<?, ?>>, CCBRegistrate> either;
        synchronized (CALLBACKS) {
            either = CALLBACKS.computeIfAbsent(id.getNamespace(), string -> Either.left(new ArrayList<>()));
            either.ifLeft(callbacks -> callbacks.add(callbackImpl));
        }
        either.ifRight(callbackImpl::addToRegistrate);
    }

    private record CallbackImpl<R, T extends R>(ResourceKey<? extends Registry<R>> registry, ResourceLocation id, NonNullConsumer<? super T> callback) {
        public void addToRegistrate(@NotNull CCBRegistrate registrate) {
            registrate.<R, T>addRegisterCallback(id.getPath(), registry, callback);
        }
    }
}
