package net.ty.createcraftedbeginning.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.util.Helpers;
import org.jetbrains.annotations.NotNull;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IElementHelper;

public enum BreezeCoolerComponentProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendTooltip(ITooltip tooltip, @NotNull BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();

        FrostLevel frostLevel = FrostLevel.values()[data.getInt("frostLevel")];
        int coolTimeRemaining = data.getInt("coolTimeRemaining");
        boolean isCreative = data.getBoolean("isCreative");

        if (!frostLevel.isAtLeast(FrostLevel.WANING)) {
            return;
        }

        ItemStack item = new ItemStack(Items.SNOWBALL);
        tooltip.add(IElementHelper.get().smallItem(item));
        if (isCreative) {
			tooltip.append(IThemeHelper.get().info(CCBLang.translateDirect("gui.goggles.infinity_mark")));
		} else {
            tooltip.append(IThemeHelper.get().seconds(coolTimeRemaining, Helpers.getActualTickRate(accessor.getLevel())));
        }
    }

    @Override
    public void appendServerData(@NotNull CompoundTag data, @NotNull BlockAccessor accessor) {
        BreezeCoolerBlockEntity bcbe = (BreezeCoolerBlockEntity) accessor.getBlockEntity();
        data.putInt("frostLevel", bcbe.getFrostLevelFromBlock().ordinal());
        data.putInt("coolTimeRemaining", bcbe.getCoolRemainingTime());
        data.putBoolean("isCreative", bcbe.isCreative);
    }

    @Override
    public @NotNull ResourceLocation getUid() {
        return CCBBlockEntities.BREEZE_COOLER.getId();
    }
}
