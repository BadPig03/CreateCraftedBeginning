package net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.gas.network.GasTransportNode;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.telemetry.GasPressureTelemetryTarget;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightRegulatorPumpBlockEntity extends KineticBlockEntity implements GasTransportNode, GasPressureTelemetryTarget {
    private static final String OUTLET_SET_PRESSURE_KEY = "OutletSetPressure";

    private final AirtightRegulatorPumpDisplay display;
    private AirtightRegulatorPumpPressureBehaviour pressureBehaviour;
    private CCBAdvancementBehaviour advancementBehaviour;
    private long outletSetPressurePa = AirtightRegulatorPumpPressureBehaviour.defaultOutletSetPressurePa();

    public AirtightRegulatorPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        display = new AirtightRegulatorPumpDisplay(this);
    }

    @Override
    public void acceptPressureTelemetry(long minPressurePa, long maxPressurePa) {
        if (level == null || level.isClientSide || isOverStressed() || maxPressurePa <= minPressurePa || getMaxPressureRisePa() <= 0 || getFlowRateLimit() <= 0) {
            return;
        }

        advancementBehaviour.awardPlayer(CCBAdvancements.TAKE_A_DEEP_BREATH);
    }

    @Override
    public void clearPressureTelemetry() {
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.putLong(OUTLET_SET_PRESSURE_KEY, outletSetPressurePa);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (compoundTag.contains(OUTLET_SET_PRESSURE_KEY)) {
            outletSetPressurePa = AirtightRegulatorPumpPressureBehaviour.normalizePressurePa(compoundTag.getLong(OUTLET_SET_PRESSURE_KEY));
        }
        if (pressureBehaviour == null) {
            return;
        }

        pressureBehaviour.syncFromOwner();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.TAKE_A_DEEP_BREATH, CCBAdvancements.GASEOUS_VARIATIONS);
        behaviours.add(advancementBehaviour);
        behaviours.add(new AirtightRegulatorPumpTransportBehaviour(this));

        pressureBehaviour = new AirtightRegulatorPumpPressureBehaviour(this);
        behaviours.add(pressureBehaviour);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = display.addToGoggleTooltip(tooltip);
        if (!added) {
            return false;
        }

        addStressImpactStats(tooltip, calculateStressApplied());
        return true;
    }

    @Override
    public boolean allowsGasTransport(Level level, BlockState state, BlockPos pos, Direction direction) {
        return isRunning() && direction == AirtightRegulatorPumpBlock.getInputDirection(state);
    }

    @Override
    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    public long getFlowRateLimit() {
        if (!isRunning()) {
            return 0;
        }

        long configuredMaximum = Math.max(0, CCBConfig.server().machines.airtightRegulatorPump.maxFlowPerTick.get());
        if (configuredMaximum <= 0) {
            return 0;
        }

        return Math.max(1, Math.round(configuredMaximum * getSpeedProgress()));
    }

    public long getOutletSetPressurePa() {
        return outletSetPressurePa;
    }

    public long getMaxPressureRisePa() {
        double configuredMaximumAtm = Math.max(0, CCBConfig.server().machines.airtightRegulatorPump.maxPressureRise.getF());
        return GasPressure.pascals(configuredMaximumAtm);
    }

    public void setOutletSetPressurePa(long outletSetPressurePa) {
        long normalizedPressurePa = AirtightRegulatorPumpPressureBehaviour.normalizePressurePa(outletSetPressurePa);
        if (this.outletSetPressurePa == normalizedPressurePa) {
            return;
        }

        this.outletSetPressurePa = normalizedPressurePa;
        if (pressureBehaviour != null) {
            pressureBehaviour.syncFromOwner();
        }
        onOutletSetPressureChanged();
    }

    void applyOutletSetPressureFromBehaviour(long outletSetPressurePa) {
        long normalizedPressurePa = AirtightRegulatorPumpPressureBehaviour.normalizePressurePa(outletSetPressurePa);
        if (this.outletSetPressurePa == normalizedPressurePa) {
            return;
        }

        this.outletSetPressurePa = normalizedPressurePa;
        onOutletSetPressureChanged();
    }

    private void onOutletSetPressureChanged() {
        setChanged();
        if (level == null) {
            return;
        }

        if (!level.isClientSide) {
            GasNetworkTopology.invalidate(level, worldPosition);
        }
        notifyUpdate();
    }

    private boolean hasRequiredSpeed() {
        return getBlockState().getBlock() instanceof AirtightRegulatorPumpBlock block && Mth.abs(getSpeed()) >= block.getMinimumRequiredSpeedLevel().getSpeedValue();
    }

    private boolean isRunning() {
        return level != null && !isRemoved() && hasRequiredSpeed();
    }

    private double getSpeedProgress() {
        double maximumSpeed = Math.max(1, AllConfigs.server().kinetics.maxRotationSpeed.get());
        return Mth.clamp(Mth.abs(getSpeed()) / maximumSpeed, 0, 1);
    }
}
