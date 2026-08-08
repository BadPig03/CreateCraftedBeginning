package net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ethereal;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.data.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizedEtherealAirCannonHandler extends EtherealAirCannonHandler {
    /**
     * {@inheritDoc}
     */
    @Override
    public ItemStack getRenderIcon(Level level) {
        ItemStack icon = super.getRenderIcon(level);
        RegistryLookup<Enchantment> enchantmentRegistryLookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        icon.enchant(enchantmentRegistryLookup.getOrThrow(Enchantments.MENDING), 1);
        return icon;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getGasConsumptionMultiplier() {
        return 0.37f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(CCBLang.translate("gui.airtight_cannon.pressurized_ethereal_air").style(ChatFormatting.DARK_GREEN).component());
    }

    @Override
    protected void applyAdditionalEffects(Level level, List<LivingEntity> entities, DamageSource explosionDamageSource, AirtightCannonShotContext context) {
        super.applyAdditionalEffects(level, entities, explosionDamageSource, context);
        addLevitation(entities, DEFAULT_DURATION * 2, 1, context.effectMultiplier());
    }
}
