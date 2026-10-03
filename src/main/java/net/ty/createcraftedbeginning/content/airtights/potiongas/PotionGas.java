package net.ty.createcraftedbeginning.content.airtights.potiongas;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasBuilder;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Iterator;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PotionGas extends Gas {
    public static final long GAS_PER_APPLICATION = 20;
    public static final long INSTANT_GAS_PER_APPLICATION = 100;
    public static final int INSTANT_COOLDOWN_TICKS = 40;
    public static final int MAX_TARGETS = 4;
    public static final int EFFECT_DURATION = 60;
    private static final int MAX_TRANSLATED_AMPLIFIER = 5;

    public PotionGas(GasBuilder builder) {
        super(builder);
    }

    public static PotionContents getContents(GasStack stack) {
        if (!(stack.getGasType() instanceof PotionGas)) {
            return PotionContents.EMPTY;
        }

        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    }

    public static @Nullable MobEffectInstance findReleaseEffect(GasStack stack) {
        return findReleaseEffect(getContents(stack));
    }

    public static @Nullable MobEffectInstance findReleaseEffect(PotionContents contents) {
        Iterator<MobEffectInstance> effects = contents.getAllEffects().iterator();
        if (!effects.hasNext()) {
            return null;
        }

        MobEffectInstance effect = effects.next();
        if (effects.hasNext() || !isSupportedEffect(effect)) {
            return null;
        }

        return effect;
    }

    public static boolean isSupportedEffect(MobEffectInstance effect) {
        return effect.getAmplifier() >= 0 && (effect.getEffect().value().isInstantenous() || !effect.isInfiniteDuration() && effect.getDuration() > 0);
    }

    @Override
    public int getTint(GasStack stack) {
        PotionContents contents = getContents(stack);
        if (contents.equals(PotionContents.EMPTY)) {
            return super.getTint(stack);
        }

        return ARGB32.color(ARGB32.alpha(getTint()), contents.getColor());
    }

    @Override
    public Component getName(GasStack stack) {
        MutableComponent effects = Component.empty();
        boolean first = true;
        for (MobEffectInstance effect : getContents(stack).getAllEffects()) {
            if (!first) {
                effects.append(Component.translatable("createcraftedbeginning.potion_gas.effect_separator"));
            }

            first = false;
            MutableComponent name = Component.translatable(effect.getDescriptionId());
            int amplifier = effect.getAmplifier();
            if (amplifier > MAX_TRANSLATED_AMPLIFIER) {
                name.append(" ").append(String.valueOf((long) amplifier + 1));
            }
            else if (amplifier > 0) {
                name.append(" ").append(Component.translatable("potion.potency." + amplifier));
            }

            effects.append(name);
        }

        if (first) {
            return super.getName(stack);
        }

        return Component.translatable("createcraftedbeginning.potion_gas.named", super.getName(stack), effects);
    }

    @Override
    public List<Component> getTooltip(GasStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }

        if (!getContents(stack).hasEffects()) {
            return List.of(CCBLang.translateDirect("potion_gas.no_effect").withStyle(ChatFormatting.GRAY));
        }

        if (findReleaseEffect(stack) == null) {
            return List.of(CCBLang.translateDirect("potion_gas.unsupported_effect").withStyle(ChatFormatting.RED));
        }

        return List.of();
    }

    public GasStack createStack(long amount, PotionContents contents) {
        if (amount <= 0) {
            return GasStack.EMPTY;
        }

        GasStack stack = new GasStack(this, amount);
        stack.set(DataComponents.POTION_CONTENTS, contents);
        return stack;
    }
}
