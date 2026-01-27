package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AirtightLeggingsItem extends AirtightBaseArmorItem {
    public static final ResourceLocation SNEAKING_SPEED_ID = CreateCraftedBeginning.asResource("sneaking_speed");
    public static final AttributeModifier SNEAKING_SPEED_MODIFIER = new AttributeModifier(SNEAKING_SPEED_ID, 1, Operation.ADD_VALUE);

    public AirtightLeggingsItem(@NotNull Properties properties) {
        super(Type.LEGGINGS, properties);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack leggings, @NotNull Holder<Enchantment> enchantment) {
        return enchantment.is(EnchantmentTags.ARMOR_EXCLUSIVE) || enchantment.is(EnchantmentTags.CURSE);
    }

    @Override
    public int getEnchantmentLevel(@NotNull ItemStack leggings, @NotNull Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.SWIFT_SNEAK)) {
            return 3;
        }
        return super.getEnchantmentLevel(leggings, enchantment);
    }

    @Override
    public @NotNull ItemEnchantments getAllEnchantments(@NotNull ItemStack leggings, @NotNull RegistryLookup<Enchantment> lookup) {
        Mutable enchants = new Mutable(super.getAllEnchantments(leggings, lookup));
        enchants.set(lookup.getOrThrow(Enchantments.SWIFT_SNEAK), 3);
        return enchants.toImmutable();
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack leggings) {
        return false;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack leggings) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack leggings) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(@NotNull ItemStack leggings) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack leggings, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        AirtightArmorsUtils.appendLeggingsHoverText(leggings, context, tooltip, tooltipFlag);
    }
}
