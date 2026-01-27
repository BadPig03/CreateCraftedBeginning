package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments.Mutable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerClients;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AirtightHelmetItem extends AirtightBaseArmorItem {
    static {
        GogglesItem.addIsWearingPredicate(player -> CCBConfig.client().enableHelmetGoggles.get() && CCBItems.AIRTIGHT_HELMET.isIn(player.getItemBySlot(EquipmentSlot.HEAD)));
    }

    public AirtightHelmetItem(@NotNull Properties properties) {
        super(Type.HELMET, properties);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack helmet, @NotNull Holder<Enchantment> enchantment) {
        return enchantment.is(EnchantmentTags.ARMOR_EXCLUSIVE) || enchantment.is(EnchantmentTags.CURSE);
    }

    @Override
	public int getEnchantmentLevel(@NotNull ItemStack helmet, @NotNull Holder<Enchantment> enchantment) {
		if (enchantment.is(Enchantments.AQUA_AFFINITY)) {
            return 1;
        }
		return super.getEnchantmentLevel(helmet, enchantment);
	}

    @Override
	public @NotNull ItemEnchantments getAllEnchantments(@NotNull ItemStack helmet, @NotNull RegistryLookup<Enchantment> lookup) {
		Mutable enchants = new Mutable(super.getAllEnchantments(helmet, lookup));
		enchants.set(lookup.getOrThrow(Enchantments.AQUA_AFFINITY), 1);
		return enchants.toImmutable();
	}

    @Override
    public boolean isEnderMask(@NotNull ItemStack helmet, @NotNull Player player, @NotNull EnderMan endermanEntity) {
        return helmet.is(CCBItems.AIRTIGHT_HELMET);
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack helmet) {
        return false;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack helmet) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack helmet) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(@NotNull ItemStack helmet) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack helmet, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        AirtightArmorsUtils.appendHelmetHoverText(helmet, context, tooltip, tooltipFlag);
    }
}
