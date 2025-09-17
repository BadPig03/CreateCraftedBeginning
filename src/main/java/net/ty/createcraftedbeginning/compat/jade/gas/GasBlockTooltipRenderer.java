package net.ty.createcraftedbeginning.compat.jade.gas;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.util.Helpers;
import org.jetbrains.annotations.NotNull;
import snownee.jade.api.Accessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IElementHelper;

public class GasBlockTooltipRenderer<ACCESSOR extends Accessor<?>> implements IComponentProvider<ACCESSOR> {
    public static final GasBlockTooltipRenderer<?> INSTANCE = new GasBlockTooltipRenderer<>();



    @Override
    public ResourceLocation getUid() {
        return GasConstants.TOOLTIP_RENDERER;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, @NotNull ACCESSOR accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();

        if (data.contains("windLevel") && data.contains("windTimeRemaining") && data.contains("isCreative")) {
            WindLevel windLevel = WindLevel.values()[data.getInt("windLevel")];
            int windTimeRemaining = Mth.abs(data.getInt("windTimeRemaining"));
            boolean isCreative = data.getBoolean("isCreative");

            IElementHelper helper = IElementHelper.get();
            float tickRate = Helpers.getActualTickRate(accessor.getLevel());

            if (windLevel == WindLevel.ILL) {
                tooltip.add(helper.smallItem(new ItemStack(Items.POISONOUS_POTATO)));
                if (isCreative) {
                    tooltip.append(IThemeHelper.get().info(CCBLang.translateDirect("gui.goggles.infinity_mark")));
                } else {
                    tooltip.append(IThemeHelper.get().seconds(windTimeRemaining, tickRate).withStyle(ChatFormatting.RED));
                }
            } else if (windLevel.isAtLeast(WindLevel.BREEZE)) {
                tooltip.add(helper.smallItem(new ItemStack(Items.WIND_CHARGE)));
                if (isCreative) {
                    tooltip.append(IThemeHelper.get().info(CCBLang.translateDirect("gui.goggles.infinity_mark")));
                } else {
                    tooltip.append(IThemeHelper.get().seconds(windTimeRemaining, tickRate));
                }
            }
        }

        if (!data.contains(GasConstants.STORAGE_KEY) || !data.contains(GasConstants.STORAGE_UID_KEY)) {
            return;
        }

        String uidString = data.getString(GasConstants.STORAGE_UID_KEY);
        if (!GasConstants.TOOLTIP_RENDERER.toString().equals(uidString)) {
            return;
        }

        boolean showDetails = accessor.showDetails();
        DataProviderHelper.appendData(tooltip, data, showDetails);
    }
}