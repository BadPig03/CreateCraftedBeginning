package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
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
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class EndIncinerationBlowerBlockEntity extends KineticBlockEntity {
    private static final String COMPOUND_KEY_SHOW_OUTLINE = "ShowOutline";
    private static final String COMPOUND_KEY_OWNER = "Owner";

    private EndIncinerationBlowerStructuralBlockEntity structural;
    private EndIncinerationBlowerFakePlayer player;
    private UUID owner;
    private boolean showOutline;
    private int particleCounter;

    public EndIncinerationBlowerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        showOutline = true;
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

    private static void applyIgnition(@NotNull Level level, AABB area, EndIncinerationBlowerFakePlayer fakePlayer, UUID playerUUID) {
        for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!livingEntity.isAlive() || livingEntity.fireImmune()) {
                continue;
            }

            if (livingEntity.hurt(livingEntity.damageSources().onFire(), 2)) {
                livingEntity.igniteForSeconds(2);
                livingEntity.setLastHurtByPlayer(fakePlayer);
                Player player = level.getPlayerByUUID(playerUUID);
                if (player != null && !CCBAdvancements.HOT_HOT_HOT.isAlreadyAwardedTo(player)) {
                    CCBAdvancements.HOT_HOT_HOT.awardTo(player);
                }
            }
            if (!(livingEntity instanceof SnowGolem golem)) {
                continue;
            }

            golem.die(golem.damageSources().onFire());
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        player = new EndIncinerationBlowerFakePlayer(serverLevel, owner);
        Vec3 center = VecHelper.getCenterOf(worldPosition);
        player.setPos(center.x, center.y, center.z);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        displayOutline();
        spawnParticles();
        applyPrimaryEffect();
        verifyStructural();
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

    public void updateStructural() {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockPos structuralPos = worldPosition.below();
        if (!level.getBlockState(structuralPos).is(CCBBlocks.END_CASING_BLOCK)) {
            return;
        }
        if (!level.setBlockAndUpdate(structuralPos, CCBBlocks.END_INCINERATION_BLOWER_STRUCTURAL_BLOCK.getDefaultState())) {
            return;
        }

        structural = getStructural();
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

    private @Nullable EndIncinerationBlowerStructuralBlockEntity getStructural() {
        if (level == null || !(level.getBlockEntity(worldPosition.below()) instanceof EndIncinerationBlowerStructuralBlockEntity be)) {
            return null;
        }

        return be;
    }

    private void displayOutline() {
        if (!(level instanceof ServerLevel serverLevel) || !showOutline) {
            return;
        }

        CatnipServices.NETWORK.sendToClientsAround(serverLevel, worldPosition, 64, new EndIncinerationBlowerOutlinePacket(worldPosition, getSpeed()));
    }

    private void verifyStructural() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (structural == null || structural.isRemoved()) {
            structural = getStructural();
        }
        if (structural != null) {
            return;
        }

        level.destroyBlock(worldPosition, true);
    }

    private void spawnParticles() {
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
            level.addParticle(CCBParticleTypes.END_INCINERATION.getParticleOptions(), offset.x, offset.y, offset.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private void applyPrimaryEffect() {
        float absSpeed = Mth.abs(getSpeed());
        if (absSpeed < SpeedLevel.MEDIUM.getSpeedValue() || level == null || level instanceof PonderLevel) {
            return;
        }

        EndIncinerationBlowerStructuralBlockEntity structural = getStructural();
        if (structural == null) {
            return;
        }

        AABB area = calculateArea(worldPosition, absSpeed);
        switch (structural.getBlowerWorkingMode().get()) {
            case SMOKING -> applyFanProcessing(level, AllFanProcessingTypes.SMOKING, area);
            case BLASTING -> applyFanProcessing(level, AllFanProcessingTypes.BLASTING, area);
            case IGNITION -> applyIgnition(level, area, player, owner);
        }
    }
}
