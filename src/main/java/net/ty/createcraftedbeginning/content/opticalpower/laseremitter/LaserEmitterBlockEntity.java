package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.LaserBehaviour;
import net.ty.createcraftedbeginning.content.opticalpower.laserreceiver.LaserReceiverBlock;
import net.ty.createcraftedbeginning.content.opticalpower.laserreceiver.LaserReceiverBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerConsumerBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetwork;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetworkManager;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LaserEmitterBlockEntity extends SmartBlockEntity implements OpticalPowerConsumerBlockEntity {
    private static final String COMPOUND_KEY_OPTICAL_POWER_POINTS = "OpticalPowerPoints";

    private LaserBehaviour laserBehaviour;
    private int allocatedPowerPoints;

    public LaserEmitterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        laserBehaviour = new LaserBehaviour(this, this::getLaserDirection, () -> allocatedPowerPoints > 0);
        behaviours.add(laserBehaviour);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level == null || level.isClientSide) {
            return;
        }

        OpticalPowerNetworkManager.registerConsumer(level, worldPosition);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        if (allocatedPowerPoints <= 0) {
            return;
        }

        BlockHitResult hitResult = laserBehaviour.getHitResult();
        if (hitResult == null) {
            return;
        }

        BlockPos hitPos = hitResult.getBlockPos();
        BlockState hitState = level.getBlockState(hitPos);
        if (!LaserReceiverBlock.canReceiveLaser(hitState, hitResult.getDirection()) || !(level.getBlockEntity(hitPos) instanceof LaserReceiverBlockEntity receiver)) {
            return;
        }

        receiver.receiveLaser(allocatedPowerPoints);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        OpticalPowerNetworkManager.ensureConsumer(level, worldPosition);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        if (!clientPacket) {
            return;
        }

        CCBNbtUtils.putInt(compoundTag, COMPOUND_KEY_OPTICAL_POWER_POINTS, allocatedPowerPoints);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (!clientPacket) {
            return;
        }

        allocatedPowerPoints = CCBMathUtils.clampNonNegative(CCBNbtUtils.getInt(compoundTag, COMPOUND_KEY_OPTICAL_POWER_POINTS), OpticalPowerNetwork.MAX_CONSUMER_POWER_POINTS);
    }

    @Override
    public void applyOpticalPowerAllocation(int powerPoints) {
        if (level == null || level.isClientSide) {
            return;
        }

        int clamped = CCBMathUtils.clampNonNegative(powerPoints, OpticalPowerNetwork.MAX_CONSUMER_POWER_POINTS);
        if (allocatedPowerPoints == clamped) {
            return;
        }

        allocatedPowerPoints = clamped;
        notifyUpdate();
    }

    @Override
    public AABB getRenderBoundingBox() {
        Vec3 extension = Vec3.atLowerCornerOf(getLaserDirection().getNormal()).scale(LaserBehaviour.MAX_RANGE);
        return new AABB(worldPosition).expandTowards(extension).inflate(0.125);
    }

    public boolean isLaserActive() {
        return allocatedPowerPoints > 0;
    }

    public float getBeamLength() {
        if (laserBehaviour == null) {
            return 0;
        }
        return laserBehaviour.getBeamLength();
    }

    public Direction getLaserDirection() {
        return getBlockState().getValue(DirectionalBlock.FACING);
    }
}
