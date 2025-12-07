package net.ty.createcraftedbeginning.content.obsolete.cinderincinerationblower;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.registry.CCBDamageSources;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CinderIncinerationBlowerBlockEntity extends KineticBlockEntity {
    public static final int MAX_RANGE = 15;
    private static final int DEFAULT_RANGE = 7;
    private static final String COMPOUND_KEY_SCROLL_VALUE = "ScrollValue";
    private int timer;
    private int edgeLength = DEFAULT_RANGE;

    public CinderIncinerationBlowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null) {
            return;
        }

        float absSpeed = Mth.abs(speed);
        int interval = Mth.ceil(2560 / absSpeed);
        if (timer > interval) {
            applyPrimaryEffect(level);
            spawnParticle(level, absSpeed);
            timer = 0;
        }
        timer++;
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider registries, boolean clientPacket) {
        super.write(compoundTag, registries, clientPacket);
        compoundTag.putInt(COMPOUND_KEY_SCROLL_VALUE, edgeLength);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider registries, boolean clientPacket) {
        super.read(compoundTag, registries, clientPacket);
        if (compoundTag.contains(COMPOUND_KEY_SCROLL_VALUE)) {
            edgeLength = compoundTag.getInt(COMPOUND_KEY_SCROLL_VALUE);
        }
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        CinderIncinerationBlowerScrollValueBehaviour scroll = new CinderIncinerationBlowerScrollValueBehaviour(Component.translatable("appliances.cinder_incineration_blower.working_range"), this, new CinderIncinerationBlowerValueBox());
        scroll.between(0, MAX_RANGE);
        scroll.value = DEFAULT_RANGE;
        scroll.withCallback(this::onWorkingRangeChanged);
        behaviours.add(scroll);
        super.addBehaviours(behaviours);
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

    private void applyPrimaryEffect(@NotNull Level level) {
        if (level.isClientSide) {
            return;
        }

        Vec3 center = Vec3.atCenterOf(worldPosition);
        double halfEdge = edgeLength / 2.0f;
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

        if (!(entity instanceof SnowGolem golem)) {
            return;
        }
        golem.die(CCBDamageSources.cinderNozzleFire(level));
    }

    private void onWorkingRangeChanged(int newRange) {
        edgeLength = newRange;
        notifyUpdate();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public int getRange() {
        return edgeLength;
    }
}
