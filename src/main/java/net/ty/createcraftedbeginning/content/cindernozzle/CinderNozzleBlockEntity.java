package net.ty.createcraftedbeginning.content.cindernozzle;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.data.CCBDamageSources;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CinderNozzleBlockEntity extends KineticBlockEntity {
    static final int DEFAULT_RANGE = 7;
    static final int MAX_RANGE = 16;

    private int timer = 0;
    private int edgeLength = DEFAULT_RANGE;

    public CinderNozzleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();

        float absSpeed = Mth.abs(speed);
        if (level == null || absSpeed < 32) {
            return;
        }

        int interval = Mth.ceil(2560 / absSpeed);
        if (timer > interval) {
            applyPrimaryEffect(level);
            spawnParticle(level, absSpeed);
            timer = 0;
        }
        timer++;
    }

    private void spawnParticle(@NotNull Level level, float absSpeed) {
        if (!level.isClientSide) {
            return;
        }

        for (int i = 0; i < Mth.clamp(Mth.floor(absSpeed / 64), 1, 8); i++) {
            Vec3 centerOf = VecHelper.getCenterOf(worldPosition);
            Vec3 v = VecHelper.offsetRandomly(centerOf, level.random, 1.25f);
            Vec3 m = centerOf.subtract(v);
            level.addParticle(ParticleTypes.LAVA, v.x, v.y, v.z, m.x, m.y, m.z);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        CinderNozzleScrollValueBehaviour scroll = new CinderNozzleScrollValueBehaviour(Component.translatable("logistics.cinder_nozzle.working_range"), this, new CinderNozzleValueBox());
        scroll.between(0, MAX_RANGE);
        scroll.value = DEFAULT_RANGE;
        scroll.withCallback(this::onWorkingRangeChanged);
        behaviours.add(scroll);
    }

    private void onWorkingRangeChanged(int newRange) {
        edgeLength = newRange;
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public int getRange() {
        return edgeLength;
    }

    private void applyPrimaryEffect(@NotNull Level level) {
        if (level.isClientSide) {
            return;
        }

        Vec3 center = Vec3.atCenterOf(worldPosition);
        double halfEdge = edgeLength / 2F;
        AABB area = new AABB(center.x - halfEdge, center.y - halfEdge, center.z - halfEdge, center.x + halfEdge, center.y + halfEdge, center.z + halfEdge);
        for (Entity entity : level.getEntities(null, area)) {
            if (!(entity instanceof LivingEntity) || !entity.isAlive()) {
                continue;
            }
            if (!(entity instanceof Mob livingEntity) || entity.fireImmune()) {
                continue;
            }

            doFireDamage(livingEntity);
        }
    }

    private void doFireDamage(@NotNull LivingEntity entity) {
        entity.igniteForSeconds((float) 2);
        entity.hurt(CCBDamageSources.cinderNozzleFire(level), 2);

        if (entity instanceof SnowGolem) {
            entity.die(CCBDamageSources.cinderNozzleFire(level));
        }
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("ScrollValue", edgeLength);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (compound.contains("ScrollValue")) {
            edgeLength = compound.getInt("ScrollValue");
        }
    }
}
