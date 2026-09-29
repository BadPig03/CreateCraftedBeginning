package net.ty.createcraftedbeginning.content.airtights.airtightcannon;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map.Entry;
import java.util.OptionalDouble;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightCannonCharge {
    private static final int EFFICIENT_USE_TIME = 15;
    private static final float MIN_CHARGED_RATIO = 0.33333334F;

    private AirtightCannonCharge() {
    }

    static OptionalDouble getChargedRatio(ItemStack cannon, int timeCharged) {
        int efficientUseTime = getEfficientUseTime(cannon);
        int minimumUseTime = Math.max(Mth.ceil(efficientUseTime * MIN_CHARGED_RATIO), 1);
        if (timeCharged < minimumUseTime) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(Mth.clamp((float) timeCharged / efficientUseTime, 0.0F, 2));
    }

    static int getEfficientUseTime(ItemStack cannon) {
        int quickChargeLevel = getEnchantmentLevel(cannon, Enchantments.QUICK_CHARGE);
        return Math.max(EFFICIENT_USE_TIME - quickChargeLevel * 3, 1);
    }

    static int getEnchantmentLevel(ItemStack cannon, ResourceKey<Enchantment> enchantment) {
        if (!cannon.is(CCBItems.AIRTIGHT_CANNON)) {
            return 0;
        }

        return cannon.getTagEnchantments().entrySet().stream().filter(entry -> entry.getKey().is(enchantment)).findFirst().map(Entry::getValue).orElse(0);
    }
}
