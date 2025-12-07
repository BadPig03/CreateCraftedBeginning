package net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity;
import org.jetbrains.annotations.NotNull;

public class MeltdownOverheatState implements IOverheatState {
    private static final String NAME = "meltdown";

    @Override
    public IOverheatState getNextState() {
        return this;
    }

    @Override
    public IOverheatState getPreviousState() {
        return OverheatManager.SEVERE;
    }

    @Override
    public void tick(@NotNull AirCompressorBlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();
        if (level == null || level.isClientSide) {
            return;
        }

        level.destroyBlock(pos, false);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8.0f, true, ExplosionInteraction.NONE);
    }

    @Override
    public String getSerializedName() {
        return NAME;
    }

    @Override
    public ChatFormatting getDisplayColor() {
        return ChatFormatting.DARK_RED;
    }

    @Override
    public float getEfficiency() {
        return 0;
    }

    @Override
    public String getTranslationKey() {
        return "gui.goggles.air_compressor.meltdown";
    }

    @Override
    public int getAnalogOutputSignal() {
        return 15;
    }

    @Override
    public void spawnParticlesInPonderLevel(PonderLevel level, BlockPos pos, int tick) {
    }
}
