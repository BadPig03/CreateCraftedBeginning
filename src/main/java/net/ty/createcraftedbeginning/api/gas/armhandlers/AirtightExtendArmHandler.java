package net.ty.createcraftedbeginning.api.gas.armhandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface AirtightExtendArmHandler {
    SimpleRegistry<Gas, AirtightExtendArmHandler> REGISTRY = SimpleRegistry.create();

    float getGasConsumptionMultiplier();

    float getIncreasedBlockInteractionRange();

    float getIncreasedEntityInteractionRange();

    float getIncreasedKnockback();

    default String getRenderStr(float n) {
        return String.format("%.2f", n).replaceAll("\\.?0+$", "");
    }

    default void appendHoverText(@NotNull ItemStack arm, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        float consumptionMultiplier = getGasConsumptionMultiplier();
        MutableComponent advancedConsumptionMultiplier = flag.isAdvanced() ? CCBLang.text(" [x" + getRenderStr(consumptionMultiplier) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.gas_tools.gas_consumption", getRenderStr(consumptionMultiplier * 100)).add(advancedConsumptionMultiplier.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());

        String blockRangeRenderStr = getRenderStr(getIncreasedBlockInteractionRange());
        MutableComponent advancedBlockRange = flag.isAdvanced() ? CCBLang.text(" [+" + blockRangeRenderStr + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.airtight_extend_arm.block_interaction_range", blockRangeRenderStr).add(advancedBlockRange.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());

        String entityRangeRenderStr = getRenderStr(getIncreasedEntityInteractionRange());
        MutableComponent advancedEntityRange = flag.isAdvanced() ? CCBLang.text(" [+" + entityRangeRenderStr + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.airtight_extend_arm.entity_interaction_range", entityRangeRenderStr).add(advancedEntityRange.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());

        String knockbackRenderStr = getRenderStr(getIncreasedKnockback());
        MutableComponent advancedKnockback = flag.isAdvanced() ? CCBLang.text(" [+" + knockbackRenderStr + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.airtight_extend_arm.attack_knockback", knockbackRenderStr).add(advancedKnockback.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());
    }
}
