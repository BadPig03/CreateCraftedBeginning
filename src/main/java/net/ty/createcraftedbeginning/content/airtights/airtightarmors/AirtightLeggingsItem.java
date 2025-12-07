package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterClientUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AirtightLeggingsItem extends AirtightBaseArmorItem {
    public AirtightLeggingsItem(@NotNull Properties properties) {
        super(Type.LEGGINGS, properties);
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack leggings) {
        return false;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack leggings) {
        return GasCanisterClientUtils.isBarVisible();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack leggings) {
        return GasCanisterClientUtils.getBarWidth();
    }

    @Override
    public int getBarColor(@NotNull ItemStack leggings) {
        return GasCanisterClientUtils.getBarColor();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack leggings, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        AirtightArmorsUtils.appendLeggingsHoverText(leggings, context, tooltip, tooltipFlag);
    }
}
