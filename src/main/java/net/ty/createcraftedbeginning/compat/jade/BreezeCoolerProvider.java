package net.ty.createcraftedbeginning.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IElementHelper;

public enum BreezeCoolerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ItemStack ICON = new ItemStack(Items.SNOWBALL);

    private static final String COMPOUND_KEY_FROST_LEVEL = "FrostLevel";
    private static final String COMPOUND_KEY_COOL_TIME_REMAINING = "CoolTimeRemaining";
    private static final String COMPOUND_KEY_IS_CREATIVE = "IsCreative";

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendTooltip(ITooltip tooltip, @NotNull BlockAccessor accessor, IPluginConfig config) {
        CompoundTag compoundTag = accessor.getServerData();
        FrostLevel frostLevel = FrostLevel.values()[compoundTag.getInt(COMPOUND_KEY_FROST_LEVEL)];
        int coolTimeRemaining = compoundTag.getInt(COMPOUND_KEY_COOL_TIME_REMAINING);
        if (!frostLevel.isAtLeast(FrostLevel.CHILLED) || coolTimeRemaining == 0) {
            return;
        }

        tooltip.add(IElementHelper.get().smallItem(ICON));
        boolean isCreative = compoundTag.getBoolean(COMPOUND_KEY_IS_CREATIVE);
        tooltip.append(isCreative ? IThemeHelper.get().info(Component.translatable("jade.gas.infinity_mark")) : IThemeHelper.get().seconds(coolTimeRemaining, accessor.tickRate()));
    }

    @Override
    public void appendServerData(@NotNull CompoundTag data, @NotNull BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof BreezeCoolerBlockEntity cooler)) {
            return;
        }

        data.putInt(COMPOUND_KEY_FROST_LEVEL, cooler.getFrostLevelFromBlock().ordinal());
        data.putInt(COMPOUND_KEY_COOL_TIME_REMAINING, cooler.getCoolRemainingTime());
        data.putBoolean(COMPOUND_KEY_IS_CREATIVE, cooler.isCreative());
    }

    @Override
    public @NotNull ResourceLocation getUid() {
        return CCBBlockEntities.BREEZE_COOLER.getId();
    }
}
