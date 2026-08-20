package net.ty.createcraftedbeginning.content.airtights.airtightextendarm;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightExtendArmItem extends Item {
    public AirtightExtendArmItem(Properties properties) {
        super(properties);
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
        GasStack displayedGas = CanisterContainerClients.getDisplayedGasContent();
        if (displayedGas.isEmpty()) {
            return;
        }

        AirtightArmHandler armHandler = AirtightArmHandlerUtils.of(displayedGas.getGasType());

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(displayedGas).add(CCBLang.translate("gui.gas_tools.content")).style(ChatFormatting.GRAY).component());

        float consumptionMultiplier = armHandler.getGasConsumptionMultiplier();
        MutableComponent advancedConsumptionText = tooltipFlag.isAdvanced() ? CCBLang.text(" [x" + GasConsumptions.format(consumptionMultiplier) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.gas_tools.gas_consumption", GasConsumptions.formatPercent(consumptionMultiplier)).add(advancedConsumptionText.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());
        tooltip.add(CCBLang.translate("gui.airtight_extend_arm.block_interaction_range", GasConsumptions.format(armHandler.getIncreasedBlockInteractionRange())).style(ChatFormatting.DARK_GREEN).component());
        tooltip.add(CCBLang.translate("gui.airtight_extend_arm.entity_interaction_range", GasConsumptions.format(armHandler.getIncreasedEntityInteractionRange())).style(ChatFormatting.DARK_GREEN).component());
        tooltip.add(CCBLang.translate("gui.airtight_extend_arm.attack_knockback", GasConsumptions.format(armHandler.getIncreasedKnockback())).style(ChatFormatting.DARK_GREEN).component());
    }
}
