package net.ty.createcraftedbeginning.content.airtights.handlers.cannon.natural;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedNaturalAirCannonHandler extends NaturalAirCannonHandler {
    @Override
    public ItemStack getRenderIcon(Level level) {
        ItemStack icon = super.getRenderIcon(level);
        RegistryLookup<Enchantment> enchantmentRegistryLookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        icon.enchant(enchantmentRegistryLookup.getOrThrow(Enchantments.MENDING), 1);
        return icon;
    }

    @Override
    public float getGasConsumptionMultiplier() {
        return 0.65f;
    }

    @Override
    public void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(CCBLang.translate("gui.airtight_cannon.pressurized_natural_air").style(ChatFormatting.DARK_GREEN).component());
    }

    @Override
    protected boolean hasAdditionalEffects() {
        return true;
    }

    @Override
    protected void applyAdditionalEffects(Level level, List<LivingEntity> entities, DamageSource explosionDamageSource, AirtightCannonShotContext context) {
        int duration = Math.round(DEFAULT_DURATION * context.effectMultiplier());
        for (LivingEntity entity : entities) {
            entity.addEffect(new MobEffectInstance(MobEffects.WIND_CHARGED, duration, 0));
        }
    }
}
