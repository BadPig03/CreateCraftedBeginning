package net.ty.createcraftedbeginning.content.opticalpower.laserreceiver;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerUnits;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LaserReceiverBlockEntity extends GeneratingKineticBlockEntity {
    private static final String COMPOUND_KEY_OPTICAL_POWER_POINTS = "OpticalPowerPoints";
    private static final float GENERATED_SPEED = 32;
    private static final int CAPACITY_REFERENCE_POWER_POINTS = 16;

    private int receivedPowerPoints;
    private long accumulatingTick = Long.MIN_VALUE;
    private int accumulatingPowerPoints;
    private long completedTick = Long.MIN_VALUE;
    private int completedPowerPoints;

    public LaserReceiverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static int getMaxReceivedPowerPoints() {
        return Math.max(1, OpticalPowerUnits.toPowerPoints(CCBConfig.server().opticalPower.maxLaserReceiverPowerSu.get()));
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level == null || level.isClientSide) {
            return;
        }

        receivedPowerPoints = 0;
        updateGeneratedRotation();
    }

    @Override
    public float calculateAddedStressCapacity() {
        capacity = calculateCapacity();
        lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        if (!clientPacket) {
            return;
        }

        CCBNbtUtils.putInt(compoundTag, COMPOUND_KEY_OPTICAL_POWER_POINTS, receivedPowerPoints);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (!clientPacket || !CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_OPTICAL_POWER_POINTS)) {
            return;
        }

        receivedPowerPoints = CCBMathUtils.clampNonNegative(CCBNbtUtils.getInt(compoundTag, COMPOUND_KEY_OPTICAL_POWER_POINTS), getMaxReceivedPowerPoints());
    }

    @Override
    public float getGeneratedSpeed() {
        if (receivedPowerPoints <= 0) {
            return 0;
        }
        return GENERATED_SPEED;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        long gameTime = level.getGameTime();
        int targetPowerPoints = 0;
        if (accumulatingTick == gameTime - 1) {
            targetPowerPoints = accumulatingPowerPoints;
        }
        else if (completedTick == gameTime - 1) {
            targetPowerPoints = completedPowerPoints;
        }
        applyReceivedPower(targetPowerPoints);
    }

    public void receiveLaser(int powerPoints) {
        if (level == null || level.isClientSide) {
            return;
        }

        long gameTime = level.getGameTime();
        if (accumulatingTick != gameTime) {
            completedTick = accumulatingTick;
            completedPowerPoints = accumulatingPowerPoints;
            accumulatingTick = gameTime;
            accumulatingPowerPoints = 0;
        }
        accumulatingPowerPoints = CCBMathUtils.clampNonNegative(accumulatingPowerPoints + powerPoints, getMaxReceivedPowerPoints());
    }

    public Direction getOutputDirection() {
        return getBlockState().getValue(DirectionalBlock.FACING).getOpposite();
    }

    private float calculateCapacity() {
        double laserFactor = (double) receivedPowerPoints / CAPACITY_REFERENCE_POWER_POINTS;
        return (float) (BlockStressValues.getCapacity(getStressConfigKey()) * laserFactor);
    }

    private void applyReceivedPower(int powerPoints) {
        int clamped = CCBMathUtils.clampNonNegative(powerPoints, getMaxReceivedPowerPoints());
        if (receivedPowerPoints == clamped) {
            return;
        }

        receivedPowerPoints = clamped;
        updateGeneratedRotation();
        notifyUpdate();
    }
}
