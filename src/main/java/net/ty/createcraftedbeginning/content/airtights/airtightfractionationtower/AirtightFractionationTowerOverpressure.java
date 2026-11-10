package net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.overpressure.OverpressureState;
import net.ty.createcraftedbeginning.gas.overpressure.PressureRuptureService;
import net.ty.createcraftedbeginning.gas.visual.OverpressureTooltips.Snapshot;
import net.ty.createcraftedbeginning.gas.visual.OverpressureTooltips.Status;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightFractionationTowerOverpressure {
    private static final String COMPOUND_KEY_MAXIMUM_PRESSURE = "MaximumPressure";
    private static final int SYNC_INTERVAL = 5;

    private final AirtightFractionationTowerBlockEntity owner;
    private final OverpressureState state = new OverpressureState();
    private long maximumPressurePa;
    private int syncCooldown;
    private boolean syncPending;
    private boolean ruptured;

    AirtightFractionationTowerOverpressure(AirtightFractionationTowerBlockEntity owner) {
        this.owner = owner;
    }

    boolean tick() {
        if (ruptured) {
            return true;
        }

        Level level = owner.getLevel();
        if (level == null || level.isClientSide || owner.isVirtual()) {
            return false;
        }

        PressureSample sample = samplePressure(level);
        if (sample == null) {
            return false;
        }

        boolean pressureChanged = maximumPressurePa != sample.pressurePa();
        maximumPressurePa = sample.pressurePa();
        boolean stressChanged = state.tick(maximumPressurePa);
        if (stressChanged) {
            owner.setChanged();
        }
        if (state.isFailureReady() && sample.compartment() != null) {
            ruptured = true;
            PressureRuptureService.rupture(level, sample.position(), sample.compartment());
            return true;
        }

        syncPending |= pressureChanged || stressChanged;
        if (syncCooldown > 0) {
            syncCooldown--;
        }
        if (!syncPending || syncCooldown > 0) {
            return false;
        }

        syncPending = false;
        syncCooldown = SYNC_INTERVAL;
        owner.sendData();
        return false;
    }

    @Nullable Snapshot snapshot() {
        boolean overpressure = GasPressureLimits.isOverpressure(maximumPressurePa);
        float stress = state.getStressFraction();
        if (!overpressure && stress <= 0) {
            return null;
        }

        Status status;
        if (overpressure && state.isAtFailureThreshold()) {
            status = Status.CRITICAL;
        }
        else if (overpressure) {
            status = Status.OVERPRESSURE;
        }
        else {
            status = Status.RECOVERING;
        }
        return new Snapshot(stress, status);
    }

    CompoundTag write(boolean clientPacket) {
        CompoundTag tag = new CompoundTag();
        state.writeStress(tag);
        if (!clientPacket) {
            return tag;
        }

        tag.putLong(COMPOUND_KEY_MAXIMUM_PRESSURE, maximumPressurePa);
        return tag;
    }

    void read(CompoundTag tag, boolean clientPacket) {
        state.readStress(tag);
        state.restartStabilizationGrace();
        if (!clientPacket) {
            return;
        }

        maximumPressurePa = GasPressureLimits.clampToHardLimit(NbtValues.getLongOrDefault(tag, COMPOUND_KEY_MAXIMUM_PRESSURE, GasPressure.VACUUM_PA));
    }

    private @Nullable PressureSample samplePressure(Level level) {
        BlockPos origin = owner.getOrigin();
        if (origin == null) {
            return null;
        }

        long maximum = GasPressure.VACUUM_PA;
        BlockPos failurePos = owner.getBlockPos();
        GasPressureCompartment failureCompartment = null;
        for (int layer = 0; layer < owner.getHeight(); layer++) {
            BlockPos corner = origin.above(layer);
            if (!level.isLoaded(corner) || !level.isLoaded(corner.offset(2, 0, 0)) || !level.isLoaded(corner.offset(0, 0, 2)) || !level.isLoaded(corner.offset(2, 0, 2))) {
                return null;
            }

            BlockPos center = corner.offset(1, 0, 1);
            if (!(level.getBlockEntity(center) instanceof AirtightFractionationTowerBlockEntity layerController) || layerController.isRemoved() || !owner.isPartOfSameTower(layerController)) {
                return null;
            }

            GasStorageHandler gases = layerController.getInventory().getGases();
            if (gases == null) {
                return null;
            }

            for (int tank = 0; tank < gases.getTanks(); tank++) {
                GasPressureCompartment compartment = gases.getPressureCompartment(tank);
                long pressure = compartment.getPressurePa();
                if (pressure <= maximum) {
                    continue;
                }

                maximum = pressure;
                failurePos = center;
                failureCompartment = compartment;
            }
        }
        return new PressureSample(maximum, failurePos, failureCompartment);
    }

    private record PressureSample(long pressurePa, BlockPos position, @Nullable GasPressureCompartment compartment) {
    }
}
