package net.ty.createcraftedbeginning.content.airtightcannon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.airtightcannon.AirtightCannonProjectileEntityHitAction;
import net.ty.createcraftedbeginning.registry.CCBBuiltInRegistries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CCBAirtightCannonProjectileEntityHitActions {
    static {
        register("set_on_fire", SetOnFire.CODEC);
        register("potion_effect", PotionEffect.CODEC);
    }

    public static void init() {
    }

    private static void register(String name, MapCodec<? extends AirtightCannonProjectileEntityHitAction> codec) {
        Registry.register(CCBBuiltInRegistries.POTATO_PROJECTILE_ENTITY_HIT_ACTION, CreateCraftedBeginning.asResource(name), codec);
    }

    private static void applyEffect(LivingEntity entity, @NotNull MobEffectInstance effect) {
        if (effect.getEffect().value().isInstantenous()) {
            effect.getEffect().value().applyInstantenousEffect(null, null, entity, effect.getDuration(), 1.0);
        } else {
            entity.addEffect(effect);
        }
    }

    public record SetOnFire(int ticks) implements AirtightCannonProjectileEntityHitAction {
        public static final MapCodec<SetOnFire> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(ExtraCodecs.POSITIVE_INT.fieldOf("ticks").forGetter(SetOnFire::ticks)).apply(instance, SetOnFire::new));

        @Contract("_ -> new")
        public static @NotNull SetOnFire seconds(int seconds) {
            return new SetOnFire(seconds * 20);
        }

        @Override
        public boolean execute(ItemStack projectile, @NotNull EntityHitResult ray, Type type) {
            ray.getEntity().setRemainingFireTicks(ticks);
            return false;
        }

        @Override
        public MapCodec<? extends AirtightCannonProjectileEntityHitAction> codec() {
            return CODEC;
        }
    }

    public record PotionEffect(Holder<MobEffect> effect, int level, int ticks, boolean recoverable) implements AirtightCannonProjectileEntityHitAction {
        public static final MapCodec<PotionEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(PotionEffect::effect), ExtraCodecs.POSITIVE_INT.fieldOf("level").forGetter(PotionEffect::level), ExtraCodecs.POSITIVE_INT.fieldOf("ticks").forGetter(PotionEffect::ticks), Codec.BOOL.fieldOf("recoverable").forGetter(PotionEffect::recoverable)).apply(instance, PotionEffect::new));

        @Override
        public boolean execute(ItemStack projectile, @NotNull EntityHitResult ray, Type type) {
            Entity entity = ray.getEntity();
            if (entity.level().isClientSide) {
                return true;
            }
            if (entity instanceof LivingEntity) {
                applyEffect((LivingEntity) entity, new MobEffectInstance(effect, ticks, level - 1));
            }
            return !recoverable;
        }

        @Override
        public MapCodec<? extends AirtightCannonProjectileEntityHitAction> codec() {
            return CODEC;
        }
    }
}
