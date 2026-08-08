package net.ty.createcraftedbeginning.advancement;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger.SimpleInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.ty.createcraftedbeginning.advancement.CriterionTriggerBase.Instance;
import net.ty.createcraftedbeginning.api.CCBAPI;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CriterionTriggerBase<T extends Instance> implements CriterionTrigger<T> {
    protected final Map<PlayerAdvancements, Set<Listener<T>>> listeners = new HashMap<>();

    private final ResourceLocation id;

    public CriterionTriggerBase(String id) {
        this.id = CCBAPI.asResource(id);
    }

    @Override
    public void addPlayerListener(PlayerAdvancements advancements, Listener<T> listener) {
        Set<Listener<T>> playerListeners = listeners.computeIfAbsent(advancements, $ -> new HashSet<>());
        playerListeners.add(listener);
    }

    @Override
    public void removePlayerListener(PlayerAdvancements advancements, Listener<T> listener) {
        Set<Listener<T>> playerListeners = listeners.get(advancements);
        if (playerListeners == null) {
            return;
        }

        playerListeners.remove(listener);
        if (!playerListeners.isEmpty()) {
            return;
        }

        listeners.remove(advancements);
    }

    @Override
    public void removePlayerListeners(PlayerAdvancements advancements) {
        listeners.remove(advancements);
    }

    public ResourceLocation getId() {
        return id;
    }

    protected void trigger(ServerPlayer player, @Nullable List<Supplier<Object>> suppliers) {
        PlayerAdvancements advancements = player.getAdvancements();
        Set<Listener<T>> playerListeners = listeners.get(advancements);
        if (playerListeners == null) {
            return;
        }

        List<Listener<T>> triggeredListeners = new ArrayList<>();
        for (Listener<T> listener : playerListeners) {
            if (!listener.trigger().test(suppliers)) {
                continue;
            }

            triggeredListeners.add(listener);
        }

        for (Listener<T> listener : triggeredListeners) {
            listener.run(advancements);
        }
    }

    public abstract static class Instance implements SimpleInstance {
        protected abstract boolean test(@Nullable List<Supplier<Object>> suppliers);
    }
}
