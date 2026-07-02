package net.ty.createcraftedbeginning.registry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.mobeffects.JetpackFlightEffect;
import net.ty.createcraftedbeginning.content.mobeffects.ZombificationEffect;
import net.ty.createcraftedbeginning.content.mobeffects.ZombificationImmunityEffect;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBMobEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, CreateCraftedBeginning.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> JETPACK_FLIGHT = EFFECTS.register("jetpack_flight", () -> new JetpackFlightEffect(MobEffectCategory.BENEFICIAL, 0xB7AA2E));
    public static final DeferredHolder<MobEffect, MobEffect> ZOMBIFICATION = EFFECTS.register("zombification", () -> new ZombificationEffect(MobEffectCategory.HARMFUL, 0x748073));
    public static final DeferredHolder<MobEffect, MobEffect> ZOMBIFICATION_IMMUNITY = EFFECTS.register("zombification_immunity", () -> new ZombificationImmunityEffect(MobEffectCategory.BENEFICIAL, 0xD63B3B));

    public static void register(IEventBus eventBus) {
		EFFECTS.register(eventBus);
	}
}
