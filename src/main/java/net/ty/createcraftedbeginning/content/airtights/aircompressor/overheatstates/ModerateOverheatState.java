package net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity;
import org.jetbrains.annotations.NotNull;

public class ModerateOverheatState implements IOverheatState {
    private static final String NAME = "moderate";

    @Override
    public IOverheatState getNextState() {
        return OverheatManager.SEVERE;
    }

    @Override
    public IOverheatState getPreviousState() {
        return OverheatManager.SLIGHT;
    }

    @Override
    public void tick(@NotNull AirCompressorBlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();
        if (level == null || !level.isClientSide || level.getGameTime() % 4 != 0) {
            return;
        }

        spawnParticles(level, pos, 0.4f, 0.8f);
    }

    @Override
    public String getSerializedName() {
        return NAME;
    }

    @Override
    public ChatFormatting getDisplayColor() {
        return ChatFormatting.GOLD;
    }

    @Override
    public float getEfficiency() {
        return 0.6f;
    }

    @Override
    public String getTranslationKey() {
        return "gui.goggles.air_compressor.moderate";
    }

    @Override
    public int getAnalogOutputSignal() {
        return 10;
    }

    @Override
    public void spawnParticlesInPonderLevel(@NotNull PonderLevel level, BlockPos pos, int tick) {
        if (tick % 2 != 0) {
            return;
        }

        spawnParticles(level, pos, 0.4f, 0.8f);
    }
}
