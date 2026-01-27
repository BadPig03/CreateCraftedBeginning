package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.core.Holder;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerClients;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AirtightChestplateItem extends AirtightChestplateArmorItem {
    public static final ResourceLocation CREATIVE_FLIGHT_ID = CreateCraftedBeginning.asResource("creative_flight");
    public static final AttributeModifier CREATIVE_FLIGHT_MODIFIER = new AttributeModifier(CREATIVE_FLIGHT_ID, 1, Operation.ADD_VALUE);

    public AirtightChestplateItem(Properties properties) {
        super(Type.CHESTPLATE, properties);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack chestplate, @NotNull Holder<Enchantment> enchantment) {
        return enchantment.is(EnchantmentTags.ARMOR_EXCLUSIVE) || enchantment.is(EnchantmentTags.CURSE);
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack chestplate) {
        return false;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack chestplate) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack chestplate) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(@NotNull ItemStack chestplate) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack chestplate, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        AirtightArmorsUtils.appendChestplateHoverText(chestplate, context, tooltip, tooltipFlag);
    }

    @Override
    public boolean canElytraFly(@NotNull ItemStack chestplate, @NotNull LivingEntity entity) {
        return entity instanceof Player player && AirtightArmorsUtils.canElytraFlightTick(player, 0);
    }

    @Override
    public boolean elytraFlightTick(@NotNull ItemStack chestplate, @NotNull LivingEntity entity, int flightTicks) {
        return entity instanceof Player player && AirtightArmorsUtils.canElytraFlightTick(player, flightTicks);
    }
}
