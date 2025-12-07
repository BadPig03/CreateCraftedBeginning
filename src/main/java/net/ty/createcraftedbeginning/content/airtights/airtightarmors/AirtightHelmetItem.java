package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterClientUtils;
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
    public boolean isEnderMask(@NotNull ItemStack helmet, @NotNull Player player, @NotNull EnderMan endermanEntity) {
        return helmet.is(CCBItems.AIRTIGHT_HELMET);
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack helmet) {
        return false;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack helmet) {
        return GasCanisterClientUtils.isBarVisible();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack helmet) {
        return GasCanisterClientUtils.getBarWidth();
    }

    @Override
    public int getBarColor(@NotNull ItemStack helmet) {
        return GasCanisterClientUtils.getBarColor();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack helmet, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        AirtightArmorsUtils.appendHelmetHoverText(helmet, context, tooltip, tooltipFlag);
    }
}
