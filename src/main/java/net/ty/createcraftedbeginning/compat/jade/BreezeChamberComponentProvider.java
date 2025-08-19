package net.ty.createcraftedbeginning.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IElementHelper;

public enum BreezeChamberComponentProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();

        FrostLevel frostLevel = FrostLevel.values()[data.getInt("frostLevel")];
        boolean isCreative = data.getBoolean("isCreative");
        int coolTimeRemaining = data.getInt("coolTimeRemaining");

        if (!frostLevel.isAtLeast(FrostLevel.WANING)) {
            return;
        }

        ItemStack item = new ItemStack(frostLevel.isAtLeast(FrostLevel.GALLING) ? Items.POWDER_SNOW_BUCKET : Items.SNOWBALL);
        tooltip.add(IElementHelper.get().smallItem(item));

        if (isCreative) {
            tooltip.append(IThemeHelper.get().info(Component.translatable("jade.infinity")));
        } else {
            tooltip.append(IThemeHelper.get().seconds(coolTimeRemaining, 20));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BreezeChamberBlockEntity bcbe = (BreezeChamberBlockEntity) accessor.getBlockEntity();
        data.putBoolean("isCreative", bcbe.isCreative());
        data.putInt("frostLevel", bcbe.getFrostLevelFromBlock().ordinal());
        data.putInt("coolTimeRemaining", bcbe.getCoolRemainingTime());
    }

    @Override
    public ResourceLocation getUid() {
        return CCBBlockEntities.BREEZE_CHAMBER.getId();
    }
}
