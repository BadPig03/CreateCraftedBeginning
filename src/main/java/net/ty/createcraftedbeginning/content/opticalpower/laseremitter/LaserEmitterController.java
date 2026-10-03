package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.content.opticalpower.laser.LaserBehaviour;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetwork;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver.PhotothermalReceiverBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver.PhotothermalReceiverPort;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserEmitterController {
    private static final String COMPOUND_KEY_OPTICAL_POWER_LP = "OpticalPowerLp";

    private final LaserEmitterBlockEntity emitter;
    private int allocatedPowerLp;

    LaserEmitterController(LaserEmitterBlockEntity emitter) {
        this.emitter = emitter;
    }

    void tickServer(LaserBehaviour laserBehaviour) {
        Level level = emitter.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        if (allocatedPowerLp <= 0) {
            return;
        }

        BlockHitResult hitResult = laserBehaviour.getHitResult();
        if (hitResult == null) {
            return;
        }

        BlockPos hitPos = hitResult.getBlockPos();
        PhotothermalReceiverPort port = PhotothermalReceiverPort.findHit(hitResult);
        if (port == null || !(level.getBlockEntity(hitPos) instanceof PhotothermalReceiverBlockEntity receiver)) {
            return;
        }

        receiver.receiveLaser(emitter.getBlockPos(), port, allocatedPowerLp);
    }

    void write(CompoundTag compoundTag, boolean clientPacket) {
        if (!clientPacket) {
            return;
        }

        compoundTag.putInt(COMPOUND_KEY_OPTICAL_POWER_LP, allocatedPowerLp);
    }

    void read(CompoundTag compoundTag, boolean clientPacket) {
        if (!clientPacket) {
            return;
        }

        allocatedPowerLp = Mth.clamp(compoundTag.getInt(COMPOUND_KEY_OPTICAL_POWER_LP), 0, OpticalPowerNetwork.MAX_CONSUMER_POWER_LP);
    }

    void applyAllocation(int powerLp) {
        Level level = emitter.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        int clamped = Mth.clamp(powerLp, 0, OpticalPowerNetwork.MAX_CONSUMER_POWER_LP);
        if (allocatedPowerLp == clamped) {
            return;
        }

        allocatedPowerLp = clamped;
        emitter.notifyUpdate();
    }

    int getAllocatedPowerLp() {
        return allocatedPowerLp;
    }
}
