package net.ty.createcraftedbeginning.api.drillhandlers.ultrawarm;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandler;
import net.ty.createcraftedbeginning.data.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UltrawarmAirDrillHandler implements AirtightDrillHandler {
    protected static final int BASE_BURNING_TIME = 4;

    @Override
    public int getDamageAddition() {
        return 0;
    }

    @Override
    public float getConsumptionMultiplier() {
        return 0.9f;
    }

    @Override
    public void extraBehaviour(LivingEntity entity, Player player, ServerLevel serverLevel) {
        AirtightDrillHandler.super.extraBehaviour(entity, player, serverLevel);
        entity.igniteForSeconds(4);
    }

    @Override
    public void appendHoverText(ItemStack drill, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        AirtightDrillHandler.super.appendHoverText(drill, context, tooltip, flag);
        MutableComponent advancedBurningTime = flag.isAdvanced() ? CCBLang.text(" [+" + BASE_BURNING_TIME + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.airtight_handheld_drill.burning_time", BASE_BURNING_TIME).add(advancedBurningTime.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());
    }
}
