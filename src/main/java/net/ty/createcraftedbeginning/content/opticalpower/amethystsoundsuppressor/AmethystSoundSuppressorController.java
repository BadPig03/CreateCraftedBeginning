package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor.AmethystSoundSuppressorSerialization.ClientSnapshot;
import net.ty.createcraftedbeginning.content.opticalpower.energy.OpticalEnergyStorage;
import net.ty.createcraftedbeginning.content.opticalpower.laser.exposure.OpticalExposure;
import net.ty.createcraftedbeginning.content.opticalpower.laser.exposure.OpticalExposureSettlement.Receipt;
import net.ty.createcraftedbeginning.content.opticalpower.laser.exposure.OpticalExposureSettlement;
import net.ty.createcraftedbeginning.content.opticalpower.laser.exposure.OpticalIllumination;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public final class AmethystSoundSuppressorController {
    public static final double CAPACITY_LUMEN_SECONDS = 128;
    private static final double GROWTH_COST_LUMEN_SECONDS = 64;
    private static final int STORAGE_SYNC_INTERVAL_TICKS = 5;

    private final AmethystSoundSuppressorBlockEntity suppressor;
    private final AmethystSoundSuppressorIdentity identity;
    private final OpticalExposureSettlement settlement;
    private final OpticalEnergyStorage storage = new OpticalEnergyStorage(CAPACITY_LUMEN_SECONDS);
    private double receivedFluxLm;
    private double qualifiedFluxLm;
    private long lastCrystalGrowthTick = -1;

    AmethystSoundSuppressorController(AmethystSoundSuppressorBlockEntity suppressor) {
        this.suppressor = suppressor;
        identity = new AmethystSoundSuppressorIdentity(suppressor.getBlockPos(), null);
        settlement = new OpticalExposureSettlement(suppressor);
    }

    void refresh() {
        if (!(suppressor.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        AmethystSoundSuppressorManager.update(serverLevel, identity, SubLevelBridge.resolve(serverLevel, suppressor.getBlockPos()).blockPos(), suppressor);
    }

    void tickServer(ServerLevel serverLevel) {
        AmethystSoundSuppressorManager.update(serverLevel, identity, SubLevelBridge.resolve(serverLevel, suppressor.getBlockPos()).blockPos(), suppressor);
        Receipt receipt = settlement.settle(Double.MAX_VALUE, exposure -> exposure.hit().target().contraptionId() == null);
        if (receipt == null) {
            return;
        }

        long gameTime = serverLevel.getGameTime();
        double previousReceivedFluxLm = receivedFluxLm;
        double previousQualifiedFluxLm = qualifiedFluxLm;
        double previousQuantity = storage.getQuantityLumenSeconds();
        OpticalIllumination illumination = new OpticalIllumination();
        VoxelShape receivingShape = AmethystSoundSuppressorPort.INSTANCE.getReceivingShape();
        VoxelShape collisionShape = AmethystSoundSuppressorPort.INSTANCE.getCollisionShape();
        for (OpticalExposure exposure : receipt.exposures()) {
            illumination.add(exposure, 0, receivingShape, collisionShape);
        }
        receivedFluxLm = Math.min(receipt.offeredFluxLm(), illumination.getFluxLm());
        qualifiedFluxLm = Math.min(receivedFluxLm, illumination.getQualifiedFluxLm(0, OpticalIllumination.GROWTH_ILLUMINANCE_LX));
        storage.receive(qualifiedFluxLm * OpticalExposureSettlement.TICK_SECONDS);
        if (canGrowCrystal(gameTime)) {
            AmethystSoundSuppressorCrystalGrowth.tryGrow(serverLevel, suppressor);
        }
        double quantity = storage.getQuantityLumenSeconds();
        if (previousQuantity != quantity) {
            suppressor.setChanged();
        }
        if (receivedFluxLm == previousReceivedFluxLm && qualifiedFluxLm == previousQualifiedFluxLm && (previousQuantity == quantity || gameTime % STORAGE_SYNC_INTERVAL_TICKS != 0)) {
            return;
        }

        suppressor.sendData();
    }

    void write(CompoundTag tag, boolean clientPacket) {
        AmethystSoundSuppressorSerialization.write(tag, storage, lastCrystalGrowthTick, receivedFluxLm, qualifiedFluxLm, clientPacket);
    }

    void readSaved(CompoundTag tag) {
        storage.readSaved(tag, suppressor.getBlockPos());
        lastCrystalGrowthTick = AmethystSoundSuppressorSerialization.readGrowthTick(tag);
    }

    void restoreClientState(ClientSnapshot snapshot) {
        storage.restoreQuantity(snapshot.quantityLumenSeconds());
        lastCrystalGrowthTick = snapshot.lastGrowthTick();
        receivedFluxLm = snapshot.receivedFluxLm();
        qualifiedFluxLm = snapshot.qualifiedFluxLm();
    }

    double getReceivedFluxLm() {
        return receivedFluxLm;
    }

    double getQuantityLumenSeconds() {
        return storage.getQuantityLumenSeconds();
    }

    double getQualifiedFluxLm() {
        return qualifiedFluxLm;
    }

    boolean isIlluminated() {
        return qualifiedFluxLm > 0;
    }

    boolean canGrowCrystal(long gameTime) {
        return storage.hasAtLeast(GROWTH_COST_LUMEN_SECONDS) && lastCrystalGrowthTick != gameTime;
    }

    void recordCrystalGrowth(long gameTime) {
        storage.extract(GROWTH_COST_LUMEN_SECONDS);
        lastCrystalGrowthTick = gameTime;
        suppressor.setChanged();
        suppressor.sendData();
    }

    void remove() {
        receivedFluxLm = 0;
        qualifiedFluxLm = 0;
        if (!(suppressor.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        AmethystSoundSuppressorManager.remove(serverLevel, identity, suppressor);
    }
}
