package net.ty.createcraftedbeginning.content.airtights.gas;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;

public class GasBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private static final String COMPOUND_KEY_GAS_AMOUNT = "GasAmount";
    private static final String COMPOUND_KEY_GAS_TYPE = "GasType";
    private static final String COMPOUND_KEY_DIFFUSE_COOLDOWN = "DiffuseCooldown";

    private static final float DIFFUSE_DIVIDEND = 20000.0f;
    private static final int DEFAULT_DIFFUSE_INTERVAL = 20;
    private static final int DIFFUSE_THRESHOLD = 50;

    private int diffuseCooldown = DEFAULT_DIFFUSE_INTERVAL;
    private long gasAmount;
    private Gas gasType = Gas.EMPTY_GAS_HOLDER.value();

    public GasBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(DEFAULT_DIFFUSE_INTERVAL);
    }

    public static @NotNull EnumMap<Direction, Boolean> calculateDiffuseWeights(@NotNull Level level, BlockPos pos) {
        EnumMap<Direction, Boolean> weightsMap = new EnumMap<>(Direction.class);
        if (level.isClientSide) {
            return weightsMap;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof GasBlockEntity)) {
            return weightsMap;
        }

        for (Direction direction : Iterate.directions) {
            if (!GasBlock.canDiffuseTo(level.getBlockState(pos.relative(direction)))) {
                continue;
            }

            weightsMap.put(direction, true);
        }
        return weightsMap;
    }

    @Contract(pure = true)
    public static void diffuseGas(@NotNull Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof GasBlockEntity gas)) {
            return;
        }

        long currentAmount = gas.gasAmount;
        if (currentAmount < DIFFUSE_THRESHOLD) {
            return;
        }

        Direction[] directions = Iterate.horizontalDirections;
        for (Direction direction : directions) {
            BlockPos relativePos = pos.relative(direction);
            BlockState relativeState = level.getBlockState(relativePos);
            if (!GasBlock.canDiffuseTo(relativeState)) {
                continue;
            }

            if (level.isEmptyBlock(relativePos)) {
                //level.setBlockAndUpdate(relativePos, CCBBlocks.GAS_BLOCK.getDefaultState());
            }
            if (!(level.getBlockEntity(relativePos) instanceof GasBlockEntity relativeGas)) {
                continue;
            }

            long diffuseAmount = Mth.ceil(currentAmount * 0.2f);
            gas.changeGasAmount(-diffuseAmount, true);
            relativeGas.changeGasAmount(diffuseAmount, true);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.goggles.gas").forGoggles(tooltip);
        CCBLang.gasName(gasType).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        CCBLang.number(gasAmount).add(CCBLang.translate("gui.goggles.unit.milli_buckets")).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        long currentAmount = gasAmount;
        if (currentAmount < DIFFUSE_THRESHOLD) {
            if (currentAmount <= 0) {
                level.destroyBlock(worldPosition, false);
            }
            return;
        }

        diffuseGas(level, worldPosition);
        sendData();
    }

    @Override
    protected void write(@NotNull CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.putLong(COMPOUND_KEY_GAS_AMOUNT, gasAmount);
        NBTHelper.writeResourceLocation(compoundTag, COMPOUND_KEY_GAS_TYPE, gasType.getResourceLocation());
        if (clientPacket) {
            return;
        }

        compoundTag.putInt(COMPOUND_KEY_DIFFUSE_COOLDOWN, diffuseCooldown);
    }

    @Override
    protected void read(@NotNull CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        gasAmount = compoundTag.getLong(COMPOUND_KEY_GAS_AMOUNT);
        gasType = Gas.getGasByName(NBTHelper.readResourceLocation(compoundTag, COMPOUND_KEY_GAS_TYPE));
        if (clientPacket) {
            return;
        }

        diffuseCooldown = compoundTag.getInt(COMPOUND_KEY_DIFFUSE_COOLDOWN);
    }

    public void changeGasAmount(long amount, boolean refreshCooldown) {
        gasAmount += amount;
        if (refreshCooldown) {
            //diffuseCooldown = gasAmount > 0 ? Mth.ceil(DIFFUSE_DIVIDEND / gasAmount) : DEFAULT_DIFFUSE_INTERVAL;
            diffuseCooldown = DEFAULT_DIFFUSE_INTERVAL;
        }
        notifyUpdate();
        if (gasAmount > 0 || level == null || level.isClientSide) {
            return;
        }

        level.destroyBlock(worldPosition, false);
    }
}
