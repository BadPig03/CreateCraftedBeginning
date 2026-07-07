package net.ty.createcraftedbeginning.content.airtights.airtightextendarm;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandler;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.data.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightExtendArmItem extends Item {
    public AirtightExtendArmItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isDamageable(ItemStack arm) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack arm) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(ItemStack arm) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(ItemStack arm) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack arm, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return;
        }

        Gas gasType = CanisterContainerSuppliers.getFirstAvailableGasContent(player).getGasType();
        if (gasType.isEmpty()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(gasType).add(CCBLang.translate("gui.tooltips.gas_tools.content")).style(ChatFormatting.GRAY).component());
        AirtightArmHandler armHandler = AirtightArmHandlerUtils.of(gasType);
        float consumptionMultiplier = armHandler.getGasConsumptionMultiplier();
        MutableComponent advancedConsumptionMultiplier = tooltipFlag.isAdvanced() ? CCBLang.text(" [x" + armHandler.getRenderStr(consumptionMultiplier) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.gas_tools.gas_consumption", armHandler.getRenderStr(consumptionMultiplier * 100)).add(advancedConsumptionMultiplier.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());

        String blockRangeRenderStr = armHandler.getRenderStr(armHandler.getIncreasedBlockInteractionRange());
        MutableComponent advancedBlockRange = tooltipFlag.isAdvanced() ? CCBLang.text(" [+" + blockRangeRenderStr + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.airtight_extend_arm.block_interaction_range", blockRangeRenderStr).add(advancedBlockRange.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());

        String entityRangeRenderStr = armHandler.getRenderStr(armHandler.getIncreasedEntityInteractionRange());
        MutableComponent advancedEntityRange = tooltipFlag.isAdvanced() ? CCBLang.text(" [+" + entityRangeRenderStr + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.airtight_extend_arm.entity_interaction_range", entityRangeRenderStr).add(advancedEntityRange.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());

        String knockbackRenderStr = armHandler.getRenderStr(armHandler.getIncreasedKnockback());
        MutableComponent advancedKnockback = tooltipFlag.isAdvanced() ? CCBLang.text(" [+" + knockbackRenderStr + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.airtight_extend_arm.attack_knockback", knockbackRenderStr).add(advancedKnockback.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());
    }

    @Override
    public boolean isValidRepairItem(ItemStack arm, ItemStack repair) {
        return false;
    }
}
