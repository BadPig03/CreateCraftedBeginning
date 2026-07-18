package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {
    private static final String COMPOUND_KEY_CORE = "Core";
    private final TeslaTurbineCore core;

    private CCBAdvancementBehaviour advancementBehaviour;
    private float lastGeneratedSpeed = Float.NaN;

    public TeslaTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        core = new TeslaTurbineCore(this);
        setLazyTickRate(TeslaTurbineUtils.LAZY_TICK_RATE);
    }

    public static float calculateStressCapacity(float generatedSpeed) {
        float speed = Math.abs(generatedSpeed);
        int maxSpeed = AllConfigs.server().kinetics.maxRotationSpeed.get();
        double baseCapacity = BlockStressValues.getCapacity(CCBBlocks.TESLA_TURBINE_BLOCK.get());
        return (float) (speed * baseCapacity / maxSpeed);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        core.tick();
        if (isOverStressed()) {
            lastGeneratedSpeed = Float.NaN;
            return;
        }

        refreshGeneratedRotationIfNeeded();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return core.addToGoggleTooltip(tooltip);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        core.lazyTick();
    }

    @Override
    public void initialize() {
        super.initialize();
        lastGeneratedSpeed = Float.NaN;
        refreshGeneratedRotationIfNeeded();
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = calculateStressCapacity(getGeneratedSpeed());
        lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level == null || level.isClientSide || getSpeed() == 0 || getGeneratedSpeed() == 0) {
            return;
        }

        if (!core.getStructureManager().isActive()) {
            return;
        }

        advancementBehaviour.awardPlayer(CCBAdvancements.GENIUS_ENGINEER);
        if (core.getLevelCalculator().getCurrentLevel() != TeslaTurbineUtils.MAX_LEVEL) {
            return;
        }

        advancementBehaviour.awardPlayer(CCBAdvancements.MIRACLE_OF_ENGINEERING);
    }

    @Override
    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.put(COMPOUND_KEY_CORE, core.write(provider, clientPacket));
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        CompoundTag coreTag = compoundTag.contains(COMPOUND_KEY_CORE) ? compoundTag.getCompound(COMPOUND_KEY_CORE) : new CompoundTag();
        core.read(coreTag, provider, clientPacket);
        lastGeneratedSpeed = Float.NaN;
    }

    @Override
    public float getGeneratedSpeed() {
        int direction = core.getFlowMeter().isClockwiseFlow() ? -1 : 1;
        int modifier = getBlockState().getValue(TeslaTurbineBlock.AXIS) == Axis.Z ? -1 : 1;
        return TeslaTurbineUtils.BASE_ROTATION_SPEED * core.getLevelCalculator().getCurrentLevel() * direction * modifier;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.MIRACLE_OF_ENGINEERING, CCBAdvancements.GENIUS_ENGINEER, CCBAdvancements.TESLA_TURBINE_EASY_AS_PIE);
        behaviours.add(advancementBehaviour);
    }

    public TeslaTurbineCore getCore() {
        return core;
    }

    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    private void refreshGeneratedRotationIfNeeded() {
        if (level == null || level.isClientSide) {
            return;
        }

        float generatedSpeed = getGeneratedSpeed();
        if (Float.compare(generatedSpeed, lastGeneratedSpeed) == 0) {
            return;
        }

        lastGeneratedSpeed = generatedSpeed;
        updateGeneratedRotation();
    }
}
