package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.data.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionChamberFilterItem extends Item {
    public GasInjectionChamberFilterItem(Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltips, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltips, flag);
        Optional<ResourceLocation> optionalTypeId = GasInjectionChamberUtils.getFanProcessingTypeId(stack);
        tooltips.add(CCBLang.translateDirect("gui.gas_injection_chamber_filter.processing_type", optionalTypeId.map(location -> GasInjectionChamberUtils.getFanProcessingTypeName(location).copy().withStyle(ChatFormatting.AQUA)).orElseGet(() -> Component.translatable("fan_processing_type.empty").withStyle(ChatFormatting.GRAY))).withStyle(ChatFormatting.GRAY));
    }
}
