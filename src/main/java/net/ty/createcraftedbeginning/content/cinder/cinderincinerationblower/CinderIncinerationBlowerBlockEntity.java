package net.ty.createcraftedbeginning.content.cinder.cinderincinerationblower;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class CinderIncinerationBlowerBlockEntity extends KineticBlockEntity {
    private static final String COMPOUND_KEY_SHOW_OUTLINE = "ShowOutline";
    private static final String COMPOUND_KEY_OWNER = "Owner";

    private ScrollOptionBehaviour<BlowerWorkingMode> blowerWorkingMode;
    private int particleCounter;
    private boolean showOutline;
    private CinderIncinerationBlowerFakePlayer player;
    private UUID owner;

    public CinderIncinerationBlowerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        particleCounter = 0;
        showOutline = true;
    }

    private static void applyFanProcessing(@NotNull Level level, FanProcessingType processingType, AABB area) {
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (level.isClientSide) {
                processingType.spawnProcessingParticles(level, itemEntity.position());
                continue;
            }

            if (!FanProcessing.canProcess(itemEntity, processingType)) {
                continue;
            }

            FanProcessing.applyProcessing(itemEntity, processingType);
        }

        BlockPos.betweenClosedStream(area.deflate(0.5)).forEach(blockPos -> {
            TransportedItemStackHandlerBehaviour behaviour = BlockEntityBehaviour.get(level, blockPos, TransportedItemStackHandlerBehaviour.TYPE);
            if (behaviour == null) {
                return;
            }

            behaviour.handleProcessingOnAllItems(transported -> {
                if (level.isClientSide) {
                    processingType.spawnProcessingParticles(level, behaviour.getWorldPositionOf(transported));
                    return TransportedResult.doNothing();
                }

                return FanProcessing.applyProcessing(transported, level, processingType);
            });
        });
    }

    private static void applyIgnition(@NotNull Level level, AABB area) {
        for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!livingEntity.isAlive() || livingEntity.fireImmune()) {
                continue;
            }

            livingEntity.igniteForSeconds(15);
            if (!(livingEntity instanceof SnowGolem golem)) {
                continue;
            }

            golem.die(golem.damageSources().onFire());
        }
    }

    private static void applyGrinding(@NotNull Level level, AABB area, Player player) {
        for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!livingEntity.isAlive() || livingEntity.fireImmune()) {
                continue;
            }

            if (livingEntity.hurt(livingEntity.damageSources().onFire(), 1)) {
                livingEntity.igniteForSeconds(2);
                livingEntity.setLastHurtByPlayer(player);
            }
            if (!(livingEntity instanceof SnowGolem golem)) {
                continue;
            }

            golem.die(golem.damageSources().onFire());
        }
    }

    public static float calculateRange(float speed) {
        float absSpeed = Mth.abs(speed);
        if (absSpeed < SpeedLevel.MEDIUM.getSpeedValue()) {
            return 0;
        }

        return absSpeed / SpeedLevel.MEDIUM.getSpeedValue() - 0.5f;
    }

    public static @NotNull AABB calculateArea(BlockPos pos, float speed) {
        Vec3 center = Vec3.atCenterOf(pos);
        double range = calculateRange(speed);
        return new AABB(center.x - range, center.y - range, center.z - range, center.x + range, center.y + range, center.z + range);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        player = new CinderIncinerationBlowerFakePlayer(serverLevel, owner);
        Vec3 center = VecHelper.getCenterOf(worldPosition);
        player.setPos(center.x, center.y, center.z);
    }

    @Override
    public void tick() {
        super.tick();
        displayOutline();
        spawnParticle();
        applyPrimaryEffect();
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.putBoolean(COMPOUND_KEY_SHOW_OUTLINE, showOutline);
        if (owner == null) {
            return;
        }

        compoundTag.putUUID(COMPOUND_KEY_OWNER, owner);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (compoundTag.contains(COMPOUND_KEY_SHOW_OUTLINE)) {
            showOutline = compoundTag.getBoolean(COMPOUND_KEY_SHOW_OUTLINE);
        }
        if (compoundTag.contains(COMPOUND_KEY_OWNER)) {
            owner = compoundTag.getUUID(COMPOUND_KEY_OWNER);
        }
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        blowerWorkingMode = new ScrollOptionBehaviour<>(BlowerWorkingMode.class, CCBLang.translateDirect("gui.cinder_incineration_blower.working_mode"), this, new CinderIncinerationBlowerValueBox());
        behaviours.add(blowerWorkingMode);
    }

    public void toggleShowOutline() {
        showOutline = !showOutline;
        notifyUpdate();
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public void discardPlayer() {
        if (player == null) {
            return;
        }

        player.discard();
        player = null;
    }

    private void displayOutline() {
        if (!(level instanceof ServerLevel serverLevel) || !showOutline) {
            return;
        }

        CatnipServices.NETWORK.sendToClientsAround(serverLevel, worldPosition, 64, new CinderIncinerationBlowerOutlinePacket(worldPosition, getSpeed()));
    }

    private void spawnParticle() {
        float absSpeed = Mth.abs(getSpeed());
        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();
        float ratio = absSpeed / mediumSpeed;
        if (absSpeed < mediumSpeed || level == null || !level.isClientSide) {
            return;
        }

        particleCounter = (particleCounter + 1) % 40;
        if (particleCounter % Mth.floor(40 / ratio) != 0) {
            return;
        }

        for (int i = 0; i < Mth.floor(ratio); i++) {
            Vec3 centerOf = VecHelper.getCenterOf(worldPosition);
            Vec3 offset = VecHelper.offsetRandomly(centerOf, level.random, calculateRange(absSpeed) * 0.9f);
            Vec3 velocity = centerOf.subtract(offset);
            level.addParticle(ParticleTypes.LAVA, offset.x, offset.y, offset.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private void applyPrimaryEffect() {
        float absSpeed = Mth.abs(getSpeed());
        if (absSpeed < SpeedLevel.MEDIUM.getSpeedValue() || level == null || level instanceof PonderLevel) {
            return;
        }

        AABB area = calculateArea(worldPosition, absSpeed);
        switch (blowerWorkingMode.get()) {
            case SMOKING -> applyFanProcessing(level, AllFanProcessingTypes.SMOKING, area);
            case BLASTING -> applyFanProcessing(level, AllFanProcessingTypes.BLASTING, area);
            case IGNITION -> applyIgnition(level, area);
            case GRINDING -> applyGrinding(level, area, player);
        }
    }

    private enum BlowerWorkingMode implements INamedIconOptions {
        SMOKING(CCBIcons.I_SMOKING),
        BLASTING(CCBIcons.I_BLASTING),
        IGNITION(CCBIcons.I_IGNITION),
        GRINDING(CCBIcons.I_GRINDING);

        private final String translationKey;
        private final CCBIcons icon;

        BlowerWorkingMode(CCBIcons icon) {
            this.icon = icon;
            translationKey = "createcraftedbeginning.gui.cinder_incineration_blower.working_mode." + Lang.asId(name());
        }

        @Override
        public CCBIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }
}
