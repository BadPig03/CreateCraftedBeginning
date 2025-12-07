package net.ty.createcraftedbeginning.content.obsolete.pneumaticengine;

import com.simibubi.create.content.equipment.armor.BacktankBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PneumaticEngineBlockEntity extends GeneratingKineticBlockEntity {
    private boolean isActive;
    private boolean isClockwise;
    private int airTimer;

    public PneumaticEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        isActive = false;
        isClockwise = true;
    }

    @Override
    public void initialize() {
        super.initialize();

        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed()) {
            updateGeneratedRotation();
        }
    }

    @Override
    protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (!clientPacket) {
            compound.putBoolean("Clockwise", isClockwise);
        }
    }

    @Override
    protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (!clientPacket && compound.contains("Clockwise")) {
            isClockwise = compound.getBoolean("Clockwise");
        }
    }

    @Override
    public float getGeneratedSpeed() {
        int speedDirection = isClockwise ? 1 : -1;
        return isActive ? speedDirection * 48.0f : 0;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
    }

    @Override
    public void tick() {
        super.tick();

        if (!isOverStressed()) {
            updateGeneratedRotation();
        }

        boolean currentActive = isBelowTankAndHasAir();

        if (!currentActive && isOverStressed() && speed == 0 && level != null) {
            clearKineticInformation();
            level.destroyBlock(worldPosition, true);
            return;
        }

        if (level != null && level instanceof PonderLevel) {
            if (++airTimer >= 10) {
                airTimer = 0;
            }
            if (airTimer % 3 == 0) {
                spawnAirParticle();
            }
            return;
        }

        if (currentActive) {
            if (++airTimer >= 10) {
                airTimer = 0;
                consumeTankAir();
            }
            if (airTimer % 3 == 0) {
                spawnAirParticle();
            }
        }

        isActive = currentActive;
    }

    private void spawnAirParticle() {
        if (level == null || !level.isClientSide || speed == 0) {
            return;
        }
        Vec3 centerOf = VecHelper.getCenterOf(worldPosition);
        double angle = level.random.nextDouble() * Math.PI * 2;
        double distance = 0.75 + level.random.nextDouble() * 0.75;
        Vec3 targetPosition = centerOf.add(Math.cos(angle) * distance, Math.sin(angle) * distance, level.random.nextDouble() * 0.6 - 0.3);
        Vec3 motion = targetPosition.subtract(centerOf).normalize().scale(0.075f);
        level.addParticle(ParticleTypes.CLOUD, centerOf.x, centerOf.y, centerOf.z, motion.x, motion.y, motion.z);
    }

    private void consumeTankAir() {
        if (level == null) {
            return;
        }
        BlockPos tankPos = worldPosition.below();
        BlockEntity tankBlockEntity = level.getBlockEntity(tankPos);
        if (!(tankBlockEntity instanceof BacktankBlockEntity tank)) {
            return;
        }
        tank.setAirLevel(Math.max(tank.getAirLevel() - 1, 0));
        tank.setChanged();
    }

    private boolean isBelowTankAndHasAir() {
        if (level == null) {
            return false;
        }

        BlockPos tankPos = worldPosition.below();
        BlockState tankState = level.getBlockState(tankPos);
        BooleanProperty waterProperty = BlockStateProperties.WATERLOGGED;
        if (tankState.hasProperty(waterProperty) && tankState.getValue(waterProperty)) {
            return false;
        }

        BlockEntity tankBlockEntity = level.getBlockEntity(tankPos);
        return tankBlockEntity instanceof BacktankBlockEntity tank && tank.getAirLevel() > 0;
    }

    public void toggleDirection() {
        isClockwise = !isClockwise;
        updateGeneratedRotation();
        notifyUpdate();
    }
}