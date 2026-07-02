package net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SlightOverheatState implements IOverheatState {
    private static final String NAME = "slight";
    private static final int TICK_RATE = 8;

    @Override
    public ChatFormatting getDisplayColor() {
        return ChatFormatting.YELLOW;
    }

    @Override
    public float getEfficiency() {
        return 0.9f;
    }

    @Override
    public int getAnalogOutputSignal() {
        return 5;
    }

    @Override
    public IOverheatState getNextState() {
        return OverheatManager.MODERATE;
    }

    @Override
    public IOverheatState getPreviousState() {
        return OverheatManager.NORMAL;
    }

    @Override
    public String getSerializedName() {
        return NAME;
    }

    @Override
    public String getTranslationKey() {
        return "gui.goggles.air_compressor.slight";
    }

    @Override
    public void spawnParticlesInPonderLevel(PonderLevel level, BlockPos pos, int tick) {
        if (tick % 4 != 0) {
            return;
        }

        spawnParticles(level, pos, 0.7f, 1.0f);
    }

    @Override
    public void tick(AirCompressorBlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();
        if (level == null || !level.isClientSide || level.getGameTime() % TICK_RATE != 0) {
            return;
        }

        spawnParticles(level, pos, 0.7f, 1.0f);
    }
}
