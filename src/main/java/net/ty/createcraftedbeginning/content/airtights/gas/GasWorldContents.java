package net.ty.createcraftedbeginning.content.airtights.gas;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class GasWorldContents {
    private static final String COMPOUND_KEY_BLOCK_POS = "BlockPos";
    private static final String COMPOUND_KEY_GAS_AMOUNT = "GasAmount";
    private static final String COMPOUND_KEY_GAS_TYPE = "GasType";

    private final BlockPos blockPos;
    private long gasAmount;
    private Gas gasType;

    public GasWorldContents(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public GasWorldContents(BlockPos blockPos, Gas gasType, long gasAmount) {
        this.blockPos = blockPos;
        this.gasType = gasType;
        this.gasAmount = gasAmount;
    }

    public static @NotNull GasWorldContents read(@NotNull CompoundTag compoundTag) {
        BlockPos blockPos = BlockPos.of(compoundTag.getLong(COMPOUND_KEY_BLOCK_POS));
        GasWorldContents contents = new GasWorldContents(blockPos);
        contents.gasType = Gas.getGasByName(NBTHelper.readResourceLocation(compoundTag, COMPOUND_KEY_GAS_TYPE));
        contents.gasAmount = compoundTag.getLong(COMPOUND_KEY_GAS_AMOUNT);
        return contents;
    }

    @Contract(pure = true)
    public @NotNull CompoundTag write() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putLong(COMPOUND_KEY_BLOCK_POS, blockPos.asLong());
        compoundTag.putLong(COMPOUND_KEY_GAS_AMOUNT, gasAmount);
        NBTHelper.writeResourceLocation(compoundTag, COMPOUND_KEY_GAS_TYPE, gasType.getResourceLocation());
        return compoundTag;
    }

    @Contract(" -> new")
    public @NotNull ChunkPos getChunkPos() {
        return new ChunkPos(blockPos);
    }

    @Contract(" -> new")
    public @NotNull ShortPos getShortPos() {
        return ShortPos.fromGlobalPos(blockPos);
    }

    @Contract(" -> new")
    public @NotNull BlockPos getBlockPos() {
        return blockPos;
    }

    @Contract(" -> new")
    public @NotNull Gas getGasType() {
        return gasType;
    }

    public long getGasAmount() {
        return gasAmount;
    }
}
