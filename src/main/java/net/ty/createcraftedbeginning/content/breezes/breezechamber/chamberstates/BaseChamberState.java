package net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

public abstract class BaseChamberState {
    protected static final String COMPOUND_KEY_REMAINING_TIME = "RemainingTime";
    protected static final String COMPOUND_KEY_IS_CREATIVE = "isCreative";
    protected static final int OVERFLOW_THRESHOLD = 54000;
    protected static final int NOTIFY_INTERVAL = 5;

    protected int remainingTime;
    protected boolean isCreative;

    public BaseChamberState(int remainingTime, boolean isCreative) {
        this.remainingTime = remainingTime;
        this.isCreative = isCreative;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public boolean isCreative() {
        return isCreative;
    }

    public void read(@NotNull CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_REMAINING_TIME)) {
            remainingTime = compoundTag.getInt(COMPOUND_KEY_REMAINING_TIME);
        }
        if (compoundTag.contains(COMPOUND_KEY_IS_CREATIVE)) {
            isCreative = compoundTag.getBoolean(COMPOUND_KEY_IS_CREATIVE);
        }
    }

    public void save(@NotNull CompoundTag compoundTag) {
        compoundTag.putInt(COMPOUND_KEY_REMAINING_TIME, remainingTime);
        compoundTag.putBoolean(COMPOUND_KEY_IS_CREATIVE, isCreative);
    }

    public void tick(BreezeChamberBlockEntity chamber) {
        validateStates(chamber);
    }

    public void validateStates(@NotNull BreezeChamberBlockEntity chamber) {
        Level level = chamber.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockPos pos = chamber.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(CCBBlocks.BREEZE_CHAMBER_BLOCK)) {
            return;
        }

        WindLevel windLevel = state.getValue(BreezeChamberBlock.WIND_LEVEL);
        if (windLevel == WindLevel.CALM || remainingTime != 0 || isCreative) {
            return;
        }

        chamber.setChamberState(new InactiveChamberState());
    }

    public abstract WindLevel getWindLevel();

    public abstract ChargerType getChargerType();

    public abstract InteractionResult onItemInsert(BreezeChamberBlockEntity chamber, ItemStack stack, boolean forceOverflow, boolean simulate);
}
