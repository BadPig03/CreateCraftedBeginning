package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {
    private static final String COMPOUND_KEY_CORE = "Core";
    private final TeslaTurbineCore core;

    private CCBAdvancementBehaviour advancementBehaviour;

    public TeslaTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        core = new TeslaTurbineCore(this);
        setLazyTickRate(TeslaTurbineUtils.LAZY_TICK_RATE);
    }

    private static float calculateStressCapacity(float generatedSpeed) {
        float absoluteSpeed = Math.abs(generatedSpeed);
        int maxRotationSpeed = AllConfigs.server().kinetics.maxRotationSpeed.get();
        double baseStressCapacity = BlockStressValues.getCapacity(CCBBlocks.TESLA_TURBINE_BLOCK.get());
        return absoluteSpeed * (float) baseStressCapacity / maxRotationSpeed;
    }

    @Override
    public void tick() {
        super.tick();
        core.tick();
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
        core.initialize();
    }

    @Override
    public float calculateAddedStressCapacity() {
        float stressCapacity = calculateStressCapacity(getGeneratedSpeed());
        lastCapacityProvided = stressCapacity;
        return stressCapacity;
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        core.onSpeedChanged();
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_CORE, core.write(provider, clientPacket));
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        CompoundTag coreTag = CCBNbtUtils.getCompoundOrEmpty(compoundTag, COMPOUND_KEY_CORE);
        core.read(coreTag, provider, clientPacket);
    }

    @Override
    public float getGeneratedSpeed() {
        return core.getGeneratedSpeed();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.MIRACLE_OF_ENGINEERING, CCBAdvancements.GENIUS_ENGINEER, CCBAdvancements.TESLA_TURBINE_EASY_AS_PIE);
        behaviours.add(advancementBehaviour);
    }

    public IGasHandler createGasHandler(boolean clockwise) {
        return core.createGasHandler(clockwise);
    }

    CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    void refreshStructure() {
        core.getStructureManager().tick();
    }
}
