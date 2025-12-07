package net.ty.createcraftedbeginning.api.gas.armorhandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("unused")
public interface AirtightArmorsHandler {
    SimpleRegistry<Gas, AirtightArmorsHandler> REGISTRY = SimpleRegistry.create();

    boolean canCureEffect(@NotNull MobEffectInstance effectInstance);

    float[] getConsumptionMultiplier();

    float getMultiplierForBoostingElytra();

    default String getRenderStr(float n) {
        return String.format("%.2f", n).replaceAll("\\.?0+$", "");
    }

    default void appendHelmetHoverText(ItemStack helmet, TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (canCureEffect(new MobEffectInstance(MobEffects.GLOWING))) {
            tooltip.add(CCBLang.translate("gui.tooltips.airtight_armors.natural_effect").style(ChatFormatting.DARK_GREEN).component());
        }

        float consumptionMultiplier = getConsumptionMultiplier()[0];
        MutableComponent advancedConsumptionMultiplier = flag.isAdvanced() ? CCBLang.text(" [x" + getRenderStr(consumptionMultiplier) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.gas_tools.gas_consumption", getRenderStr(consumptionMultiplier * 100)).add(advancedConsumptionMultiplier.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());
    }

    default void appendChestplateHoverText(ItemStack helmet, TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        float consumptionMultiplier = getConsumptionMultiplier()[1];
        MutableComponent advancedConsumptionMultiplier = flag.isAdvanced() ? CCBLang.text(" [x" + getRenderStr(consumptionMultiplier) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.gas_tools.gas_consumption", getRenderStr(consumptionMultiplier * 100)).add(advancedConsumptionMultiplier.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());

        float elytraMultiplier = getMultiplierForBoostingElytra();
        MutableComponent advancedElytraMultiplier = flag.isAdvanced() ? CCBLang.text(" [x" + getRenderStr(elytraMultiplier) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.airtight_armors.elytra_boost", getRenderStr(elytraMultiplier * 100)).add(advancedElytraMultiplier.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());
    }

    default void appendLeggingsHoverText(ItemStack leggings, TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        float consumptionMultiplier = getConsumptionMultiplier()[2];
        MutableComponent advancedConsumptionMultiplier = flag.isAdvanced() ? CCBLang.text(" [x" + getRenderStr(consumptionMultiplier) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.gas_tools.gas_consumption", getRenderStr(consumptionMultiplier * 100)).add(advancedConsumptionMultiplier.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());
    }

    default void appendBootsHoverText(ItemStack boots, TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        float consumptionMultiplier = getConsumptionMultiplier()[3];
        MutableComponent advancedConsumptionMultiplier = flag.isAdvanced() ? CCBLang.text(" [x" + getRenderStr(consumptionMultiplier) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.gas_tools.gas_consumption", getRenderStr(consumptionMultiplier * 100)).add(advancedConsumptionMultiplier.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());
    }
}
