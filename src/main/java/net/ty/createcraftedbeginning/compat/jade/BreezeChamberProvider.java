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

public enum BreezeChamberProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String COMPOUND_KEY_WIND_LEVEL = "WindLevel";
    private static final String COMPOUND_KEY_WIND_TIME_REMAINING = "WindTimeRemaining";
    private static final String COMPOUND_KEY_IS_CREATIVE = "IsCreative";

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
    }

    @Override
    public void appendServerData(@NotNull CompoundTag compoundTag, @NotNull BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof BreezeChamberBlockEntity chamber)) {
            return;
        }

        compoundTag.putInt(COMPOUND_KEY_WIND_LEVEL, chamber.getWindLevelFromBlock().ordinal());
        compoundTag.putInt(COMPOUND_KEY_WIND_TIME_REMAINING, chamber.getWindRemainingTime());
        compoundTag.putBoolean(COMPOUND_KEY_IS_CREATIVE, chamber.isCreative());
    }

    @Override
    public @NotNull ResourceLocation getUid() {
        return CCBBlockEntities.BREEZE_CHAMBER.getId();
    }
}
