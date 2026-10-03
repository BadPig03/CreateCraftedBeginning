package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionChamberFilterItem extends Item {
    public GasInjectionChamberFilterItem(Properties properties) {
        super(properties);
    }

    public static boolean isFilter(ItemStack stack) {
        return stack.is(CCBItems.GAS_INJECTION_CHAMBER_FILTER.get());
    }

    public static Optional<ResourceLocation> getFanProcessingTypeId(ItemStack stack) {
        if (!isFilter(stack)) {
            return Optional.empty();
        }

        return Optional.ofNullable(stack.get(CCBDataComponents.GAS_INJECTION_CHAMBER_FILTER_FAN_PROCESSING_TYPE));
    }

    public static Optional<FanProcessingType> getFanProcessingType(ResourceLocation typeId) {
        return Optional.ofNullable(CreateBuiltInRegistries.FAN_PROCESSING_TYPE.get(typeId));
    }

    public static ItemStack create(ItemStack input, FanProcessingType type) {
        ItemStack filterStack = input.copy();
        ResourceLocation typeId = CreateBuiltInRegistries.FAN_PROCESSING_TYPE.getKey(type);
        if (typeId == null) {
            filterStack.remove(CCBDataComponents.GAS_INJECTION_CHAMBER_FILTER_FAN_PROCESSING_TYPE);
            return filterStack;
        }

        filterStack.set(CCBDataComponents.GAS_INJECTION_CHAMBER_FILTER_FAN_PROCESSING_TYPE, typeId);
        int filterColor = GasInjectionFilterColors.getColor(typeId, type);
        filterStack.set(CCBDataComponents.GAS_INJECTION_CHAMBER_FILTER_COLOR, filterColor);
        return filterStack;
    }

    static int getColor(ItemStack stack) {
        if (!isFilter(stack)) {
            return GasInjectionFilterColors.DEFAULT_COLOR;
        }

        return stack.getOrDefault(CCBDataComponents.GAS_INJECTION_CHAMBER_FILTER_COLOR, GasInjectionFilterColors.DEFAULT_COLOR);
    }

    static Component getFanProcessingTypeName(ResourceLocation typeId) {
        return Component.translatableWithFallback("fan_processing_type." + typeId.getNamespace() + '.' + typeId.getPath().replace('/', '.'), typeId.toString());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltips, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltips, flag);
        Optional<ResourceLocation> fanProcessingTypeId = getFanProcessingTypeId(stack);
        tooltips.add(CCBLang.translateDirect("gui.gas_injection_chamber_filter.processing_type", fanProcessingTypeId.map(typeId -> getFanProcessingTypeName(typeId).copy().withStyle(ChatFormatting.AQUA)).orElseGet(() -> Component.translatable("fan_processing_type.empty").withStyle(ChatFormatting.GRAY))).withStyle(ChatFormatting.GRAY));
    }
}
