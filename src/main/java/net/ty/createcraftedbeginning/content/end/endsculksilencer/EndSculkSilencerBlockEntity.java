package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.end.endcasing.EndCasingBlock;
import net.ty.createcraftedbeginning.content.end.endcasing.EndMechanicalBlockEntity;
import net.ty.createcraftedbeginning.platform.CCBSubLevelBridge;
import net.ty.createcraftedbeginning.platform.CCBSubLevelBridge.Projection;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EndSculkSilencerBlockEntity extends EndMechanicalBlockEntity<EndSculkSilencerStructuralBlockEntity> {
    public static final int LAZY_TICK_RATE = 20;

    private static final String COMPOUND_KEY_SHOW_OUTLINE = "ShowOutline";
    private static final float MAX_ANIMATION_SPEED = 40;
    private static Consumer<EndSculkSilencerBlockEntity> clientTicker = silencer -> {};

    private final LerpedFloat animationSpeed;
    private final LerpedFloat animation;

    private boolean showOutline;
    private boolean inSableSubLevel;

    public EndSculkSilencerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(LAZY_TICK_RATE);
        animationSpeed = LerpedFloat.linear().startWithValue(0);
        animation = LerpedFloat.angular().startWithValue(0);
        showOutline = true;
        inSableSubLevel = false;
    }

    public static void setClientTicker(Consumer<EndSculkSilencerBlockEntity> ticker) {
        clientTicker = ticker;
    }

    public static boolean meetsRequiredSpeed(float speed, short range) {
        float multiplier = Math.max(0, CCBConfig.server().endDevices.speedRequirementMultiplier.getF());
        return range > 0 && Mth.abs(speed) >= SpeedLevel.MEDIUM.getSpeedValue() * range * Mth.sqrt(range) * multiplier;
    }

    public static float calculateAnimationTargetSpeed(float kineticSpeed) {
        float absSpeed = Mth.abs(kineticSpeed);
        if (absSpeed == 0) {
            return 0;
        }

        float rawTargetSpeed = Math.signum(kineticSpeed) * 2 * Mth.ceil(Math.log10(absSpeed) + Math.sqrt(absSpeed));
        return Mth.clamp(rawTargetSpeed, -MAX_ANIMATION_SPEED, MAX_ANIMATION_SPEED);
    }

    public static AABB calculateArea(Level level, BlockPos pos, short range) {
        int chunkRadius = Math.max(0, range - 1);
        int centerChunkX = pos.getX() >> 4;
        int centerChunkZ = pos.getZ() >> 4;
        int minX = centerChunkX - chunkRadius << 4;
        int minZ = centerChunkZ - chunkRadius << 4;
        int maxX = centerChunkX + chunkRadius + 1 << 4;
        int maxZ = centerChunkZ + chunkRadius + 1 << 4;
        return new AABB(minX, level.getMinBuildHeight(), minZ, maxX, level.getMaxBuildHeight(), maxZ);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.STEVES_REDEMPTION);
        behaviours.add(advancementBehaviour);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        refreshSilencerState();
    }

    @Override
    protected Class<EndSculkSilencerStructuralBlockEntity> getStructuralClass() {
        return EndSculkSilencerStructuralBlockEntity.class;
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

        if (!level.setBlockAndUpdate(structuralPos, CCBBlocks.END_SCULK_SILENCER_STRUCTURAL_BLOCK.getDefaultState())) {
            return;
        }

        structural = getStructural();
        refreshSilencerState();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            updateAnimation();
            clientTicker.accept(this);
            return;
        }

        if (!(level instanceof ServerLevel serverLevel) || !inSableSubLevel) {
            return;
        }

        Projection projection = CCBSubLevelBridge.resolve(serverLevel, worldPosition);
        inSableSubLevel = projection.inSubLevel();
        refreshSilencerState(serverLevel, projection.blockPos());
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        refreshSilencerState();
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.putBoolean(COMPOUND_KEY_SHOW_OUTLINE, showOutline);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (!compoundTag.contains(COMPOUND_KEY_SHOW_OUTLINE)) {
            return;
        }

        showOutline = compoundTag.getBoolean(COMPOUND_KEY_SHOW_OUTLINE);
    }

    @Override
    public boolean isSpeedRequirementFulfilled() {
        EndSculkSilencerStructuralBlockEntity structural = getStructuralForUse();
        return structural != null && meetsRequiredSpeed(getSpeed(), structural.getWorkingRange());
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }

        refreshSilencerState();
    }

    @Override
    public void invalidate() {
        removeSilencerState();
        super.invalidate();
    }

    public LerpedFloat getAnimation() {
        return animation;
    }

    public void refreshSilencerState() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Projection projection = CCBSubLevelBridge.resolve(serverLevel, worldPosition);
        inSableSubLevel = projection.inSubLevel();
        refreshSilencerState(serverLevel, projection.blockPos());
    }

    public void toggleShowOutline() {
        showOutline = !showOutline;
        setChanged();
        notifyUpdate();
    }

    public boolean isShowingOutline() {
        return showOutline;
    }

    public short getActiveWorkingRange() {
        EndSculkSilencerStructuralBlockEntity structural = getStructuralForUse();
        if (structural == null) {
            return 0;
        }

        short range = structural.getWorkingRange();
        return meetsRequiredSpeed(getSpeed(), range) ? range : 0;
    }

    private void refreshSilencerState(ServerLevel serverLevel, BlockPos effectCenter) {
        GlobalEndSculkSilencerManager.update(serverLevel, worldPosition, effectCenter, getActiveWorkingRange());
    }

    private void removeSilencerState() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        GlobalEndSculkSilencerManager.remove(serverLevel, worldPosition);
    }

    private void updateAnimation() {
        if (isSpeedRequirementFulfilled()) {
            animationSpeed.chase(calculateAnimationTargetSpeed(getSpeed()), 0.1, Chaser.EXP);
        }
        else {
            animationSpeed.chase(0, 0.2, Chaser.EXP);
        }

        animationSpeed.tickChaser();
        animation.setValue(animation.getValue() + animationSpeed.getValue());
    }

    private @Nullable EndSculkSilencerStructuralBlockEntity getStructuralForUse() {
        if (structural != null && !structural.isRemoved()) {
            return structural;
        }

        structural = getStructural();
        return structural;
    }
}
