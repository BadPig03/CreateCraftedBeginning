package net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity;
import org.jetbrains.annotations.NotNull;

public class NormalOverheatState implements IOverheatState {
    private static final String NAME = "normal";

    @Override
    public IOverheatState getNextState() {
        return OverheatManager.SLIGHT;
    }

    @Override
    public IOverheatState getPreviousState() {
        return this;
    }

    @Override
    public void tick(@NotNull AirCompressorBlockEntity blockEntity) {
    }

    @Override
    public String getSerializedName() {
        return NAME;
    }

    @Override
    public ChatFormatting getDisplayColor() {
        return ChatFormatting.AQUA;
    }

    @Override
    public float getEfficiency() {
        return 1.0f;
    }

    @Override
    public String getTranslationKey() {
        return "gui.goggles.air_compressor.normal";
    }

    @Override
    public int getAnalogOutputSignal() {
        return 0;
    }

    @Override
    public void spawnParticlesInPonderLevel(PonderLevel level, BlockPos pos, int tick) {
    }
}
