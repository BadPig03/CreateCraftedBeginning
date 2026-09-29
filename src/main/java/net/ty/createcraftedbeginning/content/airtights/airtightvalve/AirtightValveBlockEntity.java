package net.ty.createcraftedbeginning.content.airtights.airtightvalve;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
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
import net.ty.createcraftedbeginning.gas.network.GasTransportNode;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightValveBlockEntity extends KineticBlockEntity implements GasTransportNode {
    private static final String VALVE_POSITION_KEY = "ValvePosition";

    private final AirtightValveDisplay display;
    private CCBAdvancementBehaviour advancementBehaviour;
    private float valvePosition;

    public AirtightValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        display = new AirtightValveDisplay(this);
        valvePosition = state.hasProperty(AirtightValveBlock.OPEN) && state.getValue(AirtightValveBlock.OPEN) ? 1 : 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        float speed = getSpeed();
        if (speed == 0) {
            return;
        }

        float step = Mth.clamp(Mth.abs(speed) / 320, 0, 1);
        float previousPosition = valvePosition;
        valvePosition = Mth.approach(valvePosition, speed > 0 ? 1 : 0, step);

        if (previousPosition != valvePosition) {
            setChanged();
        }

        BlockState state = getBlockState();
        boolean open = state.getValue(AirtightValveBlock.OPEN);
        if (!open && valvePosition >= 1) {
            setOpen(true);
            return;
        }

        if (!open || valvePosition > 0) {
            return;
        }

        setOpen(false);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.putFloat(VALVE_POSITION_KEY, valvePosition);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (compoundTag.contains(VALVE_POSITION_KEY)) {
            valvePosition = Mth.clamp(compoundTag.getFloat(VALVE_POSITION_KEY), 0, 1);
            return;
        }

        valvePosition = getBlockState().getValue(AirtightValveBlock.OPEN) ? 1 : 0;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.GASEOUS_VARIATIONS);
        behaviours.add(advancementBehaviour);
        behaviours.add(new AirtightValveTransportBehaviour(this));
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
        return isOpen() && direction.getAxis() == state.getValue(AirtightValveBlock.AXIS);
    }

    @Override
    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    public boolean isOpen() {
        BlockState state = getBlockState();
        return state.hasProperty(AirtightValveBlock.OPEN) && state.getValue(AirtightValveBlock.OPEN);
    }

    private void setOpen(boolean open) {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        if (state.getValue(AirtightValveBlock.OPEN) == open) {
            return;
        }

        level.setBlockAndUpdate(worldPosition, state.setValue(AirtightValveBlock.OPEN, open));
        GasNetworkTopology.invalidate(level, worldPosition);
        notifyUpdate();
    }
}
