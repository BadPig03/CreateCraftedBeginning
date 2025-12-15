package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TeslaTurbineBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {
    private static final String COMPOUND_KEY_CORE = "Core";

    private static final int BASE_ROTATION_SPEED = 16;
    private static final int LAZY_TICK_RATE = 4;

    private final TeslaTurbineCore core;

    private CCBAdvancementBehaviour advancementBehaviour;

    public TeslaTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        core = new TeslaTurbineCore(this);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    @Override
    public void tick() {
        super.tick();
        core.tick();
        if (isOverStressed()) {
            return;
        }

        updateGeneratedRotation();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        core.getTooltipBuilder().addToGoggleTooltip(tooltip);
        return true;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        core.lazyTick(this);
    }

    @Override
    public void initialize() {
        super.initialize();
        updateGeneratedRotation();
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level == null || level.isClientSide) {
            return;
        }
        if (getSpeed() == 0) {
            return;
        }
        if (core == null || !core.getStructureManager().isActive() || core.getLevelCalculator().getCurrentLevel() != TeslaTurbineCore.MAX_LEVEL) {
            return;
        }

        advancementBehaviour.awardPlayer(CCBAdvancements.MIRACLE_OF_ENGINEERING);
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = Mth.abs(getGeneratedSpeed()) * (float) BlockStressValues.getCapacity(CCBBlocks.TESLA_TURBINE_BLOCK.get()) / AllConfigs.server().kinetics.maxRotationSpeed.get();
        lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    public void write(@NotNull CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.put(COMPOUND_KEY_CORE, core.write(provider));
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (!compoundTag.contains(COMPOUND_KEY_CORE)) {
            return;
        }

        core.read(compoundTag.getCompound(COMPOUND_KEY_CORE), provider);
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.MIRACLE_OF_ENGINEERING, CCBAdvancements.TESLA_TURBINE_EASY_AS_PIE);
        behaviours.add(advancementBehaviour);
        super.addBehaviours(behaviours);
    }

    @Override
    public float getGeneratedSpeed() {
        int direction = core.getFlowMeter().isClockwiseFlow() ? -1 : 1;
        int modifier = getBlockState().getValue(TeslaTurbineBlock.AXIS) == Axis.Z ? -1 : 1;
        return BASE_ROTATION_SPEED * core.getLevelCalculator().getCurrentLevel() * direction * modifier;
    }

    public TeslaTurbineCore getCore() {
        return core;
    }

    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }
}
