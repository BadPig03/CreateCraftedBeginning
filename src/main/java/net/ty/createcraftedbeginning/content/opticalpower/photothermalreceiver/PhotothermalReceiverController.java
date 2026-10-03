package net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.config.CCBConfig;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class PhotothermalReceiverController {
    private static final String COMPOUND_KEY_RECEIVED_POWER_LP = "ReceivedPowerLp";
    private static final String COMPOUND_KEY_OFFERED_POWER_LP = "OfferedPowerLp";
    private static final String COMPOUND_KEY_ILLUMINATED_PORTS = "IlluminatedPorts";
    private static final int PORT_MASK = (1 << PhotothermalReceiverPort.values().length) - 1;
    private static final int HEAT_SYNC_INTERVAL = 5;

    private final PhotothermalReceiverBlockEntity receiver;
    private final PhotothermalHeatState heatState = new PhotothermalHeatState();
    private final Set<BlockPos> receivedEmitters = new HashSet<>();
    private int receivedPowerLp;
    private int offeredPowerLp;
    private long accumulatingTick = Long.MIN_VALUE;
    private int accumulatingPowerLp;
    private long completedTick = Long.MIN_VALUE;
    private int completedPowerLp;
    private int accumulatingPorts;
    private int completedPorts;
    private int illuminatedPorts;

    PhotothermalReceiverController(PhotothermalReceiverBlockEntity receiver) {
        this.receiver = receiver;
    }

    void tickServer() {
        Level level = receiver.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        long gameTime = level.getGameTime();
        int previousPower = receivedPowerLp;
        int previousOfferedPower = offeredPowerLp;
        int previousPorts = illuminatedPorts;
        illuminatedPorts = 0;
        offeredPowerLp = 0;
        if (accumulatingTick == gameTime - 1) {
            offeredPowerLp = accumulatingPowerLp;
            illuminatedPorts = accumulatingPorts;
        }
        else if (completedTick == gameTime - 1) {
            offeredPowerLp = completedPowerLp;
            illuminatedPorts = completedPorts;
        }

        receivedPowerLp = Math.min(offeredPowerLp, CCBConfig.server().opticalPower.photothermalReceiver.maxReceivedPowerLp.get());
        int previousHeatProgress = heatState.getHeatProgress();
        heatState.tick(receivedPowerLp);
        HeatLevel target = switch (heatState.getHeatLevel()) {
            case 1 -> HeatLevel.KINDLED;
            case 2 -> HeatLevel.SEETHING;
            default -> HeatLevel.NONE;
        };
        BlockState state = receiver.getBlockState();
        if (state.getValue(BlazeBurnerBlock.HEAT_LEVEL) != target) {
            level.setBlockAndUpdate(receiver.getBlockPos(), state.setValue(BlazeBurnerBlock.HEAT_LEVEL, target));
            receiver.notifyUpdate();
            return;
        }

        int heatProgress = heatState.getHeatProgress();
        boolean heatProgressChanged = previousHeatProgress != heatProgress;
        if (heatProgressChanged) {
            receiver.setChanged();
        }

        boolean syncRequired = previousPorts != illuminatedPorts || previousPower != receivedPowerLp || previousOfferedPower != offeredPowerLp || heatProgressChanged && (gameTime % HEAT_SYNC_INTERVAL == 0 || heatProgress == PhotothermalHeatState.getTargetHeatProgress(receivedPowerLp));
        if (!syncRequired) {
            return;
        }

        receiver.sendData();
    }

    void write(CompoundTag compoundTag, boolean clientPacket) {
        heatState.write(compoundTag);
        if (!clientPacket) {
            return;
        }

        compoundTag.putInt(COMPOUND_KEY_ILLUMINATED_PORTS, illuminatedPorts);
        compoundTag.putInt(COMPOUND_KEY_RECEIVED_POWER_LP, receivedPowerLp);
        compoundTag.putInt(COMPOUND_KEY_OFFERED_POWER_LP, offeredPowerLp);
    }

    void read(CompoundTag compoundTag, boolean clientPacket) {
        heatState.read(compoundTag);
        if (!clientPacket) {
            return;
        }

        illuminatedPorts = compoundTag.getInt(COMPOUND_KEY_ILLUMINATED_PORTS) & PORT_MASK;
        receivedPowerLp = Math.max(0, compoundTag.getInt(COMPOUND_KEY_RECEIVED_POWER_LP));
        offeredPowerLp = Math.max(0, compoundTag.getInt(COMPOUND_KEY_OFFERED_POWER_LP));
    }

    void receiveLaser(BlockPos emitterPos, PhotothermalReceiverPort port, int powerLp) {
        Level level = receiver.getLevel();
        if (level == null || level.isClientSide || powerLp <= 0) {
            return;
        }

        long gameTime = level.getGameTime();
        if (accumulatingTick != gameTime) {
            completedTick = accumulatingTick;
            completedPowerLp = accumulatingPowerLp;
            completedPorts = accumulatingPorts;
            accumulatingTick = gameTime;
            accumulatingPowerLp = 0;
            accumulatingPorts = 0;
            receivedEmitters.clear();
        }

        if (!receivedEmitters.add(emitterPos.immutable())) {
            return;
        }

        accumulatingPorts |= port.getMask();
        accumulatingPowerLp = (int) Math.min(Integer.MAX_VALUE, (long) accumulatingPowerLp + powerLp);
    }

    boolean isPortIlluminated(PhotothermalReceiverPort port) {
        return (illuminatedPorts & port.getMask()) != 0;
    }

    int getReceivedPowerLp() {
        return receivedPowerLp;
    }

    int getOfferedPowerLp() {
        return offeredPowerLp;
    }

    int getHeatLevel() {
        return heatState.getHeatLevel();
    }

    int getHeatProgress() {
        return heatState.getHeatProgress();
    }
}
