package net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity;
import org.jetbrains.annotations.NotNull;

public class SevereOverheatState implements IOverheatState {
    private static final String NAME = "severe";

    @Override
    public IOverheatState getNextState() {
        return OverheatManager.MELTDOWN;
    }

    @Override
    public IOverheatState getPreviousState() {
        return OverheatManager.MODERATE;
    }

    @Override
    public void tick(@NotNull AirCompressorBlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();
        if (level == null || !level.isClientSide || level.getGameTime() % 2 != 0) {
            return;
        }

        spawnParticles(level, pos, 0.1f, 0.3f);
    }

    @Override
    public String getSerializedName() {
        return NAME;
    }

    @Override
    public ChatFormatting getDisplayColor() {
        return ChatFormatting.RED;
    }

    @Override
    public float getEfficiency() {
        return 0.2f;
    }

    @Override
    public String getTranslationKey() {
        return "gui.goggles.air_compressor.severe";
    }

    @Override
    public int getAnalogOutputSignal() {
        return 15;
    }

    @Override
    public void spawnParticlesInPonderLevel(@NotNull PonderLevel level, BlockPos pos, int tick) {
        spawnParticles(level, pos, 0.1f, 0.3f);
    }
}
