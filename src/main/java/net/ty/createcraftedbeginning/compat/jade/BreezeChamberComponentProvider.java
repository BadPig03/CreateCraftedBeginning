package net.ty.createcraftedbeginning.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum BreezeChamberComponentProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
    }

    @Override
    public void appendServerData(@NotNull CompoundTag data, @NotNull BlockAccessor accessor) {
        BreezeChamberBlockEntity bcbe = (BreezeChamberBlockEntity) accessor.getBlockEntity();
        data.putInt("windLevel", bcbe.getWindLevelFromBlock().ordinal());
        data.putInt("windTimeRemaining", bcbe.getWindRemainingTime());
        data.putBoolean("isCreative", bcbe.isCreative);
    }

    @Override
    public @NotNull ResourceLocation getUid() {
        return CCBBlockEntities.BREEZE_CHAMBER.getId();
    }
}
