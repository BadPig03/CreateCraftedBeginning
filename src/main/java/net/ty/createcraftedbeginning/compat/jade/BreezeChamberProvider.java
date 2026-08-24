package net.ty.createcraftedbeginning.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IElementHelper;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum BreezeChamberProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ItemStack ILL_ICON = new ItemStack(Items.POISONOUS_POTATO);
    private static final ItemStack GALE_ICON = new ItemStack(Items.WIND_CHARGE);

    private static final String COMPOUND_KEY_WIND_LEVEL = "WindLevel";
    private static final String COMPOUND_KEY_WIND_TIME_REMAINING = "WindTimeRemaining";
    private static final String COMPOUND_KEY_IS_CREATIVE = "IsCreative";

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (!serverData.contains(COMPOUND_KEY_WIND_LEVEL) || !serverData.contains(COMPOUND_KEY_WIND_TIME_REMAINING) || !serverData.contains(COMPOUND_KEY_IS_CREATIVE)) {
            return;
        }

        IElementHelper elementHelper = IElementHelper.get();
        float tickRate = accessor.tickRate();
        int remainingTicks = Mth.abs(serverData.getInt(COMPOUND_KEY_WIND_TIME_REMAINING));
        WindLevel windLevel = WindLevel.values()[serverData.getInt(COMPOUND_KEY_WIND_LEVEL)];
        boolean isCreative = serverData.getBoolean(COMPOUND_KEY_IS_CREATIVE);
        if (windLevel == WindLevel.ILL) {
            tooltip.add(elementHelper.smallItem(ILL_ICON));
            tooltip.append(isCreative ? IThemeHelper.get().info(Component.translatable("jade.gas.infinity_mark")) : IThemeHelper.get().seconds(remainingTicks, tickRate).withStyle(ChatFormatting.RED));
            return;
        }

        if (windLevel != WindLevel.GALE) {
            return;
        }

        tooltip.add(elementHelper.smallItem(GALE_ICON));
        tooltip.append(isCreative ? IThemeHelper.get().info(Component.translatable("jade.gas.infinity_mark")) : IThemeHelper.get().seconds(remainingTicks, tickRate));
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof BreezeChamberBlockEntity chamber)) {
            return;
        }

        compoundTag.putInt(COMPOUND_KEY_WIND_LEVEL, chamber.getWindLevelFromBlock().ordinal());
        compoundTag.putInt(COMPOUND_KEY_WIND_TIME_REMAINING, chamber.getWindRemainingTime());
        compoundTag.putBoolean(COMPOUND_KEY_IS_CREATIVE, chamber.isCreative());
    }

    @Override
    public ResourceLocation getUid() {
        return CCBBlockEntities.BREEZE_CHAMBER.getId();
    }
}
