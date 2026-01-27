package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments.Mutable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerClients;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AirtightBootsItem extends AirtightBaseArmorItem {
    public static final ResourceLocation MOVEMENT_EFFICIENCY_ID = CreateCraftedBeginning.asResource("movement_efficiency");
    public static final AttributeModifier MOVEMENT_EFFICIENCY_MODIFIER = new AttributeModifier(MOVEMENT_EFFICIENCY_ID, 1, Operation.ADD_VALUE);

    public AirtightBootsItem(@NotNull Properties properties) {
        super(Type.BOOTS, properties);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack boots, @NotNull Holder<Enchantment> enchantment) {
        return enchantment.is(EnchantmentTags.ARMOR_EXCLUSIVE) || enchantment.is(EnchantmentTags.CURSE);
    }

    @Override
    public int getEnchantmentLevel(@NotNull ItemStack boots, @NotNull Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.DEPTH_STRIDER)) {
            return 3;
        }
        return super.getEnchantmentLevel(boots, enchantment);
    }

    @Override
    public @NotNull ItemEnchantments getAllEnchantments(@NotNull ItemStack boots, @NotNull RegistryLookup<Enchantment> lookup) {
        Mutable enchants = new Mutable(super.getAllEnchantments(boots, lookup));
        enchants.set(lookup.getOrThrow(Enchantments.DEPTH_STRIDER), 3);
        return enchants.toImmutable();
    }

    @Override
    public boolean canWalkOnPowderedSnow(@NotNull ItemStack boots, @NotNull LivingEntity wearer) {
        return wearer instanceof Player && boots.is(CCBItems.AIRTIGHT_BOOTS);
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack boots) {
        return false;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack boots) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack boots) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(@NotNull ItemStack boots) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack boots, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        AirtightArmorsUtils.appendBootsHoverText(boots, context, tooltip, tooltipFlag);
    }
}
