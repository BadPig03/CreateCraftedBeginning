package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterClientUtils;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AirtightBootsItem extends AirtightBaseArmorItem {
    public AirtightBootsItem(@NotNull Properties properties) {
        super(Type.BOOTS, properties);
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack boots) {
        return false;
    }

    @Override
    public boolean canWalkOnPowderedSnow(@NotNull ItemStack boots, @NotNull LivingEntity wearer) {
        return wearer instanceof Player && boots.is(CCBItems.AIRTIGHT_BOOTS);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack boots) {
        return GasCanisterClientUtils.isBarVisible();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack boots) {
        return GasCanisterClientUtils.getBarWidth();
    }

    @Override
    public int getBarColor(@NotNull ItemStack boots) {
        return GasCanisterClientUtils.getBarColor();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack boots, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        AirtightArmorsUtils.appendBootsHoverText(boots, context, tooltip, tooltipFlag);
    }
}
