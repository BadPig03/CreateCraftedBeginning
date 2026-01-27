package net.ty.createcraftedbeginning.content.airtights.airtightextendarm;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerClients;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AirtightExtendArmItem extends Item {
    public AirtightExtendArmItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack arm) {
        return false;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack arm) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack arm) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(@NotNull ItemStack arm) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack arm, @NotNull ItemStack repair) {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack arm, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        AirtightExtendArmUtils.appendHoverText(arm, context, tooltip, tooltipFlag);
    }
}
