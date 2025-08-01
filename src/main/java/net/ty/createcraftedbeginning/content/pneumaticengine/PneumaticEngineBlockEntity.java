package net.ty.createcraftedbeginning.content.pneumaticengine;

import com.simibubi.create.content.equipment.armor.BacktankBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.particle.AirParticleData;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PneumaticEngineBlockEntity extends GeneratingKineticBlockEntity {
    private boolean isActive = false;
    private boolean isClockwise = true;
    private int airTimer;

    public PneumaticEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();
        isActive = false;

        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed()) {
            updateGeneratedRotation();
        }
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

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return true;
    }

    private void spawnAirParticle() {
        if (level == null || !level.isClientSide || speed == 0) {
            return;
        }
        Vec3 centerOf = VecHelper.getCenterOf(worldPosition);
        Vec3 v = VecHelper.offsetRandomly(centerOf, level.random, 1.25f);
        Vec3 m = VecHelper.clamp(centerOf.subtract(v), .05F);
        level.addParticle(ParticleTypes.CLOUD, v.x, v.y, v.z, m.x, m.y, m.z);
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
        if (!(tankBlockEntity instanceof BacktankBlockEntity tank)) {
            return false;
        }
        return tank.getAirLevel() > 0;
    }

    public void toggleDirection() {
        isClockwise = !isClockwise;
        updateGeneratedRotation();
        setChanged();
    }

    private int getClockwise() {
        return isClockwise ? 1 : -1;
    }

    @Override
    public float getGeneratedSpeed() {
        return isActive ? (getClockwise() * 64.0F) : 0;
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (!clientPacket) {
            compound.putBoolean("Clockwise", isClockwise);
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (!clientPacket && compound.contains("Clockwise")) {
            isClockwise = compound.getBoolean("Clockwise");
        }
    }
}