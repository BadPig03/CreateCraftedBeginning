package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.end.endcasing.EndCasingBlock;
import net.ty.createcraftedbeginning.content.end.endcasing.EndMechanicalBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBDamageTypes;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EndIncinerationBlowerBlockEntity extends EndMechanicalBlockEntity<EndIncinerationBlowerStructuralBlockEntity> {
    private static final String COMPOUND_KEY_SHOW_OUTLINE = "ShowOutline";
    private static final String COMPOUND_KEY_OWNER = "Owner";
    private static final String FAKE_PLAYER_NAME = "[CCB_EIB]";
    private static final String FAKE_PLAYER_UUID_PREFIX = "createcraftedbeginning:end_incineration_blower:";
    private static final int TRANSPORTED_HANDLER_CACHE_INTERVAL = 20;
    private static final int PROCESSING_PARTICLE_INTERVAL_TICKS = 10;
    private static final int MAX_PROCESSING_PARTICLE_TARGETS = 8;
    private static Consumer<EndIncinerationBlowerBlockEntity> clientTicker = blower -> {};

    private final List<BlockPos> transportedHandlerPositions;

    private GameProfile fakePlayerProfile;
    private UUID owner;
    private boolean showOutline;
    private int particleCounter;
    private int cachedBlockRadius;
    private long nextTransportedHandlerScanTime;

    public EndIncinerationBlowerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        showOutline = true;
        transportedHandlerPositions = new ArrayList<>();
        cachedBlockRadius = -1;
        nextTransportedHandlerScanTime = Long.MIN_VALUE;
    }

    public static void setClientTicker(Consumer<EndIncinerationBlowerBlockEntity> ticker) {
        clientTicker = ticker;
    }

    public static float getMaxRange() {
        return Mth.clamp(CCBConfig.server().endDevices.maxRange.getF(), 0, 32);
    }

    public static float calculateRange(float speed) {
        float absSpeed = Mth.abs(speed);
        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();
        if (absSpeed < mediumSpeed) {
            return 0;
        }

        return Mth.clamp(absSpeed / mediumSpeed - 0.5F, 0, getMaxRange());
    }

    public static int calculateBlockRadius(float speed) {
        return Mth.floor(calculateRange(speed));
    }

    public static AABB calculateArea(BlockPos pos, float speed) {
        Vec3 center = Vec3.atCenterOf(pos);
        double range = calculateRange(speed);
        return new AABB(center.x - range, center.y - range, center.z - range, center.x + range, center.y + range, center.z + range);
    }

    private static boolean applyFanProcessing(ServerLevel level, FanProcessingType processingType, AABB area, List<BlockPos> transportedHandlerPositions) {
        AtomicBoolean applied = new AtomicBoolean(false);
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (!FanProcessing.canProcess(itemEntity, processingType)) {
                continue;
            }

            if (FanProcessing.applyProcessing(itemEntity, processingType)) {
                applied.set(true);
            }
        }

        for (BlockPos blockPos : transportedHandlerPositions) {
            TransportedItemStackHandlerBehaviour behaviour = BlockEntityBehaviour.get(level, blockPos, TransportedItemStackHandlerBehaviour.TYPE);
            if (behaviour == null) {
                continue;
            }

            behaviour.handleProcessingOnAllItems(transported -> {
                TransportedResult result = FanProcessing.applyProcessing(transported, level, processingType);
                if (!result.doesNothing()) {
                    applied.set(true);
                }
                return result;
            });
        }

        return applied.get();
    }

    private static void spawnFanProcessingParticles(Level level, FanProcessingType processingType, AABB area) {
        int spawned = 0;
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (!FanProcessing.canProcess(itemEntity, processingType)) {
                continue;
            }

            processingType.spawnProcessingParticles(level, itemEntity.position());
            if (++spawned < MAX_PROCESSING_PARTICLE_TARGETS) {
                continue;
            }

            return;
        }
    }

    private static boolean applyIgnition(ServerLevel level, AABB area, FakePlayer fakePlayer, EndIncinerationBlowerBlockEntity be) {
        boolean applied = false;
        DamageSource damageSource = CCBDamageTypes.source(DamageTypes.ON_FIRE, level, fakePlayer);
        for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!livingEntity.isAlive() || livingEntity.fireImmune() || livingEntity instanceof Player && !CCBConfig.server().endDevices.ignitionAffectsPlayers.get()) {
                continue;
            }

            boolean snowGolem = livingEntity instanceof SnowGolem;
            float configuredDamage = Math.max(0, CCBConfig.server().endDevices.ignitionDamage.getF());
            float damage = snowGolem ? livingEntity.getHealth() + livingEntity.getAbsorptionAmount() + 1.0F : configuredDamage;
            if (damage <= 0) {
                continue;
            }
            if (!livingEntity.hurt(damageSource, damage)) {
                continue;
            }

            if (livingEntity.isAlive()) {
                livingEntity.igniteForSeconds(2);
            }
            applied = true;
            if (!snowGolem || livingEntity.isAlive()) {
                continue;
            }

            be.advancementBehaviour.awardPlayer(CCBAdvancements.WARM_HEARTED);
        }
        return applied;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.HOT_HOT_HOT, CCBAdvancements.WARM_HEARTED);
        behaviours.add(advancementBehaviour);
    }

    @Override
    protected Class<EndIncinerationBlowerStructuralBlockEntity> getStructuralClass() {
        return EndIncinerationBlowerStructuralBlockEntity.class;
    }

    @Override
    public void updateStructural() {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockPos structuralPos = worldPosition.below();
        if (!(level.getBlockState(structuralPos).getBlock() instanceof EndCasingBlock)) {
            return;
        }
        if (!level.setBlockAndUpdate(structuralPos, CCBBlocks.END_INCINERATION_BLOWER_STRUCTURAL_BLOCK.getDefaultState())) {
            return;
        }

        structural = getStructural();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            boolean showParticles = CCBConfig.client().enableEndIncinerationBlowerParticles.get();
            if (showParticles) {
                spawnParticles();
            }
            if (level instanceof PonderLevel) {
                return;
            }

            if (showParticles && shouldSpawnProcessingParticles(level)) {
                spawnPrimaryEffectParticles();
            }
            clientTicker.accept(this);
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        applyPrimaryEffect(serverLevel);
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

        owner = compoundTag.contains(COMPOUND_KEY_OWNER) ? compoundTag.getUUID(COMPOUND_KEY_OWNER) : null;
        fakePlayerProfile = null;
    }

    public void toggleShowOutline() {
        showOutline = !showOutline;
        notifyUpdate();
    }

    public boolean isShowingOutline() {
        return showOutline;
    }

    public void setOwner(UUID owner) {
        if (Objects.equals(this.owner, owner)) {
            return;
        }

        this.owner = owner;
        fakePlayerProfile = null;
        setChanged();
    }

    private FakePlayer getFakePlayer(ServerLevel level) {
        if (fakePlayerProfile == null) {
            String identity = owner == null ? "unowned" : owner.toString();
            UUID profileId = UUID.nameUUIDFromBytes((FAKE_PLAYER_UUID_PREFIX + identity).getBytes(StandardCharsets.UTF_8));
            fakePlayerProfile = new GameProfile(profileId, FAKE_PLAYER_NAME);
        }
        FakePlayer fakePlayer = FakePlayerFactory.get(level, fakePlayerProfile);
        Vec3 center = VecHelper.getCenterOf(worldPosition);
        fakePlayer.setPos(center.x, center.y, center.z);
        return fakePlayer;
    }

    private @Nullable EndIncinerationBlowerStructuralBlockEntity getStructuralForUse() {
        if (structural == null || structural.isRemoved()) {
            structural = getStructural();
        }
        return structural;
    }

    private List<BlockPos> getTransportedHandlerPositions(Level level, float speed) {
        int blockRadius = calculateBlockRadius(speed);
        long gameTime = level.getGameTime();
        if (cachedBlockRadius == blockRadius && gameTime < nextTransportedHandlerScanTime) {
            return transportedHandlerPositions;
        }

        transportedHandlerPositions.clear();
        BlockPos min = worldPosition.offset(-blockRadius, -blockRadius, -blockRadius);
        BlockPos max = worldPosition.offset(blockRadius, blockRadius, blockRadius);
        for (BlockPos blockPos : BlockPos.betweenClosed(min, max)) {
            TransportedItemStackHandlerBehaviour behaviour = BlockEntityBehaviour.get(level, blockPos, TransportedItemStackHandlerBehaviour.TYPE);
            if (behaviour == null) {
                continue;
            }

            transportedHandlerPositions.add(blockPos.immutable());
        }

        cachedBlockRadius = blockRadius;
        nextTransportedHandlerScanTime = gameTime + TRANSPORTED_HANDLER_CACHE_INTERVAL;
        return transportedHandlerPositions;
    }

    private boolean shouldApplyIgnition(ServerLevel level) {
        return Math.floorMod(level.getGameTime(), 20) == Math.floorMod(worldPosition.hashCode(), 20);
    }

    private boolean shouldSpawnProcessingParticles(Level level) {
        return Math.floorMod(level.getGameTime(), PROCESSING_PARTICLE_INTERVAL_TICKS) == Math.floorMod(worldPosition.hashCode(), PROCESSING_PARTICLE_INTERVAL_TICKS);
    }

    private void spawnParticles() {
        float absSpeed = Mth.abs(getSpeed());
        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();
        if (absSpeed < mediumSpeed || level == null || !level.isClientSide) {
            return;
        }

        float effectiveRatio = Mth.clamp(absSpeed / mediumSpeed, 1, Math.max(1, getMaxRange() + 0.5f));
        int spawnInterval = Math.max(1, Mth.floor(40 / effectiveRatio));
        particleCounter = (particleCounter + 1) % 40;
        if (particleCounter % spawnInterval != 0) {
            return;
        }

        int particleCount = Math.max(1, Mth.floor(effectiveRatio));
        Vec3 center = VecHelper.getCenterOf(worldPosition);
        for (int i = 0; i < particleCount; i++) {
            Vec3 offset = VecHelper.offsetRandomly(center, level.random, calculateRange(absSpeed) * 0.9F);
            Vec3 direction = center.subtract(offset);
            if (direction.lengthSqr() < 1.0E-6) {
                continue;
            }

            Vec3 velocity = direction.normalize().scale(0.025F + effectiveRatio * 0.015F);
            level.addParticle(CCBParticleTypes.END_INCINERATION.getParticleOptions(), offset.x, offset.y, offset.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private void spawnPrimaryEffectParticles() {
        float absSpeed = Mth.abs(getSpeed());
        EndIncinerationBlowerStructuralBlockEntity structural = getStructuralForUse();
        if (absSpeed < SpeedLevel.MEDIUM.getSpeedValue() || level == null || structural == null) {
            return;
        }

        AABB area = calculateArea(worldPosition, absSpeed);
        switch (structural.getBlowerWorkingMode().get()) {
            case SMOKING -> spawnFanProcessingParticles(level, AllFanProcessingTypes.SMOKING, area);
            case BLASTING -> spawnFanProcessingParticles(level, AllFanProcessingTypes.BLASTING, area);
            case IGNITION -> {
            }
        }
    }

    private void applyPrimaryEffect(ServerLevel level) {
        float absSpeed = Mth.abs(getSpeed());
        EndIncinerationBlowerStructuralBlockEntity structural = getStructuralForUse();
        if (absSpeed < SpeedLevel.MEDIUM.getSpeedValue() || structural == null) {
            return;
        }

        AABB area = calculateArea(worldPosition, absSpeed);
        boolean result = switch (structural.getBlowerWorkingMode().get()) {
            case SMOKING -> applyFanProcessing(level, AllFanProcessingTypes.SMOKING, area, getTransportedHandlerPositions(level, absSpeed));
            case BLASTING -> applyFanProcessing(level, AllFanProcessingTypes.BLASTING, area, getTransportedHandlerPositions(level, absSpeed));
            case IGNITION -> shouldApplyIgnition(level) && applyIgnition(level, area, getFakePlayer(level), this);
        };
        if (!result) {
            return;
        }

        advancementBehaviour.awardPlayer(CCBAdvancements.HOT_HOT_HOT);
    }
}
