package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasTransportNode;
import net.ty.createcraftedbeginning.gas.telemetry.GasPressureTelemetryTarget;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPumpBlockEntity extends KineticBlockEntity implements GasTransportNode, GasPressureTelemetryTarget {
    private final AirtightPumpPerformanceController performanceController;
    private final AirtightPumpDisplay display;
    private CCBAdvancementBehaviour advancementBehaviour;

    public AirtightPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        performanceController = new AirtightPumpPerformanceController(this);
        display = new AirtightPumpDisplay(this);
    }

    @Override
    public void acceptPressureTelemetry(long minPressurePa, long maxPressurePa) {
        if (level == null || level.isClientSide || isOverStressed() || maxPressurePa <= minPressurePa || getPumpMaxPressureBoostPa() <= 0 || getPumpFlowRateLimit() <= 0) {
            return;
        }

        advancementBehaviour.awardPlayer(CCBAdvancements.TAKE_A_DEEP_BREATH);
    }

    @Override
    public void clearPressureTelemetry() {
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.TAKE_A_DEEP_BREATH, CCBAdvancements.GASEOUS_VARIATIONS);
        behaviours.add(advancementBehaviour);

        behaviours.add(createTransportBehaviour());
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
        return performanceController.allowsGasTransport(state, direction);
    }

    @Override
    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    public long getPumpMaxPressureBoostPa() {
        return performanceController.getPumpMaxPressureBoostPa();
    }

    public long getPumpFlowRateLimit() {
        return performanceController.getPumpFlowRateLimit();
    }

    AirtightPumpPerformanceController getPerformanceController() {
        return performanceController;
    }

    private GasTransportBehaviour createTransportBehaviour() {
        return new AirtightPumpTransportBehaviour(this);
    }
}
