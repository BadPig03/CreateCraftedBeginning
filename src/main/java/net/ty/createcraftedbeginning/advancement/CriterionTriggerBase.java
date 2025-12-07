package net.ty.createcraftedbeginning.advancement;

import com.google.common.collect.Maps;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger.SimpleInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.advancement.CriterionTriggerBase.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public abstract class CriterionTriggerBase<T extends Instance> implements CriterionTrigger<T> {
    protected final Map<PlayerAdvancements, Set<Listener<T>>> listeners = Maps.newHashMap();

    private final ResourceLocation id;

    public CriterionTriggerBase(String id) {
        this.id = CreateCraftedBeginning.asResource(id);
    }

    @Override
    public void addPlayerListener(@NotNull PlayerAdvancements playerAdvancementsIn, @NotNull Listener<T> listener) {
        Set<Listener<T>> playerListeners = listeners.computeIfAbsent(playerAdvancementsIn, advancements -> new HashSet<>());
        playerListeners.add(listener);
    }

    @Override
    public void removePlayerListener(@NotNull PlayerAdvancements playerAdvancementsIn, @NotNull Listener<T> listener) {
        Set<Listener<T>> playerListeners = listeners.get(playerAdvancementsIn);
        if (playerListeners == null) {
            return;
        }

        playerListeners.remove(listener);
        if (!playerListeners.isEmpty()) {
            return;
        }

        listeners.remove(playerAdvancementsIn);
    }

    @Override
    public void removePlayerListeners(@NotNull PlayerAdvancements playerAdvancementsIn) {
        listeners.remove(playerAdvancementsIn);
    }

    public ResourceLocation getId() {
        return id;
    }

    protected void trigger(@NotNull ServerPlayer player, @Nullable List<Supplier<Object>> suppliers) {
        PlayerAdvancements advancements = player.getAdvancements();
        Set<Listener<T>> playerListeners = listeners.get(advancements);
        if (playerListeners == null) {
            return;
        }

        List<Listener<T>> list = new LinkedList<>();
        for (Listener<T> listener : playerListeners) {
            if (listener.trigger().test(suppliers)) {
                list.add(listener);
            }
        }
        list.forEach(listener -> listener.run(advancements));
    }

    public abstract static class Instance implements SimpleInstance {
        protected abstract boolean test(@Nullable List<Supplier<Object>> suppliers);
    }
}
