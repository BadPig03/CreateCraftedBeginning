package net.ty.createcraftedbeginning.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class SimpleCCBTrigger extends CriterionTriggerBase<SimpleCCBTrigger.Instance> {
    public SimpleCCBTrigger(String id) {
        super(id);
    }

    public void trigger(ServerPlayer player) {
        super.trigger(player, null);
    }

    public SimpleCCBTrigger.Instance instance() {
        return new SimpleCCBTrigger.Instance();
    }

    @Override
    public @NotNull Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public static class Instance extends CriterionTriggerBase.Instance {
        private static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player)).apply(instance, Instance::new));

        @SuppressWarnings("all")
        private final Optional<ContextAwarePredicate> player;

        public Instance() {
            player = Optional.empty();
        }

        @SuppressWarnings("all")
        public Instance(Optional<ContextAwarePredicate> player) {
            this.player = player;
        }

        @Override
        protected boolean test(@Nullable List<Supplier<Object>> suppliers) {
            return true;
        }

        @Override
        public @NotNull Optional<ContextAwarePredicate> player() {
            return player;
        }
    }
}
